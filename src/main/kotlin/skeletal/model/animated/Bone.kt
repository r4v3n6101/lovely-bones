package skeletal.model.animated

import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Matrix4f.mul
import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import skeletal.math.buildTransform

class Bone(
        val name: String,
        val parent: Bone?, // null if root
        val position: Vector3f,
        val rotation: Quaternion,
        val scale: Vector3f
) {

    val baseTransform: Matrix4f by lazy {
        val transform = buildTransform(rotation, scale, position)
        val parentBone = parent
        if (parentBone != null) {
            mul(parentBone.baseTransform, transform, transform)
        }
        transform
    }

    val inverseBaseTransform: Matrix4f by lazy {
        Matrix4f.invert(baseTransform, null)
    }
}