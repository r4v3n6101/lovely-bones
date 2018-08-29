package skeletal.math

import org.lwjgl.util.vector.*
import java.nio.FloatBuffer

/**
 * Represent dual quaternion real + dual * e, where's e is epsilon and e^2 == 0
 */
data class DualQuat(
        val real: Quaternion = Quaternion(),
        val dual: Quaternion = Quaternion(0f, 0f, 0f, 0f)
) : Vector(), ReadableVector {

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

    fun dot(dq: DualQuat) = Quaternion.dot(real, dq.real)

    /**
     * Load real & dual sequently, real is first
     */
    override fun load(buf: FloatBuffer): DualQuat {
        real.load(buf)
        dual.load(buf)
        return this
    }

    companion object {
        @JvmStatic
        fun mul(lhs: DualQuat, rhs: DualQuat, dest: DualQuat?): DualQuat {
            val out = dest ?: DualQuat()
            val r = Quaternion.mul(lhs.real, rhs.real, null)
            val q0 = Quaternion.mul(lhs.real, rhs.dual, null)
            val q1 = Quaternion.mul(lhs.dual, rhs.real, null)
            out.real.set(r)
            out.dual.set(q0.x + q1.x, q0.y + q1.y, q0.z + q1.z, q0.w + q1.w)
            return out
        }

        fun fromQuatAndTranslation(q: Quaternion, t: Vector3f): DualQuat {
            val real = Quaternion(q).normalise() as Quaternion
            val dual = mulPure(real, t).scale(0.5f) as Quaternion
            return DualQuat(real, dual)
        }

        fun fromMatrix(matrix: Matrix4f): DualQuat {
            val r = Quaternion()
            r.setFromMatrix(matrix)
            val t = Vector3f(matrix.m30, matrix.m31, matrix.m32)
            return fromQuatAndTranslation(r, t)
        }
    }
}