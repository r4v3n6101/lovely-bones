package skeletal.adapted

sealed class RenderType {
    object All : RenderType()
    class Part(val name: String) : RenderType()
    class Only(val names: Array<String>) : RenderType()
    class Except(val names: Array<String>) : RenderType()
}