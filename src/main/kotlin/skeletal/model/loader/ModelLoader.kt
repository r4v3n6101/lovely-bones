package skeletal.model.loader

import skeletal.model.StaticModel

interface ModelLoader {
    fun loadModel(data: ByteArray): StaticModel // TODO : Reduce abstraction layer
}