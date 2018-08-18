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

fun buildTransform(q: Quaternion, s: Vector3f, t: Vector3f): Matrix4f {
    val out = Matrix4f()
    val xx = q.x * q.x
    val xy = q.x * q.y
    val xz = q.x * q.z
    val xw = q.x * q.w
    val yy = q.y * q.y
    val yz = q.y * q.z
    val yw = q.y * q.w
    val zz = q.z * q.z
    val zw = q.z * q.w

    out.m00 = (1 - 2 * (yy + zz)) * s.x
    out.m10 = 2 * (xy - zw) * s.y
    out.m20 = 2 * (xz + yw) * s.z

    out.m01 = 2 * (xy + zw) * s.x
    out.m11 = (1 - 2 * (xx + zz)) * s.y
    out.m21 = 2 * (yz - xw) * s.z

    out.m02 = 2 * (xz - yw) * s.x
    out.m12 = 2 * (yz + xw) * s.y
    out.m22 = (1 - 2 * (xx + yy)) * s.z

    out.m30 = t.x
    out.m31 = t.y
    out.m32 = t.z
    out.m33 = 1f

    return out
}