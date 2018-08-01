package skeletal.model.loader

import skeletal.model.StaticModel

interface StaticModelLoader{
    fun loadStaticModel(data: ByteArray): StaticModel
}