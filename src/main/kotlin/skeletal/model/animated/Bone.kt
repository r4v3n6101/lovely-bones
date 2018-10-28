package skeletal.model.animated

import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import skeletal.math.DualQuat

class Bone(
        val index: Int, // Used for fast indexing of transforms
        val parent: Bone?,
        val position: Vector3f,
        val rotation: Quaternion,
        val scale: Vector3f
) {

    val baseTransform: DualQuat by lazy {
        val transform = DualQuat.fromQuatAndTranslation(rotation, position)
        val parentBone = parent
        if (parentBone != null) {
            DualQuat.mul(parentBone.baseTransform, transform, transform)
        }
        transform
    }

    val inverseBaseTransform: DualQuat by lazy {
        DualQuat(baseTransform).negate()
    }
}