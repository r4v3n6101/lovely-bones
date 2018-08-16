package skeletal.model.loader.impl

import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.ResourceLocation
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.*
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import skeletal.DOMEN
import skeletal.graphics.VertexArrayObject
import skeletal.math.DualQuat
import skeletal.math.buildTransform
import skeletal.minecraft
import skeletal.model.Mesh
import skeletal.model.animated.AnimatedModel
import skeletal.model.animated.Animation
import skeletal.model.animated.Bone
import skeletal.model.animated.Keyframe
import skeletal.model.loader.ModelLoader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

object IQMLoader : ModelLoader {

    override fun loadModel(data: ByteArray): AnimatedModel {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(16 + 4 * 3) // Skip unused fields
        val textSize = buf.int
        val textOffset = buf.int
        val meshesNum = buf.int
        val meshesOffset = buf.int
        val vertexarraysNum = buf.int
        val verticesNum = buf.int
        val vertexarraysOffset = buf.int
        val trianglesNum = buf.int
        val trianglesOffset = buf.int
        buf.int // adjacency
        val bonesNum = buf.int // or joints
        val bonesOffset = buf.int
        val posesNum = buf.int
        val posesOffset = buf.int
        val animsNum = buf.int
        val animsOffset = buf.int
        val framesNum = buf.int
        val framechannels = buf.int // framechannelsNum
        val framedataOffset = buf.int
        val boundsOffset = buf.int

        if (textOffset == 0) error("Text data hasn't found")
        if (vertexarraysOffset == 0) error("Vertex arrays data hasn't found")
        if (trianglesOffset == 0) error("Triangles data hasn't found")
        if (meshesOffset == 0) error("Meshes data hasn't found")

        buf.position(textOffset)
        val text = ByteArray(textSize) { buf.get() }

        val vaoId = glGenVertexArrays()
        glBindVertexArray(vaoId)
        buf.position(vertexarraysOffset)
        val vbos = IntArray(vertexarraysNum + 1)
        repeat(vertexarraysNum) {
            vbos[it] = readVertexArrayToVBO(buf, verticesNum)
        }
        vbos[vbos.size - 1] = readIndicesToVBO(buf, trianglesOffset, trianglesNum * 3)
        glBindVertexArray(0)

        buf.position(meshesOffset)
        val meshes: HashMap<String, Mesh> = HashMap(meshesNum)
        repeat(meshesNum) {
            val meshName = readNulString(text, buf.int)
            val material = readNulString(text, buf.int)
            buf.int; buf.int // firstVertex & vertices
            val firstTri = buf.int
            val numTris = buf.int

            meshes[meshName] = Mesh(loadTexture(material), firstTri * 3, numTris * 3)
        }

        val bones: ArrayList<Bone> = ArrayList(bonesNum)
        val anims: HashMap<String, Animation> = HashMap(animsNum)

        // TODO Rewrite animation reading code
        if (bonesOffset != 0 && posesOffset != 0 && animsOffset != 0 && framedataOffset != 0) {
            buf.position(bonesOffset)
            repeat(bonesNum) {
                val name = readNulString(text, buf.int)
                val parent = buf.int
                val translate = Vector3f(buf.float, buf.float, buf.float)
                val rotation = Quaternion(buf.float, buf.float, buf.float, buf.float).apply { normalise() }
                val scale = Vector3f(buf.float, buf.float, buf.float)
                bones += Bone(name, bones.getOrNull(parent), translate, rotation, scale)
            }

            buf.position(posesOffset)
            val poses = Array(posesNum) {
                iqmpose(buf.int, buf.int,
                        floatArrayOf(
                                buf.float, buf.float, buf.float, // Translation
                                buf.float, buf.float, buf.float, buf.float, // Rotation (Quaternion)
                                buf.float, buf.float, buf.float // Scale (unused)
                        ),
                        floatArrayOf(
                                buf.float, buf.float, buf.float, // Translation
                                buf.float, buf.float, buf.float, buf.float, // Rotation (Quaternion)
                                buf.float, buf.float, buf.float // Scale (unused)
                        )
                )
            }

            buf.position(framedataOffset)
            val keyframes = ArrayList<Keyframe>(framesNum)
            for (i in 0 until framesNum) {
                val transforms = Array(posesNum) { Matrix4f() }
                for (j in 0 until posesNum) {
                    val pose = poses[j]
                    val off = pose.offset.copyOf()
                    for (channel in off.indices)
                        if ((pose.mask ushr channel) and 1 != 0)
                            off[channel] += pose.scale[channel] * java.lang.Short.toUnsignedInt(buf.short)

                    val p = Vector3f(off[0], off[1], off[2])
                    val q = Quaternion(off[3], off[4], off[5], off[6])
                            .apply { normalise() }
                    val s = Vector3f(off[7], off[8], off[9])
                    val bone = bones[j]

                    val parentBone = bone.parent
                    val m = buildTransform(q, s, p)
                    if (parentBone != null) Matrix4f.mul(transforms[pose.parent], m, m)
                    transforms[j] = m
                }
                for (j in 0 until posesNum) {
                    Matrix4f.mul(transforms[j], bones[j].inverseBaseTransform, transforms[j])
                }
                keyframes += Keyframe(transforms.map(DualQuat.Companion::fromMatrix).toTypedArray())
            }

            buf.position(animsOffset)
            repeat(animsNum) {
                val name = readNulString(text, buf.int)
                val firstFrame = buf.int
                val frames = buf.int
                val framerate = buf.float
                val loop = buf.int == 1
                val animKeyframes = keyframes.slice(firstFrame until firstFrame + frames).toTypedArray()
                anims[name] = Animation(animKeyframes, framerate, loop)
            }
        }

        val aabb = if (boundsOffset != 0) {
            buf.position(boundsOffset)
            AxisAlignedBB.getBoundingBox(
                    buf.float.toDouble(), buf.float.toDouble(), buf.float.toDouble(), // min
                    buf.float.toDouble(), buf.float.toDouble(), buf.float.toDouble() // max
            )
        } else null // TODO : Read all bounds
        return AnimatedModel(VertexArrayObject(vaoId, vbos), meshes, aabb, bones, anims)
    }

