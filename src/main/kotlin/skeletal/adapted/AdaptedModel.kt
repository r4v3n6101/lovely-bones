package skeletal.adapted

import org.lwjgl.util.vector.Matrix3f
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector3f
import skeletal.ModClass
import skeletal.animator.AnimationItem
import skeletal.animator.Animator
import skeletal.model.animated.AnimatedModel

class AdaptedModel(
        val animatedModel: AnimatedModel,
        val position: Vector3f = Vector3f(),
        val angles: Vector3f = Vector3f(), // Euler angles, in radians
        val scale: Vector3f = Vector3f(1f, 1f, 1f)
) {
    val animator = Animator(animatedModel.skeleton)
    val modelMatrix: Matrix4f = Matrix4f()
    val inverseTransposeMatrix: Matrix3f = Matrix3f()

    fun updateMatrices(renderPosX: Float, renderPosY: Float, renderPosZ: Float) {
        modelMatrix.setIdentity()
        modelMatrix
                .rotate(angles.x, Vector3f(1f, 0f, 0f))
                .rotate(angles.y, Vector3f(0f, 1f, 0f))
                .rotate(angles.z, Vector3f(0f, 0f, 1f))
        modelMatrix.scale(scale)
        modelMatrix.m30 = position.x - renderPosX
        modelMatrix.m31 = position.y - renderPosY
        modelMatrix.m32 = position.z - renderPosZ
        modelMatrix.m33 = 1f

        inverseTransposeMatrix.m00 = modelMatrix.m00
        inverseTransposeMatrix.m01 = modelMatrix.m10
        inverseTransposeMatrix.m02 = modelMatrix.m20
        inverseTransposeMatrix.m10 = modelMatrix.m01
        inverseTransposeMatrix.m11 = modelMatrix.m11
        inverseTransposeMatrix.m12 = modelMatrix.m21
        inverseTransposeMatrix.m20 = modelMatrix.m02
        inverseTransposeMatrix.m21 = modelMatrix.m12
        inverseTransposeMatrix.m22 = modelMatrix.m22

        inverseTransposeMatrix.invert()
    }

    fun setPosition(x: Float, y: Float, z: Float) {
        position.set(x, y, z)
    }

    fun setAngles(x: Float, y: Float, z: Float) {
        angles.set(x, y, z)
    }

    fun setScale(x: Float, y: Float, z: Float) {
        scale.set(x, y, z)
    }

    fun play(name: String, weight: Float) {
        val animation = animatedModel.animations[name]!!
        animator.animations[name] = AnimationItem(
                keyframes = animation.keyframes, // TODO : copyOf()
                framerate = animation.framerate,
                weight = weight
        )
    }

    fun pause(name: String) {
        animator.animations[name]!!.paused = true
    }

    fun resume(name: String) {
        animator.animations[name]!!.paused = false
    }

    fun stop(anim: String) {
        animator.animations.remove(anim)
    }

    fun stopAll() {
        animator.animations.clear()
    }

    fun isPlaying(name: String) = name in animator.animations

    /**
     * Add model to render stack
     */
    fun render(type: RenderType) {
        ModClass.modelsToRender.add(this to type)
    }
}