package com.vrgames.specimen.render

import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.EnvironmentLightingSettingsComponent
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.ImageBasedLightComponent
import com.pico.spatial.core.ecs.ImageBasedLightReceiverComponent
import com.pico.spatial.core.ecs.ImageBasedLightSource
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.Material
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.math.EulerAngles
import com.pico.spatial.core.math.Vector3
import com.vrgames.specimen.core.Cabinet
import com.vrgames.specimen.core.Group
import com.vrgames.specimen.core.Play
import com.vrgames.specimen.core.Vec3
import kotlin.math.cos
import kotlin.math.sin

/**
 * The naturalist's desk with the box of the level on it, the cabinet behind, each drawer a glass shelf
 * for its kind. What is in hand floats above the desk in its full model, loaded when it is picked;
 * everything else uses the small model. In the room's frame, turned to the player's heading.
 */
class CabinetView(private val models: Map<String, Entity>, private val tex: Map<String, TextureResource>) {
    val root = Entity()
    val desk = Entity()
    private var tray = Entity().also { desk.addChild(it) }
    private val shelves = Entity().also { root.addChild(it) }
    private val iblSource = Entity().also { root.addChild(it) }
    private val owner = HashMap<Entity, String>()
    private val trayViews = HashMap<String, Entity>()
    private val shelfViews = HashMap<String, Entity>()
    private val sphere = MeshResource.createSphere(1f).shared()
    private val wood = (tex["wood"]?.let { t ->
        PhysicallyBasedMaterial.create().apply {
            setBaseColorTexture(t)
            setEmissiveTexture(t)
            setEmissiveColor(color(0.22f, 0.22f, 0.22f))
            setRoughness(0.5f)
        }.shared()
    } ?: pbr(color(0.26f, 0.15f, 0.08f), roughness = 0.5f).shared())
    private val felt = pbr(color(0.1f, 0.2f, 0.14f), roughness = 1f).shared()
    private val brass = pbr(color(0.8f, 0.6f, 0.28f), metallic = 1f, roughness = 0.3f).shared()
    private val glass = pbr(color(0.8f, 0.9f, 0.95f, 0.1f), roughness = 0.05f, emissive = color(0f, 0f, 0f), blending = BlendingMode.TRANSPARENT).shared()
    private val halo = glow(color(1f, 0.85f, 0.55f, 0.08f)).shared()
    // Too faint to see until hover brightens it; a see-through layer would darken what is behind it.
    private val hoverPad = glow(color(1f, 0.95f, 0.8f, 0.006f)).shared()
    private val sparkle = Sparks("spec_sparkle", 120, sphere, glow(color(1f, 0.85f, 0.5f, 1f)).shared()).also { root.addChild(it.entity) }
    private val fireflies = Sparks("spec_flies", 80, sphere, glow(color(0.7f, 1f, 0.5f, 1f)).shared()).also { root.addChild(it.entity) }

    /** The one in hand: a holder that turns, with the small model at once and the full one when it has come. */
    private val hand = Entity().also { root.addChild(it) }
    private val handLight = model(sphere, halo).apply { setScale(0.26f); enabled = false }.also { root.addChild(it) }
    private var handBody: Entity? = null
    private var handId: String? = null
    private var handClock = 0.0
    private val fullCache = LinkedHashMap<String, Entity>()
    private var played: Play? = null
    private var clock = 0.0
    private val flyers = ArrayList<Entity>()
    private var showClock = 0.0
    private val trophyViews = ArrayList<Entity>()
    private val trophyMats = listOf(
        pbr(color(0.72f, 0.45f, 0.22f), metallic = 1f, roughness = 0.3f), pbr(color(0.8f, 0.82f, 0.86f), metallic = 1f, roughness = 0.22f), pbr(color(1f, 0.78f, 0.3f), metallic = 1f, roughness = 0.2f),
    ).map { it.shared() }
    private val trophyOff = pbr(color(0.08f, 0.08f, 0.1f), roughness = 0.8f).shared()

    init {
        root.addChild(desk)
        furniture()
    }

    private fun clone(name: String): Entity? = models[name]?.clone(Entity.CloneOptions(true, true))

