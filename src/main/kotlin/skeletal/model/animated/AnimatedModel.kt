package skeletal.model.animated

import skeletal.graphics.VertexArrayObject
import skeletal.model.Mesh
import skeletal.model.StaticModel

class AnimatedModel(
        vao: VertexArrayObject,
        meshes: Map<String, Mesh>,
        val skeleton: List<Bone>,
        val animations: Map<String, Animation>
) : StaticModel(vao, meshes) {
    override fun getType() = "AnimatedModel"
}