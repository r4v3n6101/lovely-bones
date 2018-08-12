package skeletal.model.animated

import java.nio.FloatBuffer
import kotlin.math.floor

class Animator {
    private var time: Float = 0f
    private var currentAnimation: Animation? = null
    private var frameTime: Float = 0f

    fun playAnimation(animation: Animation) {
        resetTime()
        currentAnimation = animation
        frameTime = 1f / animation.framerate
    }

    fun update(dt: Float) {
        time += dt
    }

    // TODO : Loop
    fun storeSkeletonData(buf: FloatBuffer) {
        val localAnim = currentAnimation
        if (localAnim != null) {
            val timestep = time * frameTime
            val keyframes = localAnim.keyframes

            val kf1 = floor(timestep).toInt()  // int part of time
            val kf2 = kf1 + 1
            val lerpStep = timestep - kf1 // fract part of time

            val keyframe1 = keyframes[kf1 % keyframes.size]
            val keyframe2 = keyframes[kf2 % keyframes.size]
            val skeletonSize = keyframe1.transforms.size

            repeat(skeletonSize) { index ->
                val dq0 = keyframe1.transforms[index]
                val dq1 = keyframe2.transforms[index]

                buf.put(dq0.q0.x + lerpStep * (dq1.q0.x - dq0.q0.x))
                buf.put(dq0.q0.y + lerpStep * (dq1.q0.y - dq0.q0.y))
                buf.put(dq0.q0.z + lerpStep * (dq1.q0.z - dq0.q0.z))
                buf.put(dq0.q0.w + lerpStep * (dq1.q0.w - dq0.q0.w))

                buf.put(dq0.q1.x + lerpStep * (dq1.q1.x - dq0.q1.x))
                buf.put(dq0.q1.y + lerpStep * (dq1.q1.y - dq0.q1.y))
                buf.put(dq0.q1.z + lerpStep * (dq1.q1.z - dq0.q1.z))
                buf.put(dq0.q1.w + lerpStep * (dq1.q1.w - dq0.q1.w))
            }
        }
    }

    fun resetTime() {
        time = 0f
    }
}