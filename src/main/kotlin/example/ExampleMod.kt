package example

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.relauncher.Side
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.client.model.AdvancedModelLoader
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.input.Keyboard
import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import org.lwjgl.util.vector.Vector4f
import skeletal.adapted.AdaptedModel
import skeletal.adapted.RenderType
import skeletal.math.DualQuat
import skeletal.minecraft
import skeletal.model.animated.AnimatedModel
import java.lang.Math.toRadians

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
        val player = minecraft.thePlayer
        val p = Quaternion()
        p.setFromAxisAngle(Vector4f(1f, 0f, 0f, 0.01745f * (player.rotationPitch - 40)))
        val headTransform = DualQuat.fromQuatAndTranslation(p, Vector3f())

        modelAdapted.setPosition(5f, 4f, 5f)
        modelAdapted.setAngles(-toRadians(90.0).toFloat(), 0f, toRadians(90.0).toFloat())
        modelAdapted.setScale(0.3f, 0.3f, 0.3f)

        if (modelAdapted.animator.animations.size > 0) { // FIXME: More generally way to do it
            modelAdapted.animator.transforms = modelAdapted.animator.calculateAnimationsTransforms()
        }
        val headIndex = modelAdapted.animatedModel.skeleton.indexOfFirst { it.name == "Head" } // Don't use often due to O(N) search
        modelAdapted.animator.transforms[headIndex] = headTransform

        modelAdapted.render(RenderType.All)
    }

    @SubscribeEvent
    fun tick(event: TickEvent.ClientTickEvent) {
        if (Keyboard.isKeyDown(Keyboard.KEY_K)) {
            modelAdapted.play("idle", 1f)
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_P) && modelAdapted.isPlaying("idle")) {
            modelAdapted.pause("idle")
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_R) && modelAdapted.isPlaying("idle")) {
            modelAdapted.resume("idle")
        }
    }

    private val modelAdapted by lazy { AdaptedModel(testModel) }
    private val testModel by lazy { AdvancedModelLoader.loadModel(ResourceLocation("skeletal:models/mrfixit.iqm")) as AnimatedModel } // FIXME: Long loadings
}