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

fun transformPoint(q: Quaternion, v: Vector3f, dest: Vector3f?): Vector3f {
    val out = dest ?: Vector3f()
    val qvdot = q.x * v.x + q.y * v.y + q.z * v.z
    val a = q.w * q.w - (q.x * q.x + q.y * q.y + q.z * q.z)

    val crossX = q.y * v.z - q.z * v.y
    val crossY = v.x * q.z - v.z * q.x
    val crossZ = q.x * v.y - q.y * v.x

    out.set(
            2f * (qvdot * q.x + q.w * crossX) + a * v.x,
            2f * (qvdot * q.y + q.w * crossY) + a * v.y,
            2f * (qvdot * q.z + q.w * crossZ) + a * v.z
    )
    return out
}

@Deprecated("Replace to method")
fun buildTransform(q: Quaternion, s: Vector3f, t: Vector3f): Matrix4f {
    val mat = Matrix4f()
    mat.scale(s)

    val xx = q.x * q.x
    val xy = q.x * q.y
    val xz = q.x * q.z
    val xw = q.x * q.w
    val yy = q.y * q.y
    val yz = q.y * q.z
    val yw = q.y * q.w
    val zz = q.z * q.z
    val zw = q.z * q.w

    mat.m00 = 1 - 2 * (yy + zz)
    mat.m10 = 2 * (xy - zw)
    mat.m20 = 2 * (xz + yw)
    mat.m01 = 2 * (xy + zw)
    mat.m11 = 1 - 2 * (xx + zz)
    mat.m21 = 2 * (yz - xw)
    mat.m02 = 2 * (xz - yw)
    mat.m12 = 2 * (yz + xw)
    mat.m22 = 1 - 2 * (xx + yy)

    mat.translate(t)
    return mat
}