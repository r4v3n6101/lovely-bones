@file:Suppress("NOTHING_TO_INLINE")

package skeletal.graphics

import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glDeleteVertexArrays

class VertexArrayObject(val id: Int, val vbos: IntArray) : Cleanable {

    override fun free() {
        vbos.forEach(::glDeleteBuffers)
        glDeleteVertexArrays(id)
    }

    inline fun bind() = glBindVertexArray(id)
    inline fun unbind() = glBindVertexArray(0)

    inline fun use(action: () -> Unit) {
        bind()
        action()
        unbind()
    }
}