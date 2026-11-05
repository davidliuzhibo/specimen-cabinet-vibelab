package com.vrgames.specimen.game

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vrgames.specimen.core.Ev
import com.vrgames.specimen.core.Group
import com.vrgames.specimen.core.Mode
import com.vrgames.specimen.core.Play
import com.vrgames.specimen.core.Stage
import com.vrgames.specimen.core.Stages
import com.vrgames.specimen.render.CabinetView
import com.vrgames.specimen.render.Music
import com.vrgames.specimen.render.Sfx
import com.vrgames.specimen.render.Sound

/** State the stage, its panels and the windows share. Stars and the cabinet's contents are saved. */
object Game {
    private const val PREFS = "specimen"
    private const val TAG = "SpecGame"

    enum class Phase { CARD, INTRO, PLAYING, RESULT, SHOW }

    val lock = Any()
    private var prefs: android.content.SharedPreferences? = null
    private var app: Context? = null
    val best = LinkedHashMap<String, Int>()
    /** What has been placed in the cabinet, across all levels. */
    val collected = LinkedHashSet<String>()
    var play: Play? = null; private set
    var playVersion by mutableIntStateOf(0); private set
    var stage by mutableStateOf(Stages.ALL[0]); private set
    var phase by mutableStateOf(Phase.CARD)
    var version by mutableIntStateOf(0); private set
    var note by mutableStateOf<String?>(null)
    var noteVersion by mutableIntStateOf(0); private set
    var window by mutableStateOf<String?>(null); private set
    private var windowCopy: Any? = null
    var closeWindow: (String) -> Unit = {}
    var menuOpen by mutableStateOf(false)
    var confirmRestart by mutableStateOf(false)
    var musicOn by mutableStateOf(true); private set
    var faceRequest by mutableIntStateOf(0)
    var view: CabinetView? = null
    var sound: Sound? = null
    var music: Music? = null
    var trophiesBefore = 0
    var timeScale = 1.0
    var debugView by mutableStateOf<String?>(null)
    var debugOpen by mutableStateOf<String?>(null)
    /** The id of what is in hand, for the view to load its full model. */
    var handId by mutableStateOf<String?>(null); private set
    var lastClue by mutableStateOf<String?>(null); private set
    var justPlaced by mutableStateOf<String?>(null); private set
    /** The clip now playing, or null. */
    var speaking by mutableStateOf<String?>(null); private set
    private var player: MediaPlayer? = null

    val stars get() = best.values.sum()
    val maxStars get() = Stages.ALL.size * 3
    val bronze get() = Stages.ALL.all { (best[it.id] ?: 0) > 0 }
    val silver get() = stars >= maxStars * 2 / 3
    val gold get() = stars >= maxStars
    val trophies get() = if (gold) 3 else if (silver) 2 else if (bronze) 1 else 0

    fun init(context: Context) {
        if (prefs != null) return
        app = context.applicationContext
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { p ->
            musicOn = p.getBoolean("music", true)
            p.getString("best", "")!!.split(",").filter { it.contains(":") }.forEach {
                val (k, v) = it.split(":")
                best[k] = v.toIntOrNull() ?: 0
            }
            collected += p.getString("cab", "")!!.split(",").filter { it.isNotEmpty() }
        }
        Log.i(TAG, "stars $stars/$maxStars cabinet ${collected.size}")
        AutoPilot.register(context)
    }

    fun say(text: String) {
        note = text
        noteVersion++
    }

    fun open(s: Stage): Boolean {
        val k = Stages.ALL.indexOf(s)
        return k == 0 || (best[Stages.ALL[k - 1].id] ?: 0) > 0 || (best[s.id] ?: 0) > 0
    }

    fun start() = intro(Stages.ALL.firstOrNull { (best[it.id] ?: 0) == 0 } ?: Stages.ALL.last())

    private fun fresh(s: Stage) {
        stopNarration()
        stage = s
        synchronized(lock) { play = Play(s, System.nanoTime() and 0xFFFF) }
        handId = null
        lastClue = null
        justPlaced = null
        playVersion++
    }

    fun intro(s: Stage) {
        if (!open(s)) {
            say("这一盒还没开放：先整理前面的。")
            return
        }
        fresh(s)
        phase = Phase.INTRO
        version++
    }

    fun begin() {
        fresh(stage)
        phase = Phase.PLAYING
        music?.resume()
        Log.i(TAG, "begin ${stage.id}")
        version++
    }