    /**
     * Metals want the room's reflection and a warmer tint than the pale gold of their maps; rock crystal
     * can't come out of a generated model as glass at all, so it wears a see-through material.
     */
    private fun finish(e: Entity, id: String) {
        when (Looks.finish(id)) {
            Looks.Finish.NONE -> {}
            // A see-through material made the whole crystal vanish on the emulator: glossy over the model's own maps instead.
            Looks.Finish.GLASS -> gloss(e)
            Looks.Finish.GOLD -> tint(e)
        }
    }

    private fun tint(e: Entity) {
        e.components[ModelComponent::class.java]?.materials?.let { mats ->
            for (i in 0 until mats.size) (mats[i] as? PhysicallyBasedMaterial)?.apply {
                setBaseColor(color(1f, 0.78f, 0.42f))
                setMetallic(1f)
                setRoughness(0.28f)
            }
        }
        e.getChildren().forEach { tint(it) }
    }

    private fun gloss(e: Entity) {
        e.components[ModelComponent::class.java]?.materials?.let { mats ->
            for (i in 0 until mats.size) (mats[i] as? PhysicallyBasedMaterial)?.apply {
                setMetallic(0f)
                setRoughness(0.12f)
            }
        }
        e.getChildren().forEach { gloss(it) }
    }

    private fun setMaterials(e: Entity, m: Material) {
        e.components[ModelComponent::class.java]?.materials?.let { mats -> for (i in 0 until mats.size) mats[i] = m }
        e.getChildren().forEach { setMaterials(it, m) }
    }

    /** Desk top and legs, the felt-lined tray; the cabinet with four glass-fronted rows; the trophies. */
    private fun furniture() {
        desk.addChild(model(MeshResource.createBox(Vector3(1.3f, 0.05f, 0.62f), 0.008f), wood).apply { setPos(Vector3(0f, DESK_Y - 0.025f, -0.72f)) })
        for (sx in listOf(-1f, 1f)) for (sz in listOf(-1f, 1f)) desk.addChild(model(MeshResource.createBox(Vector3(0.06f, DESK_Y - 0.05f, 0.06f), 0.008f), wood).apply {
            setPos(Vector3(sx * 0.58f, (DESK_Y - 0.05f) / 2, -0.72f + sz * 0.24f))
        })
        desk.addChild(model(MeshResource.createBox(Vector3(0.98f, 0.012f, 0.26f), 0.004f), felt).apply { setPos(Vector3(0f, DESK_Y + 0.006f, TRAY_Z)) })
        for (s in listOf(-1f, 1f)) {
            desk.addChild(model(MeshResource.createBox(Vector3(0.02f, 0.03f, 0.28f), 0.003f), brass).apply { setPos(Vector3(s * 0.5f, DESK_Y + 0.015f, TRAY_Z)) })
            desk.addChild(model(MeshResource.createBox(Vector3(1.02f, 0.03f, 0.02f), 0.003f), brass).apply { setPos(Vector3(0f, DESK_Y + 0.015f, TRAY_Z + s * 0.14f)) })
        }
        // The cabinet.
        val w = CAB_W
        val h = CAB_ROW * Group.entries.size + 0.06f
        root.addChild(model(MeshResource.createBox(Vector3(w + 0.08f, h + 0.08f, 0.02f), 0.004f), wood).apply { setPos(Vector3(0f, CAB_Y + h / 2, CAB_Z - 0.11f)) })
        for (s in listOf(-1f, 1f)) root.addChild(model(MeshResource.createBox(Vector3(0.04f, h + 0.08f, 0.22f), 0.004f), wood).apply { setPos(Vector3(s * (w / 2 + 0.02f), CAB_Y + h / 2, CAB_Z)) })
        for (k in 0..Group.entries.size) root.addChild(model(MeshResource.createBox(Vector3(w, 0.022f, 0.22f), 0.003f), wood).apply { setPos(Vector3(0f, CAB_Y + k * CAB_ROW + 0.01f, CAB_Z)) })
        root.addChild(model(MeshResource.createBox(Vector3(w, h, 0.002f), 0f), glass).apply { setPos(Vector3(0f, CAB_Y + h / 2, CAB_Z + 0.112f)) })
        // Trophies on the desk's right corner.
        for (k in 0 until 3) {
            val t = clone("trophy") ?: model(MeshResource.createCone(0.1f, 0.03f), brass)
            t.setScale(0.09f)
            t.setPos(Vector3(0.42f + k * 0.08f, DESK_Y, -0.93f))
            t.transform().eulerAngles = EulerAngles(0f, -90f, 0f)
            desk.addChild(t)
            trophyViews += t
        }
        trophies(0)
    }

