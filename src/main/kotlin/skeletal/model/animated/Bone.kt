package skeletal.model.animated

import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import skeletal.math.buildTransform

class Bone(
        val index: Int, // Used for fast indexing of transforms
        val parent: Bone?,
        val position: Vector3f,
        val rotation: Quaternion,
        val scale: Vector3f
) {

    val baseTransform: Matrix4f by lazy {
        val transform = buildTransform(rotation, scale, position)
        val parentBone = parent
        if (parentBone != null)
            Matrix4f.mul(parentBone.baseTransform, transform, transform)
        transform
    }

    val inverseBaseTransform: Matrix4f by lazy { Matrix4f.invert(baseTransform, null) }

    /*val baseTransformDQ: DualQuat by lazy {
        val transform = DualQuat.fromQuatAndTranslation(rotation, position)
        val parentBone = parent
        if (parentBone != null)
            DualQuat.mul(parentBone.baseTransformDQ, transform, transform)
        transform
    }

    val inverseBaseTransformDQ: DualQuat by lazy {
        DualQuat(
                Quaternion(baseTransformDQ.real),
                Quaternion(baseTransformDQ.dual)
        ).negate()
    }*/
}