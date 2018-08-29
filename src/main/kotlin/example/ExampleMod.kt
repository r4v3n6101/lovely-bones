package example

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.client.model.AdvancedModelLoader
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.input.Keyboard
import skeletal.adapted.AdaptedModel
import skeletal.adapted.RenderType
import skeletal.model.animated.AnimatedModel

@Mod(modid = "example_skeletal")
class ExampleMod {
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        if (event.side == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(this)
            FMLCommonHandler.instance().bus().register(this)
        }
    }

    @SubscribeEvent
    fun worldRenderer(event: RenderWorldLastEvent) {
        modelAdapted.setPosition(5f, 10f, 5f)
        modelAdapted.setAngles(Math.toRadians(-90.0).toFloat(), 0f, 0f)
        modelAdapted.setScale(0.04f, 0.04f, 0.04f)
        modelAdapted.render(RenderType.All)
        if (Keyboard.isKeyDown(Keyboard.KEY_K)) {
            modelAdapted.play("idle2", 1f)
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_P) && modelAdapted.isPlaying("idle2")) {
            modelAdapted.pause("idle2")
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_R) && modelAdapted.isPlaying("idle2")) {
            modelAdapted.resume("idle2")
        }
    }

    private val modelAdapted: AdaptedModel by lazy {
        AdaptedModel(AdvancedModelLoader.loadModel(ResourceLocation("skeletal:models/hellknight.iqm")) as AnimatedModel)
    }
}