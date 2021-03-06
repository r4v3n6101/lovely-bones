package skeletal.model

import net.minecraft.client.renderer.texture.TextureUtil.missingTexture
import net.minecraftforge.client.model.IModelCustom
import org.lwjgl.opengl.GL11.*
import skeletal.graphics.Cleanable
import skeletal.graphics.VertexArrayObject

open class StaticModel(
        private val vao: VertexArrayObject,
        private val meshes: Map<String, Mesh>
) : IModelCustom, Cleanable by vao {

    override fun getType() = "StaticModel"

    override fun renderAll() {
        vao.use {
            meshes.forEach { (_, mesh) -> renderMesh(mesh) }
        }
    }

    /**
     * Don't use more than one time. If you need to draw some meshes, use renderOnly
     */
    override fun renderPart(partName: String) {
        vao.use {
            meshes[partName]?.let(::renderMesh)
        }
    }

    override fun renderOnly(vararg groupNames: String) {
        vao.use {
            groupNames.forEach { name -> meshes[name]?.let(::renderMesh) }
        }
    }

    override fun renderAllExcept(vararg excludedGroupNames: String) {
        val set = excludedGroupNames.toHashSet()
        vao.use {
            meshes.forEach { (name, mesh) -> if (name !in set) renderMesh(mesh) }
        }
    }

    private fun renderMesh(mesh: Mesh) {
        glBindTexture(GL_TEXTURE_2D, mesh.texture ?: missingTexture.glTextureId)
        glDrawElements(
                GL_TRIANGLES,
                mesh.indices,
                GL_UNSIGNED_INT,
                mesh.firstIndex * 4L // offset in bytes, every index is int
        )
    }
}