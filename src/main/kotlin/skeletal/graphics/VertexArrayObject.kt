@file:Suppress("NOTHING_TO_INLINE")

package skeletal.graphics

import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glDeleteVertexArrays

class VertexArrayObject(private val id: Int, private val vbos: IntArray) : GLObject {

    override fun free() {
        vbos.forEach(::glDeleteBuffers)
        glDeleteVertexArrays(id)
    }

    override fun bind() = glBindVertexArray(id)
    override fun unbind() = glBindVertexArray(0)
}