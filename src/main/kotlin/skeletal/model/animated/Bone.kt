package skeletal.model.animated

import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import skeletal.math.DualQuat

class Bone(
        val name: String,
        val parentIndex: Int,
        val position: Vector3f,
        val rotation: Quaternion,
        val scale: Vector3f
) {

    fun calculateBaseTransform(skeleton: Array<Bone>) {
        val transform = DualQuat.fromQuatAndTranslation(rotation, position)
        if (parentIndex != -1) {
            DualQuat.mul(skeleton[parentIndex].baseTransform, transform, transform)
        }
        baseTransform = transform
    }

    var baseTransform = DualQuat()
        private set

    val inverseBaseTransform: DualQuat by lazy {
        DualQuat.invert(baseTransform, null)
    }
}