@file:Suppress("NOTHING_TO_INLINE")

package skeletal.graphics

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20.*

class Shader(val program: Int, private val shaders: IntArray) : Cleanable {

    override fun free() {
        shaders.forEach {
            glDetachShader(program, it)
            glDeleteShader(it)
        }
        glDeleteProgram(program)
    }

    fun bind() = glUseProgram(program)
    fun unbind() = glUseProgram(0)

    inline fun use(action: Shader.() -> Unit) {
        bind()
        this.action()
        unbind()
    }

    fun getUniformLocations(vararg names: String) =
            IntArray(names.size) { glGetUniformLocation(program, names[it]) }

    companion object {

        fun createShader(type: Int, srcCode: String): Int {
            val shader = glCreateShader(type)
            glShaderSource(shader, srcCode)
            glCompileShader(shader)
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL11.GL_FALSE)
                error(glGetShaderInfoLog(shader, GL_INFO_LOG_LENGTH))
            return shader
        }

        fun createProgram(vararg shaders: Int): Shader {
            val program = glCreateProgram()
            shaders.forEach { glAttachShader(program, it) }
            glLinkProgram(program)
            glValidateProgram(program)
            if (glGetProgrami(program, GL_VALIDATE_STATUS) == GL11.GL_FALSE)
                error(glGetProgramInfoLog(program, GL_INFO_LOG_LENGTH))
            return Shader(program, shaders)
        }
    }
}