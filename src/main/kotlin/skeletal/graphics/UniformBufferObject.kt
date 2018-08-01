package skeletal.graphics

import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30.glBindBufferBase
import org.lwjgl.opengl.GL31
import org.lwjgl.opengl.GL31.*

class UniformBufferObject(val vboId: Int, val bindingPoint: Int) : Cleanable {

    override fun free() = glDeleteBuffers(vboId)

    fun connectWithShader(program: Shader, name: String) =
            glUniformBlockBinding(program.program, glGetUniformBlockIndex(program.program, name), bindingPoint)

    fun bind() = glBindBuffer(GL_UNIFORM_BUFFER, vboId)
    fun unbind() = glBindBuffer(GL_UNIFORM_BUFFER, 0)

    inline fun use(action: () -> Unit) {
        bind()
        action()
        unbind()
    }

    companion object {
        fun createUBO(bindingPoint: Int, size: Long): UniformBufferObject {
            val id = glGenBuffers()
            glBindBuffer(GL31.GL_UNIFORM_BUFFER, id)
            glBufferData(GL31.GL_UNIFORM_BUFFER, size, GL_DYNAMIC_DRAW)
            glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0)
            glBindBufferBase(GL31.GL_UNIFORM_BUFFER, bindingPoint, id)
            return UniformBufferObject(id, bindingPoint)
        }
    }
}