    private class iqmpose(val parent: Int, val mask: Int, val offset: FloatArray, val scale: FloatArray) // TODO : Try to reduce struct

    private fun loadTexture(material: String) =
            if (material.isEmpty())
                null
            else
                minecraft.textureManager.getTexture(ResourceLocation(DOMEN, "textures/$material"))?.glTextureId

    /**
     * Read iqmvertexarray directly to VBO
     * @param buf iqm file byte buffer. Buffer's position must equals beginning of vertex arrays' data
     * @param vertices number of vertices
     * @return id of vertexarray's VBO
     */
    private fun readVertexArrayToVBO(buf: ByteBuffer, vertices: Int): Int {
        val type = VertexArrayType.values()[buf.int]
        val flags = buf.int
        val format = VertexArrayFormat.values()[buf.int]
        val size = buf.int // size of attribute (1 to 4)
        val offset = buf.int

        val lastOffset = buf.position()
        val bufSize = vertices * size * format.size
        buf.position(offset)

        val output = BufferUtils.createByteBuffer(bufSize)
        repeat(bufSize) { output.put(buf.get()) }
        output.flip()

        buf.position(lastOffset)

        val vboId = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vboId)
        glBufferData(GL_ARRAY_BUFFER, output, GL_STATIC_DRAW)
        glVertexAttribPointer(type.ordinal, size, format.glRepr, false, 0, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glEnableVertexAttribArray(type.ordinal)
        return vboId
    }

    /**
     * Read indices from list of iqmtriangles directly to VBO. VBO keeps binded
     * @param buf iqm file byte buffer. Buffer's position doesn't matter
     * @param offset offset to beginning of iqmtriangles' list
     * @param indices ammount of indices which equals num_triangles * 3
     * @return id of IBO
     */
    private fun readIndicesToVBO(buf: ByteBuffer, offset: Int, indices: Int): Int {
        buf.position(offset)

        val indicesBuf = BufferUtils.createIntBuffer(indices)
        repeat(indices) { indicesBuf.put(buf.int) }
        indicesBuf.flip()

        val vboId = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuf, GL_STATIC_DRAW)
        return vboId
    }

    /**
     * Scan string from offset to NUL byte
     * @param text array of symbols
     * @param offset offset to string beginning
     */
    private fun readNulString(text: ByteArray, offset: Int): String {
        val str = mutableListOf<Byte>()
        for (i in offset until text.size) {
            val char = text[i]
            if (char == 0.toByte()) break
            str += char
        }
        return String(str.toByteArray())
    }

    private enum class VertexArrayType {
        POSITION,
        TEXCOORD,
        NORMAL,
        TANGENT,
        BLENDINDEXES,
        BLENDWEIGHTS,
        COLOR
    }

    private enum class VertexArrayFormat(val size: Int, val glRepr: Int) {
        BYTE(1, GL_BYTE),
        UBYTE(1, GL_UNSIGNED_BYTE),
        SHORT(2, GL_SHORT),
        USHORT(2, GL_UNSIGNED_SHORT),
        INT(4, GL_INT),
        UINT(4, GL_UNSIGNED_INT),
        HALF(2, GL_HALF_FLOAT),
        FLOAT(4, GL_FLOAT),
        DOUBLE(8, GL_DOUBLE)
    }
}