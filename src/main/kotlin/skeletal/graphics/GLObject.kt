package skeletal.graphics

interface GLObject : Cleanable {

    fun bind()

    fun unbind()

    fun use(action: () -> Unit) {
        bind()
        action()
        unbind()
    }
}