    fun trophies(earned: Int) = trophyViews.forEachIndexed { k, t -> setMaterials(t, if (k < earned) trophyMats[k] else trophyOff) }

    fun specimenOf(e: Entity?): String? {
        var cur = e
        while (cur != null) {
            owner[cur]?.let { return it }
            cur = cur.getParent()
        }
        return null
    }

    /** Lays out the level's box on the tray. */
    fun build(p: Play) {
        played = p
        tray.destroy()
        tray = Entity().also { desk.addChild(it) }
        trayViews.clear()
        owner.clear()
        // Nothing in hand any more.
        handId = null
        handBody?.let { if (it !in fullCache.values) it.destroy() else it.enabled = false }
        handBody = null
        handLight.enabled = false
        val n = p.items.size
        val step = (0.9f / n).coerceAtMost(0.2f)
        p.items.forEachIndexed { k, s ->
            val x = (k - (n - 1) / 2f) * step
            val holder = Entity()
            holder.setPos(Vector3(x, DESK_Y + 0.012f, TRAY_Z))
            tray.addChild(holder)
            clone("small_${s.id}")?.let { m ->
                val pose = Looks.pose(s.id)
                m.setScale(TRAY_SIZE)
                m.setPos(Vector3(0f, pose.lift * TRAY_SIZE, 0f))
                m.transform().eulerAngles = EulerAngles(pose.pitch, pose.yaw, 0f)
                finish(m, s.id)
                holder.addChild(m)
            }
            // A pad a little wider than the thing, floor to top: what the pinch lands on.
            val pad = model(sphere, hoverPad)
            val r = (step * 0.46f).coerceAtMost(0.075f)
            pad.setScale(r)
            pad.setPos(Vector3(0f, r * 0.8f, 0f))
            pad.components.set(CollisionComponent(listOf(ShapeResource.createSphere(1f)), PhysicsMaterialResource()))
            pad.components.set(InteractableComponent())
            pad.components.set(HoverEffectComponent())
            holder.addChild(pad)
            owner[pad] = s.id
            trayViews[s.id] = holder
            holder.enabled = s.id !in p.done
        }
        receive(tray)
    }

    /** Fills the shelves with what has been collected. */
    fun fill(have: Set<String>) {
        for (id in have) if (id !in shelfViews) shelve(id)
        // After clearing the progress.
        for (id in shelfViews.keys.toList()) if (id !in have) {
            shelfViews[id]?.destroy()
            shelfViews.remove(id)
        }
    }

    private fun shelve(id: String) {
        val s = Cabinet.of(id)
        val e = clone("small_$id") ?: return
        val row = Group.entries.size - 1 - s.group.ordinal
        val inGroup = Cabinet.ALL.filter { it.group == s.group }
        val k = inGroup.indexOf(s)
        val pose = Looks.pose(id)
        val size = 0.1f
        e.setScale(size)
        e.setPos(Vector3((k - (inGroup.size - 1) / 2f) * (CAB_W - 0.06f) / inGroup.size.coerceAtLeast(6), CAB_Y + row * CAB_ROW + 0.022f + pose.lift * size, CAB_Z))
        e.transform().eulerAngles = EulerAngles(pose.pitch, pose.yaw, 0f)
        finish(e, id)
        shelves.addChild(e)
        receive(e)
        shelfViews[id] = e
    }

    /** Takes a specimen up in hand: the small model at once, then the full one (see [hold]'s suspension). */
    suspend fun hold(id: String?) {
        if (id == handId) return
        handId = id
        handBody?.let { if (it !in fullCache.values) it.destroy() else it.enabled = false }
        handBody = null
        handLight.enabled = id != null
        played?.items?.forEach { s -> trayViews[s.id]?.enabled = s.id !in played!!.done && s.id != id }
        if (id == null) return
        place(clone("small_$id"), id)
        val full = fullCache[id] ?: runCatching { Entity.loadSuspend("asset://models/$id.glb") }.getOrNull()?.also { f ->
            fullCache[id] = f
            // Only a few of the big ones stay loaded.
            if (fullCache.size > 3) fullCache.remove(fullCache.keys.first())?.destroy()
        }
        if (full != null && handId == id) {
            handBody?.destroy()
            handBody = null
            place(full, id)
            receive(full)
        }
    }

