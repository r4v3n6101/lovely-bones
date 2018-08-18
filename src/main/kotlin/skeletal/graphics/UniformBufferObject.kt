package skeletal.graphics

import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL30.glBindBufferBase
import org.lwjgl.opengl.GL31
import org.lwjgl.opengl.GL31.*

class UniformBufferObject(private val vboId: Int, private val bindingPoint: Int) : GLObject {

    override fun free() = glDeleteBuffers(vboId)

    override fun bind() = glBindBuffer(GL_UNIFORM_BUFFER, vboId)
    override fun unbind() = glBindBuffer(GL_UNIFORM_BUFFER, 0)

    fun connectWithShader(program: Shader, name: String) =
            glUniformBlockBinding(program.program, glGetUniformBlockIndex(program.program, name), bindingPoint)

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