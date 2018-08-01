package skeletal.model.loader

import skeletal.model.StaticModel
import skeletal.model.animated.AnimatedModel

interface AnimatedModelLoader {
    fun loadAnimatedModel(data: ByteArray, model: StaticModel): AnimatedModel
}