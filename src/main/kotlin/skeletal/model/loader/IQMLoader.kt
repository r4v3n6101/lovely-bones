package skeletal.model.loader

import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.model.IModelCustomLoader
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.*
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import skeletal.ModClass
import skeletal.graphics.VertexArrayObject
import skeletal.inputStream
import skeletal.math.DualQuat
import skeletal.math.buildTransform
import skeletal.minecraft
import skeletal.model.Mesh
import skeletal.model.animated.AnimatedModel
import skeletal.model.animated.Animation
import skeletal.model.animated.Bone
import skeletal.model.animated.Keyframe
import java.nio.ByteBuffer
import java.nio.ByteOrder

object IQMLoader : IModelCustomLoader {
    override fun getSuffixes() = arrayOf("iqm")
    override fun getType() = "InterQuakeModel"
    override fun loadInstance(resource: ResourceLocation) = loadModel(
            ByteBuffer
                    .wrap(resource.inputStream!!.use { it.readBytes() })
                    .order(ByteOrder.LITTLE_ENDIAN)
    )


    private fun loadModel(buf: ByteBuffer): AnimatedModel {
        val hdr = Header(
                magic = String(ByteArray(16) { buf.get() }),
                version = buf.int,
                filesize = buf.int,
                flags = buf.int,
                textSize = buf.int,
                textOffset = buf.int,
                meshesNum = buf.int,
                meshesOffset = buf.int,
                vertexarraysNum = buf.int,
                verticesNum = buf.int,
                vertexarraysOffset = buf.int,
                trianglesNum = buf.int,
                trianglesOffset = buf.int,
                adjacencyOffset = buf.int,
                bonesNum = buf.int, // or joints
                bonesOffset = buf.int,
                posesNum = buf.int,
                posesOffset = buf.int,
                animsNum = buf.int,
                animsOffset = buf.int,
                framesNum = buf.int,
                framechannelsNum = buf.int,
                framedataOffset = buf.int,
                boundsOffset = buf.int
        )

        checkIqm(hdr)

        buf.position(hdr.textOffset)
        val text = ByteArray(hdr.textSize) { buf.get() }

        val vao = createModelVao(hdr, buf)

        buf.position(hdr.meshesOffset)
        val meshes: HashMap<String, Mesh> = HashMap(hdr.meshesNum)
        repeat(hdr.meshesNum) {
            meshes += readMesh(buf, text)
        }

        val bonesList = ArrayList<Bone>(hdr.bonesNum) // Helpful list for using indices only
        val bonesMap = HashMap<String, Bone>(hdr.bonesNum)
        if (hdr.bonesOffset > 0) {
            buf.position(hdr.bonesOffset)
            repeat(hdr.bonesNum) {
                val (name, bone) = readBone(buf, text, bonesList)
                bonesList += bone
                bonesMap[name] = bone
            }
        }

        val anims: HashMap<String, Animation> = HashMap(hdr.animsNum)
        if (hdr.animsOffset > 0 && hdr.posesOffset > 0 && hdr.framedataOffset > 0) {
            buf.position(hdr.posesOffset)
            val poses = Array(hdr.posesNum) {
                Pose(buf.int, buf.int,
                        floatArrayOf(
                                buf.float, buf.float, buf.float, // Translation
                                buf.float, buf.float, buf.float, buf.float, // Rotation (Quaternion)
                                buf.float, buf.float, buf.float // Scale
                        ),
                        floatArrayOf(
                                buf.float, buf.float, buf.float, // Translation
                                buf.float, buf.float, buf.float, buf.float, // Rotation (Quaternion)
                                buf.float, buf.float, buf.float // Scale
                        )
                )
            }
            buf.position(hdr.framedataOffset)
            val keyframes = readKeyframes(hdr, buf, poses, bonesList)

            buf.position(hdr.animsOffset)
            repeat(hdr.animsNum) {
                anims += readAnimation(buf, text, keyframes)
            }
        }

        return AnimatedModel(vao, meshes, bonesMap, anims)
    }

    private fun checkIqm(hdr: Header) {
        check(hdr.magic == IQM_MAGIC) { "Wrong magic" }
        check(hdr.version == IQM_VERSION) { "Wrong version of IQM" }
        check(hdr.filesize <= 16 shl 20) { "Models bigger than 16MB aren't supported" }
        check(hdr.textOffset > 0) { "Text data hasn't found" }
        check(hdr.vertexarraysOffset > 0) { "Vertex arrays data hasn't found" }
        check(hdr.trianglesOffset > 0) { "Triangles data hasn't found" }
        check(hdr.meshesOffset > 0) { "Meshes data hasn't found" }
    }

    private fun createModelVao(hdr: Header, buf: ByteBuffer): VertexArrayObject {
        val vaoId = glGenVertexArrays()
        glBindVertexArray(vaoId)
        buf.position(hdr.vertexarraysOffset)
        val vbos = IntArray(hdr.vertexarraysNum + 1)
        repeat(hdr.vertexarraysNum) {
            vbos[it] = readVertexArrayToVBO(buf, hdr.verticesNum)
        }
        vbos[vbos.size - 1] = readIndicesToVBO(buf, hdr.trianglesOffset, hdr.trianglesNum)
        glBindVertexArray(0)
        return VertexArrayObject(vaoId, vbos)
    }

    private fun readMesh(buf: ByteBuffer, text: ByteArray): Pair<String, Mesh> {
        val meshName = readNulString(text, buf.int)
        val material = readNulString(text, buf.int)
        buf.int; buf.int // firstVertex & vertices
        val firstTri = buf.int
        val numTris = buf.int

        return meshName to Mesh(loadTexture(material), firstTri * 3, numTris * 3)
    }

