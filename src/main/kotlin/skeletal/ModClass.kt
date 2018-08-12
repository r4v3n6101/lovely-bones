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
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20.*
import org.lwjgl.util.vector.Matrix3f
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector3f
import skeletal.model.animated.AnimatedModel
import skeletal.model.animated.Animator
import skeletal.model.loader.impl.IQMLoader


@Mod(modid = "skeletal_anim")
class ModColladaModel {

    val model: AnimatedModel by lazy {
        AdvancedModelLoader.loadModel(ResourceLocation(DOMEN, "models/model.iqm")) as AnimatedModel
    }

    val uniformLocations: IntArray by lazy {
        ANIMATED_MODEL_SHADER.getUniformLocations(
                "textureSampler", "lightmapSampler", "lightmapTexcoord", "model", "inverseTransposeModel", "transforms"
        )
    }

    val animator by lazy {
        val an = Animator()
        an.playAnimation(model.animations.values.first())
        an
    }

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        AdvancedModelLoader.registerModelHandler(MinecraftModelLoader(arrayOf("iqm"), "IQM Model", IQMLoader))
        // Test
        if (event.side == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(this)
            FMLCommonHandler.instance().bus().register(this)
        }
    }

    // Test
    @SubscribeEvent
    fun worldRenderer(event: RenderWorldLastEvent) {
        updateMatricesUniform()
        animator.update(event.partialTicks)
        skeletonCacheBuf.clear()
        animator.storeSkeletonData(skeletonCacheBuf)
        skeletonCacheBuf.flip()

        val light = minecraft.theWorld.getLightBrightnessForSkyBlocks(
                renderPosX.toInt(), renderPosY.toInt(), renderPosZ.toInt(), 0
        ).toFloat()

        glFrontFace(GL_CW)
        minecraft.entityRenderer.enableLightmap(666.666)

        ANIMATED_MODEL_SHADER.use {
            glUniform1i(uniformLocations[0], 0)
            glUniform1i(uniformLocations[1], 1)

            val lightU = (light % 65536 + 8f) / 256
            val lightV = (light / 65536 + 8f) / 256
            glUniform2f(uniformLocations[2], lightU, lightV)

            glUniform4(uniformLocations[5], skeletonCacheBuf)
            repeat(3) { i ->
                repeat(3) { j ->
                    val modelBuf = BufferUtils.createFloatBuffer(16)
                    val modelMat = Matrix4f()
                    modelMat
                            .translate(
                                    Vector3f(
                                            (5 + 3 * j) - renderPosX.toFloat(),
                                            4 - renderPosY.toFloat(),
                                            (5 + 3 * i) - renderPosZ.toFloat()
                                    )
                            )
                            .rotate(45.5f, Vector3f(-1f, 0f, 0f))
                            .store(modelBuf)
                    modelBuf.flip()
                    glUniformMatrix4(uniformLocations[3], false, modelBuf)

                    val itmBuf = BufferUtils.createFloatBuffer(9) // Ugliest code ever
                    makeInverseTranspose(modelMat).store(itmBuf)
                    itmBuf.flip()
                    glUniformMatrix3(uniformLocations[4], false, itmBuf)

                    model.renderAll()
                }
            }
        }
        glFrontFace(GL_CCW)
        minecraft.entityRenderer.disableLightmap(666.666)
    }

    private fun makeInverseTranspose(modelMat: Matrix4f): Matrix3f {
        val mat = Matrix3f()
        mat.m00 = modelMat.m00
        mat.m01 = modelMat.m10
        mat.m02 = modelMat.m20

        mat.m10 = modelMat.m01
        mat.m11 = modelMat.m11
        mat.m12 = modelMat.m21

        mat.m20 = modelMat.m02
        mat.m21 = modelMat.m12
        mat.m22 = modelMat.m22
        mat.invert()
        return mat
    }
}