package skeletal.model.animated

import skeletal.model.StaticModel

class AnimatedModel(
        val model: StaticModel, val skeleton: Map<String, Bone>, val animations: Map<String, Animation>
)