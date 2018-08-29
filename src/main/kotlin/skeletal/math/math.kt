package skeletal.math

import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.Vector3f


fun mulPure(lhs: Quaternion, rhs: Vector3f) = Quaternion(
        lhs.w * rhs.x + lhs.y * rhs.z - lhs.z * rhs.y,
        lhs.w * rhs.y + lhs.z * rhs.x - lhs.x * rhs.z,
        lhs.w * rhs.z + lhs.x * rhs.y - lhs.y * rhs.x,
        -lhs.x * rhs.x - lhs.y * rhs.y - lhs.z * rhs.z
)

fun buildTransform(rotation: Quaternion, scale: Vector3f, translation: Vector3f): Matrix4f {
    val out = Matrix4f()
    val xx = rotation.x * rotation.x
    val xy = rotation.x * rotation.y
    val xz = rotation.x * rotation.z
    val xw = rotation.x * rotation.w
    val yy = rotation.y * rotation.y
    val yz = rotation.y * rotation.z
    val yw = rotation.y * rotation.w
    val zz = rotation.z * rotation.z
    val zw = rotation.z * rotation.w

    out.m00 = (1 - 2 * (yy + zz)) * scale.x
    out.m10 = 2 * (xy - zw) * scale.y
    out.m20 = 2 * (xz + yw) * scale.z

    out.m01 = 2 * (xy + zw) * scale.x
    out.m11 = (1 - 2 * (xx + zz)) * scale.y
    out.m21 = 2 * (yz - xw) * scale.z

    out.m02 = 2 * (xz - yw) * scale.x
    out.m12 = 2 * (yz + xw) * scale.y
    out.m22 = (1 - 2 * (xx + yy)) * scale.z

    out.m30 = translation.x
    out.m31 = translation.y
    out.m32 = translation.z
    out.m33 = 1f

    return out
}

