package skeletal.math

import org.lwjgl.util.vector.Quaternion
import org.lwjgl.util.vector.ReadableVector
import org.lwjgl.util.vector.Vector
import org.lwjgl.util.vector.Vector3f
import java.nio.FloatBuffer

/**
 * Represent dual quaternion real + dual * e, where's e is epsilon and e^2 == 0
 */
data class DualQuat(
        val real: Quaternion = Quaternion(),
        val dual: Quaternion = Quaternion(0f, 0f, 0f, 0f)
) : Vector(), ReadableVector {

    constructor(dq: DualQuat) : this(Quaternion(dq.real), Quaternion(dq.dual))

    /**
     * Scale each quat
     */
    override fun scale(scale: Float): DualQuat {
        real.scale(scale)
        dual.scale(scale)
        return this
    }

    /**
     * Same as ||real||^2
     */
    override fun lengthSquared() = real.lengthSquared()

    /**
     * Sequently write real & dual
     */
    override fun store(buf: FloatBuffer): DualQuat {
        real.store(buf)
        dual.store(buf)
        return this
    }

    /**
     * Conjugate each quat
     */
    override fun negate(): DualQuat {
        real.negate()
        dual.negate()
        return this
    }

    /**
     * Load real & dual sequently, real is first
     */
    override fun load(buf: FloatBuffer): DualQuat {
        real.load(buf)
        dual.load(buf)
        return this
    }

    companion object {

        fun dot(dq0: DualQuat, dq1: DualQuat) = Quaternion.dot(dq0.real, dq1.real)

        fun mul(lhs: DualQuat, rhs: DualQuat, dest: DualQuat?): DualQuat {
            val out = dest ?: DualQuat()
            val real = Quaternion.mul(lhs.real, rhs.real, null)
            val q0 = Quaternion.mul(lhs.real, rhs.dual, null)
            val q1 = Quaternion.mul(lhs.dual, rhs.real, null)

            out.real.set(real)
            out.dual.set(q0.x + q1.x, q0.y + q1.y, q0.z + q1.z, q0.w + q1.w)
            return out
        }

        fun fromQuatAndTranslation(q: Quaternion, t: Vector3f): DualQuat {
            fun mulPure(v: Vector3f, q: Quaternion) = Quaternion(
                    v.x * q.w + v.y * q.z - v.z * q.y,
                    v.y * q.w + v.z * q.x - v.x * q.z,
                    v.z * q.w + v.x * q.y - v.y * q.x,
                    -v.x * q.x - v.y * q.y - v.z * q.z
            )

            val real = Quaternion(q)
            val dual = mulPure(t, real).scale(0.5f) as Quaternion
            return DualQuat(real, dual)
        }
    }
}