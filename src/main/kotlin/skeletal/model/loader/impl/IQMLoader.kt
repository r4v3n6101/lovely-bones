package skeletal.model.loader.impl

import net.minecraft.util.AxisAlignedBB
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30.*
import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import skeletal.graphics.VertexArrayObject
import skeletal.math.DualQuat
import skeletal.math.transformPos
import skeletal.model.Mesh
import skeletal.model.StaticModel
import skeletal.model.animated.AnimatedModel
import skeletal.model.animated.Animation
import skeletal.model.animated.Bone
import skeletal.model.animated.Keyframe
import skeletal.model.loader.AnimatedModelLoader
import skeletal.model.loader.StaticModelLoader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object IQMLoader : StaticModelLoader, AnimatedModelLoader {

    override fun loadStaticModel(data: ByteArray): StaticModel {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        // Assume file is correct

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
        buf.position(buf.position() + 4 * 10) // skip unused animation data
        val boundsOffset = buf.int

        buf.position(textOffset)
        val text = ByteArray(textSize) { buf.get() }

        val vaoId = glGenVertexArrays()
        glBindVertexArray(vaoId)
        buf.position(vertexarraysOffset)
        val vbos = IntArray(vertexarraysNum + 1) { readVertexArrayToVBO(buf, verticesNum) }
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

            meshes[meshName] = Mesh(material, firstTri * 3, numTris * 3)
        }

        buf.position(boundsOffset)
        val aabb = AxisAlignedBB.getBoundingBox(
                buf.float.toDouble(), buf.float.toDouble(), buf.float.toDouble(), // min
                buf.float.toDouble(), buf.float.toDouble(), buf.float.toDouble() // max
        )
        return StaticModel(VertexArrayObject(vaoId, vbos), meshes, aabb)
    }

    override fun loadAnimatedModel(data: ByteArray, model: StaticModel): AnimatedModel {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        buf.position(16 + 3 * 4) // Skip unused fields

        val textNum = buf.int
        val textOffset = buf.int

        buf.position(buf.position() + 8 * 4) // Skip unused mesh data

        val bonesNum = buf.int // or joints
        val bonesOffset = buf.int
        val posesNum = buf.int
        val posesOffset = buf.int
        val animsNum = buf.int
        val animsOffset = buf.int
        val framesNum = buf.int
        val framechannelsNum = buf.int
        val framedataOffset = buf.int

        buf.position(textOffset)
        val text = ByteArray(textNum) { buf.get() }

        buf.position(bonesOffset)
        val bones = ArrayList<Pair<String, Bone>>(bonesNum)
        repeat(bonesNum) {
            val name = readNulString(text, buf.int)
            val parent = buf.int
            val translate = Vector3f(buf.float, buf.float, buf.float)
            val rotate = Quaternion(buf.float, buf.float, buf.float, buf.float)
            buf.float; buf.float; buf.float // skip scale
            bones += name to Bone(it, parent, rotate, translate)
        }

        val framedata = buf.slice()
        framedata.position(framedataOffset)

        val keyframes = ArrayList<Keyframe>(framesNum * posesNum)
        buf.position(posesOffset)
        for (i in 0 until framesNum) {
            val transforms = ArrayList<DualQuat>(posesNum)
            for (j in 0 until posesNum) {
                val parent = buf.int // TODO
                val channelMask = buf.int
                val channeloffset = floatArrayOf(
                        buf.float, buf.float, buf.float, // Translation
                        buf.float, buf.float, buf.float, buf.float, // Rotation (Quaternion)
                        buf.float, buf.float, buf.float // Scale (unused)
                )

                for (channel in channeloffset.indices)
                    if ((channelMask shr channel) and 1 == 1) // If mask contains bit in @channel position scale data
                        channeloffset[channel] += buf.float * framedata.short

                val q = Quaternion(channeloffset[3], channeloffset[4], channeloffset[5], channeloffset[6])
                val p = Vector3f(channeloffset[0], channeloffset[1], channeloffset[2])
                // Take in account bones space, so...
                val bone = bones[j].second
                Vector3f.add(transformPos(q, bone.inverseBasePosition, null), p, p)
                Quaternion.mul(bone.inverseBaseRotation, q, q)

                val transformation = DualQuat.fromQuatAndTranslation(q, p)
                transforms.add(transformation)
            }
            keyframes.add(Keyframe(transforms.toTypedArray()))
        }

        buf.position(animsOffset)
        val anims: HashMap<String, Animation> = HashMap(animsNum)
        repeat(animsNum) {
            val name = readNulString(text, buf.int)
            val firstFrame = buf.int
            val frames = buf.int
            val framerate = buf.float
            val loop = buf.int == 1
            val animKeyframes = keyframes.slice(firstFrame..frames).toTypedArray()
            anims[name] = Animation(animKeyframes, framerate, loop)
        }
        return AnimatedModel(model, bones.toMap(), anims)
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
        val size = buf.int
        val offset = buf.int

        val output = BufferUtils.createByteBuffer(vertices * size * format.size)
        output.put(ByteArray(vertices * size * format.size).run { buf.get(this, offset, size) })
        output.flip()
        val vboId = GL15.glGenBuffers()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, output, GL15.GL_STATIC_DRAW)
        GL20.glVertexAttribPointer(type.ordinal, size, format.glRepr, false, 0, 0)
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
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
        val array = ByteArray(indices * 4)
        buf.get(array, offset, array.size)

        val indicesBuf = BufferUtils.createByteBuffer(array.size) // a int requires 4 bytes
        indicesBuf.put(array) // Copy from original buf
        indicesBuf.flip()

        val vboId = GL15.glGenBuffers()
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboId)
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuf, GL15.GL_STATIC_DRAW) // TODO : Will slice of @buf modified if passed
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
        BYTE(1, GL11.GL_BYTE),
        UBYTE(1, GL11.GL_UNSIGNED_BYTE),
        SHORT(2, GL11.GL_SHORT),
        USHORT(2, GL11.GL_UNSIGNED_SHORT),
        INT(4, GL11.GL_INT),
        UINT(4, GL11.GL_UNSIGNED_INT),
        HALF(2, GL_HALF_FLOAT),
        FLOAT(4, GL11.GL_FLOAT),
        DOUBLE(8, GL11.GL_DOUBLE)
    }
}