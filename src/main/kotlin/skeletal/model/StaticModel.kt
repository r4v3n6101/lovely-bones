package skeletal.model

import net.minecraft.util.AxisAlignedBB
import org.lwjgl.opengl.GL11.*
import skeletal.graphics.VertexArrayObject

//TODO : Should I create general Model?
class StaticModel(val vao: VertexArrayObject, val meshes: Map<String, Mesh>, val bound: AxisAlignedBB? = null) {

    private fun renderMesh(mesh: Mesh) {
        //glBindTexture(mesh.material) FIXME TODO create texture
        vao.bind()
        glDrawElements(
                GL_TRIANGLES,
                mesh.indices,
                GL_UNSIGNED_INT,
                mesh.firstIndex * 4L // offset in bytes, every index is int
        )
        vao.unbind()
    }

    fun renderAll() {
        meshes.forEach { (_, mesh) -> renderMesh(mesh) }
    }

    fun renderPart(vararg names: String) {
        names.forEach { name -> meshes[name]?.let { renderMesh(it) } }
    }

    fun renderExcept(vararg names: String) {
        val set = names.toHashSet() // O(1) for check containment
        meshes.forEach { (name, mesh) -> if (name !in set) renderMesh(mesh) }
    }
}