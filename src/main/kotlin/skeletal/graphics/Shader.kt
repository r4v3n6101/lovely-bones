@file:Suppress("NOTHING_TO_INLINE")

package skeletal.graphics

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20.*

class Shader(val program: Int, val shaders: IntArray) : Cleanable {

    override fun free() {
        shaders.forEach {
            glDetachShader(program, it)
            glDeleteShader(it)
        }
        glDeleteProgram(program)
    }

    inline fun bind() = glUseProgram(program)
    inline fun unbind() = glUseProgram(0)

    inline fun use(action: Shader.() -> Unit) {
        bind()
        action()
        unbind()
    }

    companion object {

        fun createShader(type: Int, srcCode: String): Int {
            val shader = glCreateShader(type)
            glShaderSource(shader, srcCode)
            glCompileShader(shader)
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL11.GL_FALSE)
                println(glGetShaderInfoLog(shader, GL_INFO_LOG_LENGTH))
            return shader
        }

        fun createProgram(vararg shaders: Int): Shader {
            val program = glCreateProgram()
            shaders.forEach { glAttachShader(program, it) }
            glLinkProgram(program)
            glValidateProgram(program)
            if (glGetProgrami(program, GL_VALIDATE_STATUS) == GL11.GL_FALSE)
                println(glGetProgramInfoLog(program, GL_INFO_LOG_LENGTH))
            return Shader(program, shaders)
        }

        inline fun Shader.bindAttributes(vararg attributes: Pair<Int, String>) {
            attributes.forEach { (index, name) -> glBindAttribLocation(program, index, name) }
        }
    }
}