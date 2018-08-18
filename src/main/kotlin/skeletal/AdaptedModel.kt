package skeletal

import net.minecraftforge.client.model.IModelCustom
import org.lwjgl.util.vector.Matrix3f
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector3f
import skeletal.model.animated.AnimatedModel
import skeletal.model.animated.Animator

class AdaptedModel(
        private val animatedModel: AnimatedModel,
        val position: Vector3f,
        val angles: Vector3f, // Euler angles
        val scale: Vector3f
) : IModelCustom by animatedModel {
    private val animator = Animator(animatedModel)
    private var modelMatrix: Matrix4f = Matrix4f()
    private var inverseTransposeMatrix: Matrix3f = Matrix3f()

    init {
        updateMatrices() // First update
    }

    private fun updateMatrices() {
        modelMatrix.scale(scale)
        modelMatrix.rotate(angles.x, Vector3f(1f, 0f, 0f))
        modelMatrix.rotate(angles.y, Vector3f(0f, 1f, 0f))
        modelMatrix.rotate(angles.z, Vector3f(0f, 0f, 1f))
        modelMatrix.translate(position)

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
        updateMatrices()
    }

    fun setAngles(x: Float, y: Float, z: Float) {
        angles.set(x, y, z)
        updateMatrices()
    }

    fun setScale(x: Float, y: Float, z: Float) {
        scale.set(x, y, z)
        updateMatrices()
    }

    /**
     * Use instead setPosition & setRotation & setScale code to reduce matrices refresh
     */
    fun setTransform(pX: Float, pY: Float, pZ: Float, rX: Float, rY: Float, rZ: Float, sX: Float, sY: Float, sZ: Float) {
        position.set(pX, pY, pZ)
        angles.set(rX, rY, rZ)
        scale.set(sX, sY, sZ)
        updateMatrices()
    }

    fun play(vararg anims: Pair<String, Float>) {
        animator.animations += anims
    }

    fun play(vararg anims: String) {
        val averagedWeight = 1f / anims.size
        anims.forEach { play(it, averagedWeight) }
    }

    fun play(name: String, weight: Float) {
        animator.animations[name] = weight
    }

    fun stop(vararg anims: String) {
        anims.forEach { animator.animations.remove(it) }
    }

    fun stopAll() {
        animator.animations.clear()
    }

    fun resetTime() {
        animator.resetTime()
    }

    // TODO : World position is difference between render pos and etc
}