package skeletal

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniformMatrix4
import org.lwjgl.util.vector.Matrix4f
import skeletal.model.StaticModel
import skeletal.model.loader.impl.IQMLoader


@Mod(modid = "skeletal_anim")
class ModColladaModel {

    val staticModel: StaticModel by lazy {
        ResourceLocation(DOMEN, "models/model.iqm").inputStream.use {
            IQMLoader.loadModel(it.readBytes())
        }
    }

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        if (event.side == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(this)
            FMLCommonHandler.instance().bus().register(this)
        }
    }

    // Test
    @SubscribeEvent
    fun worldRenderer(event: RenderWorldLastEvent) {
        updateMatricesUniform()
        val buf = BufferUtils.createFloatBuffer(16)
        Matrix4f().store(buf)
        buf.flip()
        STATIC_MODEL_SHADER.use {
            glUniformMatrix4(glGetUniformLocation(program, "model"), false, buf)
            staticModel.renderAll()
        }
    }
}