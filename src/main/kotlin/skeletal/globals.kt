package skeletal

import net.minecraft.util.ResourceLocation
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.glBufferSubData
import org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL20.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER
import skeletal.graphics.Shader
import skeletal.graphics.Shader.Companion.createProgram
import skeletal.graphics.Shader.Companion.createShader
import skeletal.graphics.UniformBufferObject
import java.io.BufferedReader
import java.nio.FloatBuffer

const val DOMEN = "skeletal"

private val projectionBuf: FloatBuffer = BufferUtils.createFloatBuffer(16)
private val modelviewBuf: FloatBuffer = BufferUtils.createFloatBuffer(16)
private val matricesBuf: FloatBuffer = BufferUtils.createFloatBuffer(32)

fun updateMatricesUniform() {
    projectionBuf.clear()
    modelviewBuf.clear()

    glGetFloat(GL_PROJECTION_MATRIX, projectionBuf)
    glGetFloat(GL_MODELVIEW_MATRIX, modelviewBuf)

    matricesBuf.clear()
    repeat(projectionBuf.capacity()) { matricesBuf.put(projectionBuf.get()) }
    repeat(modelviewBuf.capacity()) { matricesBuf.put(modelviewBuf.get()) }
    matricesBuf.flip()

    MATRICES_UNIFORM_BLOCK.use { glBufferSubData(GL_UNIFORM_BUFFER, 0, matricesBuf) }
}

val MATRICES_UNIFORM_BLOCK: UniformBufferObject by lazy { UniformBufferObject.createUBO(0, 128) }

val STATIC_MODEL_SHADER: Shader by lazy {
    val vertexText = ResourceLocation(DOMEN, "shaders/static_vertex.glsl").inputStream.bufferedReader()
            .use(BufferedReader::readText)
    val fragText = ResourceLocation(DOMEN, "shaders/static_fragment.glsl").inputStream.bufferedReader()
            .use(BufferedReader::readText)

    val vertexShader = createShader(GL_VERTEX_SHADER, vertexText)
    val fragShader = createShader(GL_FRAGMENT_SHADER, fragText)
    val shader = createProgram(vertexShader, fragShader)
    MATRICES_UNIFORM_BLOCK.connectWithShader(shader, "Matrices") // TODO : Replace lazy method to init
    shader
}

/*val animatedModelShader: Shader by lazy {
    val vertexPath = ResourceLocation(DOMEN, "shaders/animated_vertex.glsl")
    val fragPath = ResourceLocation(DOMEN, "shaders/animated_frag.glsl")

    val vertexText = vertexPath.inputStream.bufferedReader().use(BufferedReader::readText)
    val fragText = fragPath.inputStream.bufferedReader().use(BufferedReader::readText)

    val vertexShader = createShader(GL_VERTEX_SHADER, vertexText)
    val fragShader = createShader(GL_VERTEX_SHADER, fragText)

    createProgram(vertexShader, fragShader)
}*/