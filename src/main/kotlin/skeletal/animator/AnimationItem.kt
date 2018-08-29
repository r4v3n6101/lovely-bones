package skeletal.animator

import skeletal.model.animated.Animation

class AnimationItem(
        val animation: Animation,
        var weight: Float = 1f,
        var elapsedFrames: Float = 0f,
        var loop: Boolean = animation.loop,
        var paused: Boolean = false
)