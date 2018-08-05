package skeletal.model.animated

import skeletal.math.DualQuat
import kotlin.math.truncate

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

    fun prepareShaderData(): Array<DualQuat> {
        val localCopyOfAnim = currentAnimation ?: error("Lol") // FIXME
        val timestep = time / frameTime
        val keyframes = localCopyOfAnim.keyframes
        val kf1 = (truncate(timestep).toInt()) % localCopyOfAnim.keyframes.size // int part of time
        val kf2 = (kf1 + 1) % localCopyOfAnim.keyframes.size // TODO : Loop
        val keyframe1 = keyframes[kf1]
        val keyframe2 = keyframes[kf2]
        val interpolationStep = timestep - kf1 // fract part of time

        return Array(keyframe1.transforms.size) { i ->
            keyframe1.transforms[i].lerp(keyframe2.transforms[i], interpolationStep, null)
        } // TODO : Cache or load directly to buffer
    }

    fun resetTime() {
        time = 0f
    }
}