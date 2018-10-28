package skeletal.animator

import org.lwjgl.util.vector.Quaternion
import skeletal.math.DualQuat
import java.nio.FloatBuffer
import kotlin.math.floor

class Animator(private val bones: Int) {

    val animations = HashMap<String, AnimationItem>()

    fun update(dt: Float) {
        val iter = animations.iterator()
        while (iter.hasNext()) {
            val shot = iter.next().value
            if (!shot.paused) shot.elapsedFrames += dt * shot.animation.framerate
            if (!shot.loop && shot.elapsedFrames + 1f > shot.animation.keyframes.size.toFloat())
                iter.remove()
        }
    }

    fun storeSkeletonData(buf: FloatBuffer) {
        // 0f required to correct sum of dual quats. If no anims, then w = 1f
        val defaultW = if (animations.isEmpty()) 1f else 0f
        val transforms = Array(bones) { DualQuat(Quaternion(0f, 0f, 0f, defaultW)) } // TODO : Rewrite
        animations.forEach { (_, shot) ->
            val anim = shot.animation
            val weight = shot.weight
            val elapsed = shot.elapsedFrames

            val kf1 = floor(elapsed).toInt()
            val kf2 = kf1 + 1
            val lerpStep = elapsed - kf1 // fract part of time

            val keyframe1 = anim.keyframes[kf1 % anim.keyframes.size]
            val keyframe2 = anim.keyframes[kf2 % anim.keyframes.size]

            for (i in 0 until bones) {
                val dq0 = keyframe1.transforms[i]
                val dq1 = keyframe2.transforms[i]
                val dest = transforms[i]
                if (DualQuat.dot(dq0, dq1) >= 0)
                    lerpAddWeight(dq0, dq1, lerpStep, weight, dest)
                else
                    negatedLerpAddWeight(dq0, dq1, lerpStep, weight, dest)
                if (dest.lengthSquared() > 0f) dest.normalise()
            }
        }
        transforms.forEach { it.store(buf) }
    }

    private fun lerpAddWeight(dq0: DualQuat, dq1: DualQuat, k: Float, w: Float, dest: DualQuat) {
        dest.real.x += (dq0.real.x + k * (dq1.real.x - dq0.real.x)) * w
        dest.real.y += (dq0.real.y + k * (dq1.real.y - dq0.real.y)) * w
        dest.real.z += (dq0.real.z + k * (dq1.real.z - dq0.real.z)) * w
        dest.real.w += (dq0.real.w + k * (dq1.real.w - dq0.real.w)) * w
        dest.dual.x += (dq0.dual.x + k * (dq1.dual.x - dq0.dual.x)) * w
        dest.dual.y += (dq0.dual.y + k * (dq1.dual.y - dq0.dual.y)) * w
        dest.dual.z += (dq0.dual.z + k * (dq1.dual.z - dq0.dual.z)) * w
        dest.dual.w += (dq0.dual.w + k * (dq1.dual.w - dq0.dual.w)) * w
    }

    private fun negatedLerpAddWeight(dq0: DualQuat, dq1: DualQuat, k: Float, w: Float, dest: DualQuat) {
        dest.real.x += (k * (dq1.real.x + dq0.real.x) - dq0.real.x) * w
        dest.real.y += (k * (dq1.real.y + dq0.real.y) - dq0.real.y) * w
        dest.real.z += (k * (dq1.real.z + dq0.real.z) - dq0.real.z) * w
        dest.real.w += (k * (dq1.real.w + dq0.real.w) - dq0.real.w) * w
        dest.dual.x += (k * (dq1.dual.x + dq0.dual.x) - dq0.dual.x) * w
        dest.dual.y += (k * (dq1.dual.y + dq0.dual.y) - dq0.dual.y) * w
        dest.dual.z += (k * (dq1.dual.z + dq0.dual.z) - dq0.dual.z) * w
        dest.dual.w += (k * (dq1.dual.w + dq0.dual.w) - dq0.dual.w) * w
    }
}