    private fun place(e: Entity?, id: String) {
        val m = e ?: return
        val pose = Looks.pose(id)
        m.enabled = true
        m.setScale(HAND_SIZE)
        m.setPos(Vector3(0f, -HAND_SIZE * 0.35f, 0f))
        m.transform().eulerAngles = EulerAngles(pose.handPitch, pose.yaw, 0f)
        finish(m, id)
        if (m.getParent() !== hand) hand.addChild(m)
        handBody = m
    }

    fun placed(id: String) {
        trayViews[id]?.enabled = false
        sparkle.emit(Vec3(0.0, HAND_Y.toDouble(), HAND_Z.toDouble()), 40, 0.25, 0.9, 0.004f)
    }

    fun sync(p: Play, dt: Double) {
        if (p !== played) return
        clock += dt
        handClock += dt
        hand.setPos(Vector3(0f, HAND_Y + 0.008f * sin(clock * 1.1).toFloat(), HAND_Z))
        // A flat one rocks gently; anything else turns all the way round.
        val rock = handId?.let { Looks.pose(it).rock } ?: false
        hand.transform().eulerAngles = EulerAngles(0f, if (rock) (32 * sin(handClock * 0.8)).toFloat() else (handClock * 28).toFloat(), 0f)
        handLight.setPos(Vector3(0f, HAND_Y, HAND_Z))
        sparkle.update(dt, 0.8)
        fireflies.update(dt, 0.2)
    }

    /** The night the cabinet is opened: butterflies off the shelf, fireflies in the room. */
    fun show(dt: Double) {
        showClock += dt
        if (flyers.isEmpty()) for (id in listOf("morpho", "swallow", "atlas", "leafwing", "dragonfly")) clone("small_$id")?.let { e ->
            e.setScale(0.1f)
            root.addChild(e)
            receive(e)
            flyers += e
        }
        flyers.forEachIndexed { i, e ->
            val a = showClock * 0.5 + i * 1.3
            e.setPos(Vector3((cos(a) * 0.48).toFloat(), (1.22 + 0.13 * sin(a * 1.7)).toFloat(), (-0.76 + sin(a) * 0.24).toFloat()))
            e.transform().eulerAngles = EulerAngles((12 * sin(showClock * 7 + i)).toFloat(), (-Math.toDegrees(a)).toFloat(), 0f)
        }
        if ((showClock * 3).toInt() != ((showClock - dt) * 3).toInt()) fireflies.emit(Vec3(sin(showClock * 3) * 0.7, 0.95 + cos(showClock * 2) * 0.3, -0.9 + sin(showClock) * 0.2), 2, 0.02, 3.0, 0.004f)
        fireflies.update(dt, 0.1)
    }

    fun useIbl(t: TextureResource) {
        iblSource.components.set(ImageBasedLightComponent(ImageBasedLightSource.Single(t), 7f))
        receive(root)
    }

    private fun receive(e: Entity) {
        if (iblSource.components[ImageBasedLightComponent::class.java] == null) return
        if (e.components[ModelComponent::class.java] != null && e.components[ImageBasedLightReceiverComponent::class.java] == null) {
            e.components.set(ImageBasedLightReceiverComponent(iblSource))
            e.components.set(EnvironmentLightingSettingsComponent(0f))
        }
        e.getChildren().forEach { receive(it) }
    }

    fun receiveAll() = receive(root)

    companion object {
        const val DESK_Y = 0.78f
        const val TRAY_Z = -0.62f
        const val TRAY_SIZE = 0.13f
        const val HAND_SIZE = 0.3f
        const val HAND_Y = 1.14f
        const val HAND_Z = -0.66f
        const val CAB_W = 1.2f
        const val CAB_Y = 0.92f
        const val CAB_ROW = 0.17f
        const val CAB_Z = -1.34f
    }
}
