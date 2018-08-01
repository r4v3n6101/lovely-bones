package skeletal.math

import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Quaternion.mulInverse
import org.lwjgl.util.vector.Vector3f

fun mulPure(lhs: Quaternion, rhs: Vector3f) = Quaternion(
        lhs.w * rhs.x + lhs.y * rhs.z - lhs.z * rhs.y,
        lhs.w * rhs.y + lhs.z * rhs.x - lhs.x * rhs.z,
        lhs.w * rhs.z + lhs.x * rhs.y - lhs.y * rhs.x,
        -lhs.x * rhs.x - lhs.y * rhs.y - lhs.z * rhs.z
)

fun transformPos(quat: Quaternion, point: Vector3f, dest: Vector3f?): Vector3f {
    val out = dest ?: Vector3f()
    val tmp = mulPure(quat, point)
    mulInverse(tmp, quat, tmp)
    out.set(tmp.x, tmp.y, tmp.z)
    return out
}