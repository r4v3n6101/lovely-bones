package skeletal.model.animated

import net.minecraft.util.AxisAlignedBB
import skeletal.graphics.VertexArrayObject
import skeletal.model.Mesh
import skeletal.model.StaticModel

class AnimatedModel(
        vao: VertexArrayObject, meshes: Map<String, Mesh>, bound: AxisAlignedBB?,
        val skeleton: Map<String, Bone>?, val animations: Map<String, Animation>? // TODO Reduce null ammount
) : StaticModel(vao, meshes, bound) {
    override fun getType() = "AnimatedModel"
}