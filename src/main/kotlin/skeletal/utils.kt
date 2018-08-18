package skeletal

import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import java.io.InputStream

// TODO : New name to file

val minecraft: Minecraft
    get() = Minecraft.getMinecraft()

val ResourceLocation.inputStream: InputStream?
    get() = minecraft.resourceManager.getResource(this)?.inputStream