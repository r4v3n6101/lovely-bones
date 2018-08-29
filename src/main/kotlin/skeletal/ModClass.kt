package skeletal

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import net.minecraft.client.renderer.entity.RenderManager.*
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.client.model.AdvancedModelLoader
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.GL21.glUniformMatrix2x4
import org.lwjgl.opengl.GL31
import skeletal.adapted.AdaptedModel
import skeletal.adapted.RenderType
import skeletal.graphics.Shader
import skeletal.graphics.UniformBufferObject
import skeletal.model.loader.IQMLoader
import java.io.BufferedReader
import java.nio.FloatBuffer


@Mod(modid = "skeletal_anim")
class ModClass {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        AdvancedModelLoader.registerModelHandler(IQMLoader)
        if (event.side == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(this)
            FMLCommonHandler.instance().bus().register(this)
        }
    }

    @SubscribeEvent
    fun worldRenderer(event: RenderWorldLastEvent) {
        updateMatricesUniform()
        minecraft.entityRenderer.enableLightmap(666.666)
        ANIMATED_MODEL_SHADER.use {
            modelsToRender.forEach { (model, renderType) ->
                renderSkeletalModel(model, renderType)
            }
        }
        minecraft.entityRenderer.disableLightmap(666.666)
        lastRenderTime = System.currentTimeMillis()
        modelsToRender.clear() // Reset after rendering
    }

    private fun renderSkeletalModel(model: AdaptedModel, renderType: RenderType) {
        with(model) {
            updateMatrices(renderPosX.toFloat(), renderPosY.toFloat(), renderPosZ.toFloat())
            animator.update((System.currentTimeMillis() - lastRenderTime) * 0.001f)

            val light = minecraft.theWorld.getLightBrightnessForSkyBlocks(
                    position.x.toInt(), position.y.toInt(), position.z.toInt(), 0
            ).toFloat()
            val lightU = (light % 65536 + 8f) / 256f
            val lightV = (light / 65536 + 8f) / 256f

            skeletonCacheBuf.clear()
            animator.storeSkeletonData(skeletonCacheBuf)
            skeletonCacheBuf.flip()

            modelBuf.clear()
            modelMatrix.store(modelBuf)
            modelBuf.flip()

            inverseTransposeBuf.clear()
            inverseTransposeMatrix.store(inverseTransposeBuf)
            inverseTransposeBuf.flip()

            glUniform1i(animatedShaderUniforms[0], 0)
            glUniform1i(animatedShaderUniforms[1], 1)
            glUniform2f(animatedShaderUniforms[2], lightU, lightV)
            glUniformMatrix4(animatedShaderUniforms[3], false, modelBuf)
            glUniformMatrix3(animatedShaderUniforms[4], false, inverseTransposeBuf)
            glUniformMatrix2x4(animatedShaderUniforms[5], false, skeletonCacheBuf)

            when (renderType) {
                RenderType.All -> animatedModel.renderAll()
                is RenderType.Only -> animatedModel.renderOnly(*renderType.names)
                is RenderType.Part -> animatedModel.renderPart(renderType.name)
                is RenderType.Except -> animatedModel.renderAllExcept(*renderType.names)
            }
        }
    }

    companion object {
        // AdaptedModel data
        val modelsToRender: ArrayList<Pair<AdaptedModel, RenderType>> = ArrayList(1024)
        private var lastRenderTime = System.currentTimeMillis()
        private val modelBuf = BufferUtils.createFloatBuffer(16)
        private val inverseTransposeBuf = BufferUtils.createFloatBuffer(9)
        private val skeletonCacheBuf: FloatBuffer by lazy {
            BufferUtils.createFloatBuffer(MAX_BONES * 8) // every bone is 8 floats
        }

        // Shaders data
        val animatedShaderUniforms: IntArray by lazy {
            ANIMATED_MODEL_SHADER.getUniformLocations(
                    "textureSampler",
                    "lightmapSampler",
                    "lightmapTexcoord",
                    "model",
                    "inverseTransposeModel",
                    "transforms"
            )
        }
        val ANIMATED_MODEL_SHADER: Shader by lazy {
            val vertexText = ResourceLocation(DOMAIN, "shaders/animated_vertex.glsl").inputStream!!.bufferedReader()
                    .use(BufferedReader::readText).replace("{MAX_BONES}", "$MAX_BONES")
            val fragText = ResourceLocation(DOMAIN, "shaders/model_fragment.glsl").inputStream!!.bufferedReader()
                    .use(BufferedReader::readText)

            val vertexShader = Shader.createShader(GL_VERTEX_SHADER, vertexText)
            val fragShader = Shader.createShader(GL_FRAGMENT_SHADER, fragText)

            val shader = Shader.createProgram(vertexShader, fragShader)
            MATRICES_UNIFORM_BLOCK.connectWithShader(shader, "Matrices")
            shader
        }

        // UniformBuffer data
        private val projectionBuf: FloatBuffer = BufferUtils.createFloatBuffer(16)
        private val modelviewBuf: FloatBuffer = BufferUtils.createFloatBuffer(16)
        private val matricesBuf: FloatBuffer = BufferUtils.createFloatBuffer(32)
        val MATRICES_UNIFORM_BLOCK: UniformBufferObject by lazy { UniformBufferObject.createUBO(0, 128) }

        fun updateMatricesUniform() {
            projectionBuf.clear()
            modelviewBuf.clear()

            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuf)
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelviewBuf)

            matricesBuf.clear()
            repeat(projectionBuf.capacity()) { matricesBuf.put(projectionBuf.get()) }
            repeat(modelviewBuf.capacity()) { matricesBuf.put(modelviewBuf.get()) }
            matricesBuf.flip()

            MATRICES_UNIFORM_BLOCK.use { GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, matricesBuf) }
        }

        val DOMAIN: String = System.getProperty("skeletal.domain", "skeletal")
        val MAX_BONES: Int = System.getProperty("skeletal.bonesNum", "128").toInt()
    }
}