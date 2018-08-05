package skeletal.math

import org.lwjgl.util.vector.*
import java.nio.FloatBuffer

/**
 * Represent dual quaternion q0 + q1 * e, where's e is epsilon and e^2 == 0
 */
class DualQuat(var q0: Quaternion, var q1: Quaternion) : Vector(), ReadableVector {

    /**
     * Scale each quat
     */
    override fun scale(scale: Float): DualQuat {
        q0.scale(scale)
        q1.scale(scale)
        return this
    }

    /**
     * Same as ||q0||^2
     */
    override fun lengthSquared() = q0.lengthSquared()

    /**
     * Sequently write q0 & q1
     */
    override fun store(buf: FloatBuffer): Vector {
        q0.store(buf)
        q1.store(buf)
        return this
    }

    /**
     * Negate each quat
     */
    override fun negate(): Vector {
        q0.negate()
        q1.negate()
        return this
    }

    /**
     * Load q0 & q1 sequently, q0 is first
     */
    override fun load(buf: FloatBuffer): Vector {
        q0.load(buf)
        q1.load(buf)
        return this
    }

    fun lerp(dq1: DualQuat, t: Float, dest: DualQuat?): DualQuat {
        val out = dest ?: DualQuat(Quaternion(), Quaternion())
        out.q0.x = q0.x + t * (dq1.q0.x - q0.x)
        out.q0.y = q0.y + t * (dq1.q0.y - q0.y)
        out.q0.z = q0.z + t * (dq1.q0.z - q0.z)
        out.q0.w = q0.w + t * (dq1.q0.w - q0.w)

        out.q1.x = q1.x + t * (dq1.q1.x - q1.x)
        out.q1.y = q1.y + t * (dq1.q1.y - q1.y)
        out.q1.z = q1.z + t * (dq1.q1.z - q1.z)
        out.q1.w = q1.w + t * (dq1.q1.w - q1.w)

        return out
    }

    companion object {
        fun fromQuatAndTranslation(q: Quaternion, t: Vector3f) = DualQuat(q, mulPure(q, t).scale(0.5f) as Quaternion)

        fun fromMatrixAndTranslation(basis: Matrix3f, t: Vector3f): DualQuat {
            val q = Quaternion()
            q.setFromMatrix(basis)
            return fromQuatAndTranslation(q, t)
        }

        fun fromMatrix(matrix: Matrix4f): DualQuat {
            val q = Quaternion()
            q.setFromMatrix(matrix)
            val t = Vector3f(matrix.m30, matrix.m31, matrix.m32)
            return fromQuatAndTranslation(q, t)
        }
    }
}