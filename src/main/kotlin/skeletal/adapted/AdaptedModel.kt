package skeletal.adapted

import org.lwjgl.util.vector.Matrix3f
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector3f
import skeletal.ModClass
import skeletal.model.animated.AnimatedModel
import skeletal.model.animated.Animator

class AdaptedModel(
        val animatedModel: AnimatedModel,
        val position: Vector3f = Vector3f(),
        val angles: Vector3f = Vector3f(), // Euler angles
        val scale: Vector3f = Vector3f(1f, 1f, 1f)
) {
    val animator = Animator(animatedModel)
    val modelMatrix: Matrix4f = Matrix4f()
    val inverseTransposeMatrix: Matrix3f = Matrix3f()

    fun updateMatrices(renderPosX: Float, renderPosY: Float, renderPosZ: Float) {
        val tmp = Vector3f(renderPosX, renderPosY, renderPosZ)
        val trans = Matrix4f().translate(Vector3f.sub(position, tmp, tmp))
        val scale = Matrix4f().scale(scale)
        val rot = Matrix4f()
                .rotate(angles.x, Vector3f(1f, 0f, 0f))
                .rotate(angles.y, Vector3f(0f, 1f, 0f))
                .rotate(angles.z, Vector3f(0f, 0f, 1f))
        Matrix4f.mul(trans, Matrix4f.mul(rot, scale, modelMatrix), modelMatrix)

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

    fun resetTime() { // TODO
        animator.resetTime()
    }

    /**
     * Add model to render stack
     */
    fun render(type: RenderType) {
        ModClass.modelsToRender.add(this to type)
    }
}