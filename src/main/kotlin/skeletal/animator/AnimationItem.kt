package skeletal.animator

import skeletal.model.animated.Keyframe

class AnimationItem(
        var keyframes: Array<Keyframe>,
        var framerate: Float = 24f,
        var weight: Float = 1f,
        var elapsedFrames: Float = 0f,
        var loop: Boolean = false,
        var paused: Boolean = false
)