    private fun readKeyframes(hdr: Header, buf: ByteBuffer, poses: Array<Pose>, bones: List<Bone>) =
            List(hdr.framesNum) { _ ->
                val transforms = Array(poses.size) { Matrix4f() }
                for (j in transforms.indices) {
                    val pose = poses[j]
                    val off = pose.offset.copyOf()
                    for (channel in off.indices)
                        if ((pose.mask ushr channel) and 1 != 0)
                            off[channel] += pose.scale[channel] * (buf.short.toInt() and 0xffff)

                    val p = Vector3f(off[0], off[1], off[2])
                    val q = Quaternion(off[3], off[4], off[5], off[6])
                            .apply { normalise() }
                    val s = Vector3f(off[7], off[8], off[9])

                    val transform = buildTransform(q, s, p)
                    if (pose.parent >= 0) Matrix4f.mul(transforms[pose.parent], transform, transform)
                    transforms[j] = transform
                }
                for (j in transforms.indices)
                    Matrix4f.mul(transforms[j], bones[j].inverseBaseTransform, transforms[j])
                Keyframe(transforms.map(DualQuat.Companion::fromMatrix).toTypedArray())
            }

    /*@Deprecated("Doesn't work correctly")
    private fun readKeyframesDQ(hdr: Header, buf: ByteBuffer, poses: Array<Pose>, bones: List<Bone>) =
            List(hdr.framesNum) { _ ->
                val transforms = Array(poses.size) { DualQuat() }
                for (j in transforms.indices) {
                    val pose = poses[j]
                    val off = pose.offset.copyOf()
                    for (channel in off.indices)
                        if ((pose.mask ushr channel) and 1 != 0)
                            off[channel] += pose.scale[channel] * (buf.short.toInt() and 0xffff)

                    val p = Vector3f(off[0], off[1], off[2])
                    val q = Quaternion(off[3], off[4], off[5], off[6])
                            .apply { normalise() }
                    val s = Vector3f(off[7], off[8], off[9])

                    val transform = DualQuat.fromQuatAndTranslation(q, p)
                    if (pose.parent >= 0) DualQuat.mul(transforms[pose.parent], transform, transform)
                    transforms[j] = transform
                }
                for (j in transforms.indices)
                    DualQuat.mul(transforms[j], bones[j].inverseBaseTransformDQ, transforms[j])
                Keyframe(transforms)
            }*/

    private fun readBone(buf: ByteBuffer, text: ByteArray, bones: List<Bone>): Pair<String, Bone> {
        val name = readNulString(text, buf.int)
        return name to Bone(
                index = bones.size,
                parent = bones.getOrNull(buf.int),
                position = Vector3f(buf.float, buf.float, buf.float),
                rotation = Quaternion(buf.float, buf.float, buf.float, buf.float).apply { normalise() },
                scale = Vector3f(buf.float, buf.float, buf.float)
        )
    }

    private fun readAnimation(buf: ByteBuffer, text: ByteArray, keyframes: List<Keyframe>): Pair<String, Animation> {
        val name = readNulString(text, buf.int)
        val firstFrame = buf.int
        val frames = buf.int
        val framerate = buf.float
        val loop = buf.int == 1
        val animKeyframes = keyframes.slice(firstFrame until firstFrame + frames).toTypedArray()
        return name to Animation(animKeyframes, framerate, loop)
    }

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
     * Read triangles from list of iqmtriangles directly to VBO. VBO keeps binded
     * @param buf iqm file byte buffer. Buffer's position doesn't matter
     * @param offset offset to beginning of iqmtriangles' list
     * @param triangles number of triangles
     * @return id of IBO
     */
    private fun readIndicesToVBO(buf: ByteBuffer, offset: Int, triangles: Int): Int {
        buf.position(offset)

        val indicesBuf = BufferUtils.createIntBuffer(triangles * 3)
        repeat(triangles) {
            val i1 = buf.int
            val i2 = buf.int
            val i3 = buf.int
            indicesBuf.put(i3)
            indicesBuf.put(i2)
            indicesBuf.put(i1) // Add in reverse order thus vertices order is CCW
        }
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

    private fun loadTexture(material: String) =
            if (material.isNotEmpty()) {
                val path = ResourceLocation(ModClass.DOMAIN, "textures/$material.png")
                val texture = SimpleTexture(path)
                if (minecraft.textureManager.loadTexture(path, texture))
                    texture.glTextureId
                else
                    null
            } else null


    // Helpful structs

    private data class Header(
            val magic: String,
            val version: Int,
            val filesize: Int,
            val flags: Int,
            val textSize: Int,
            val textOffset: Int,
            val meshesNum: Int,
            val meshesOffset: Int,
            val vertexarraysNum: Int,
            val verticesNum: Int,
            val vertexarraysOffset: Int,
            val trianglesNum: Int,
            val trianglesOffset: Int,
            val adjacencyOffset: Int,
            val bonesNum: Int,
            val bonesOffset: Int,
            val posesNum: Int,
            val posesOffset: Int,
            val animsNum: Int,
            val animsOffset: Int,
            val framesNum: Int,
            val framechannelsNum: Int,
            val framedataOffset: Int,
            val boundsOffset: Int
            // Other fields're unused
    )

    private class Pose(val parent: Int, val mask: Int, val offset: FloatArray, val scale: FloatArray)

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

    private const val IQM_MAGIC = "INTERQUAKEMODEL\u0000"
    private const val IQM_VERSION = 2
}