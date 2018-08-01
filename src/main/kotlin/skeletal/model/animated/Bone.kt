package skeletal.model.animated

import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f
import skeletal.math.transformPos

class Bone(
        val id: Int, // TODO
        val parent: Int, // TODO : Is it necessary?
        val baseRotation: Quaternion, val basePosition: Vector3f,
        val inverseBaseRotation: Quaternion = baseRotation.negate(null),
        val inverseBasePosition: Vector3f = basePosition.negate(null).let { transformPos(inverseBaseRotation, it, it) }
)