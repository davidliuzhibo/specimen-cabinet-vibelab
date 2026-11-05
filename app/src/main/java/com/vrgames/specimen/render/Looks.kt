package com.vrgames.specimen.render

/**
 * How each specimen sits, from its four-side render. Models are normalized to 1 m along their longest
 * side. [pitch] and [yaw] turn it to its best side on the tray, [lift] raises it (in model sizes).
 * The flat ones lie back to be seen from above, and [rock] rather than spin when in hand: edge-on they vanish.
 */
object Looks {
    class Pose(val pitch: Float, val yaw: Float, val lift: Float = 0f, val rock: Boolean = false, val handPitch: Float = 0f)

    private val POSES = mapOf(
        // Beetles: they stand on their legs. Their heads point along -z or +x in the models.
        "hercules" to Pose(0f, 150f),
        "stag" to Pose(0f, -120f),
        "scarab" to Pose(0f, -30f),
        "longhorn" to Pose(0f, -120f),
        // These come as flat as a pinned specimen, facing +z: laid back on the tray, upright in hand.
        "ladybug" to Pose(-70f, 0f, 0.05f, rock = true),
        "morpho" to Pose(-70f, 0f, 0.02f, rock = true),
        "swallow" to Pose(-70f, 0f, 0.02f, rock = true),
        // Wings closed, side-on along +x: it stands on its legs like the dead leaf it copies.
        "leafwing" to Pose(0f, -90f, 0f, rock = true),
        "atlas" to Pose(-70f, 0f, 0.02f, rock = true),
        "dragonfly" to Pose(-70f, 0f, 0.02f, rock = true),
        "mantis" to Pose(0f, 0f),
        // Stones and fossils showing a face.
        "amber" to Pose(-50f, 0f, 0.06f, rock = true),
        "malachite" to Pose(-55f, 0f, 0.06f, rock = true),
        "ammonite" to Pose(-55f, 0f, 0.06f, rock = true),
        "opal" to Pose(-15f, 0f),
    )

    fun pose(id: String) = POSES[id] ?: Pose(0f, 0f)

    /** What a material needs beyond the model's own maps. */
    enum class Finish { NONE, GOLD, GLASS }

    fun finish(id: String) = when (id) {
        "pyrite", "scarab" -> Finish.GOLD
        "quartz" -> Finish.GLASS
        else -> Finish.NONE
    }
}
