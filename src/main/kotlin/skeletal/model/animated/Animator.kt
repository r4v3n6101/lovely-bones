package skeletal.model.animated

import org.lwjgl.util.vector.Quaternion
import skeletal.MAX_BONES
import skeletal.math.DualQuat
import java.nio.FloatBuffer
import kotlin.math.floor

class Animator(val model: AnimatedModel) {
    private var time: Float = 0f
    val animations: HashMap<String, Float> = HashMap()

    fun update(dt: Float) {
        time += dt
        val mapIter = animations.iterator()
        /*while (mapIter.hasNext()) {
            val anim = model.animations[mapIter.next().key]!!
            if (!anim.loop && anim.keyframes.size + 1 < time.toInt())
                mapIter.remove() // TODO  : Reset time? Or make AnimationShot?
        }*/
    }

    fun resetTime() {
        time = 0f
    }

    fun storeSkeletonData(buf: FloatBuffer) {
        // 0f required to correct sum of dual quats. If no anims, then w = 1f
        val defaultW = if (animations.isEmpty()) 1f else 0f
        val tmpArray = Array(MAX_BONES) { DualQuat(Quaternion(0f, 0f, 0f, defaultW)) }
        animations.forEach { (name, weight) ->
            val anim = model.animations[name]!!
            val timestep = time / anim.framerate
            val keyframesSize = anim.keyframes.size

            val kf1 = floor(timestep).toInt()  // int part of time
            val kf2 = kf1 + 1
            val lerpStep = timestep - kf1 // fract part of time

            val keyframe1 = anim.keyframes[kf1 % keyframesSize]
            val keyframe2 = anim.keyframes[kf2 % keyframesSize]

            for (i in keyframe1.transforms.indices) {
                val dq0 = keyframe1.transforms[i]
                val dq1 = keyframe2.transforms[i]
                val dest = tmpArray[i]
                if (dq0.dot(dq1) >= 0)
                    lerpAddWeight(dq0, dq1, lerpStep, weight, dest)
                else
                    negatedLerpAddWeight(dq0, dq1, lerpStep, weight, dest)
            }
        }
        tmpArray.forEach { it.store(buf) }
    }

    private fun lerpAddWeight(dq0: DualQuat, dq1: DualQuat, k: Float, w: Float, dest: DualQuat) {
        dest.q0.x += (dq0.q0.x + k * (dq1.q0.x - dq0.q0.x)) * w
        dest.q0.y += (dq0.q0.y + k * (dq1.q0.y - dq0.q0.y)) * w
        dest.q0.z += (dq0.q0.z + k * (dq1.q0.z - dq0.q0.z)) * w
        dest.q0.w += (dq0.q0.w + k * (dq1.q0.w - dq0.q0.w)) * w
        dest.q1.x += (dq0.q1.x + k * (dq1.q1.x - dq0.q1.x)) * w
        dest.q1.y += (dq0.q1.y + k * (dq1.q1.y - dq0.q1.y)) * w
        dest.q1.z += (dq0.q1.z + k * (dq1.q1.z - dq0.q1.z)) * w
        dest.q1.w += (dq0.q1.w + k * (dq1.q1.w - dq0.q1.w)) * w
    }

    private fun negatedLerpAddWeight(dq0: DualQuat, dq1: DualQuat, k: Float, w: Float, dest: DualQuat) {
        dest.q0.x += (k * (dq1.q0.x + dq0.q0.x) - dq0.q0.x) * w
        dest.q0.y += (k * (dq1.q0.y + dq0.q0.y) - dq0.q0.y) * w
        dest.q0.z += (k * (dq1.q0.z + dq0.q0.z) - dq0.q0.z) * w
        dest.q0.w += (k * (dq1.q0.w + dq0.q0.w) - dq0.q0.w) * w
        dest.q1.x += (k * (dq1.q1.x + dq0.q1.x) - dq0.q1.x) * w
        dest.q1.y += (k * (dq1.q1.y + dq0.q1.y) - dq0.q1.y) * w
        dest.q1.z += (k * (dq1.q1.z + dq0.q1.z) - dq0.q1.z) * w
        dest.q1.w += (k * (dq1.q1.w + dq0.q1.w) - dq0.q1.w) * w
    }
}