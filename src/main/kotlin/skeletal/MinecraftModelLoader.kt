package skeletal

import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.model.IModelCustomLoader
import skeletal.model.loader.ModelLoader

class MinecraftModelLoader(
        @JvmField val suffixes: Array<String>,
        @JvmField val type: String,
        @JvmField val modelLoader: ModelLoader
) : IModelCustomLoader {

    override fun getSuffixes() = suffixes
    override fun getType() = type

    override fun loadInstance(resource: ResourceLocation) = modelLoader.loadModel(
            resource.inputStream.use { it.readBytes() }
    )
}