    fun retry() = begin()

    fun next() {
        val k = Stages.ALL.indexOf(stage)
        if (k + 1 < Stages.ALL.size) intro(Stages.ALL[k + 1]) else toCard()
    }

    fun toCard() {
        stopNarration()
        phase = Phase.CARD
        version++
    }

    fun pick(id: String) {
        val p = play ?: return
        if (phase != Phase.PLAYING) return
        stopNarration()
        lastClue = null
        p.pick(id)
        drain(p)
    }

    fun putBack() {
        val p = play ?: return
        stopNarration()
        p.putBack()
        drain(p)
    }

    fun answerName(n: String) {
        val p = play ?: return
        p.answerName(n)
        drain(p)
    }

    fun answerGroup(g: Group) {
        val p = play ?: return
        p.answerGroup(g)
        drain(p)
    }

    fun placeNext() {
        val p = play ?: return
        p.placeNext()
        drain(p)
    }

    /** After the test hook has moved the game on. */
    fun afterBot() {
        play?.let { drain(it) }
    }

    private fun drain(p: Play) {
        for (e in p.drain()) when (e) {
            is Ev.Picked -> { sound?.play(Sfx.PICK); justPlaced = null }
            is Ev.Right -> onRight(p, e.id)
            is Ev.Wrong -> {
                lastClue = "不对。" + e.clue
                sound?.play(if (p.stage.mode == Mode.HARDNESS) Sfx.SCRATCH else Sfx.WRONG)
            }
            is Ev.Note -> say(e.text)
            Ev.Won -> finish(p)
        }
        handId = p.inHand?.id
        version++
    }

    private fun onRight(p: Play, id: String) {
        collected += id
        justPlaced = id
        lastClue = null
        sound?.play(Sfx.RIGHT)
        sound?.play(Sfx.DRAWER)
        view?.placed(id)
        save()
        Log.i(TAG, "placed $id in ${p.stage.id}")
    }

    private fun finish(p: Play) {
        trophiesBefore = trophies
        val s = p.stars()
        if (s > (best[p.stage.id] ?: 0)) best[p.stage.id] = s
        save()
        phase = Phase.RESULT
        sound?.play(Sfx.FANFARE)
        Log.i(TAG, "finish ${p.stage.id} stars=$s mistakes=${p.mistakes} cabinet=${collected.size}")
    }

    /** Plays the narrator's clip for a specimen, with the music turned down. */
    fun narrate(id: String) {
        stopNarration()
        val c = app ?: return
        runCatching {
            val fd = c.assets.openFd("narration/$id.ogg")
            player = MediaPlayer().apply {
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                fd.close()
                setOnCompletionListener { stopNarration() }
                prepare()
                start()
            }
            speaking = id
            music?.duck(true)
            Log.i(TAG, "narrate $id")
        }.onFailure { Log.w(TAG, "narrate $id: ${it.message}") }
        version++
    }

    fun stopNarration() {
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
        if (speaking != null) {
            speaking = null
            music?.duck(false)
            version++
        }
    }

    fun tick(dt0: Double) {
        val dt = dt0 * timeScale
        if (phase == Phase.SHOW) view?.show(dt)
    }

    fun show() {
        if (!gold && !AutoPilot.debug) return
        stopNarration()
        phase = Phase.SHOW
        sound?.play(Sfx.FANFARE)
        version++
    }

    fun endShow() = toCard()

    private fun save() {
        prefs?.edit()?.putString("best", best.entries.joinToString(",") { "${it.key}:${it.value}" })?.putString("cab", collected.joinToString(","))?.apply()
    }

    fun restart() {
        best.clear()
        collected.clear()
        save()
        fresh(Stages.ALL[0])
        phase = Phase.CARD
        version++
    }

    fun toggleMusic() {
        musicOn = !musicOn
        music?.muted = !musicOn
        prefs?.edit()?.putBoolean("music", musicOn)?.apply()
    }

    fun grant(n: Int) {
        for (s in Stages.ALL) best[s.id] = n
        save()
        version++
    }

    fun windowShown(id: String, copy: Any? = null) {
        window = id
        if (copy != null) windowCopy = copy
        version++
    }

    fun windowGone(id: String, copy: Any? = null) {
        if (copy != null && copy !== windowCopy) return
        if (window == id) window = null
        version++
    }
}
