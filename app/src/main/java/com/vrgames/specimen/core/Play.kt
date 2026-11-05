package com.vrgames.specimen.core

sealed interface Ev {
    data class Picked(val id: String) : Ev
    data class Right(val id: String) : Ev
    data class Wrong(val id: String, val clue: String) : Ev
    data class Note(val text: String) : Ev
    data object Won : Ev
}

/** One level: which specimen is in hand, what has been placed, how many wrong guesses. */
class Play(val stage: Stage, seed: Long = 1L) {
    private val rnd = java.util.Random(seed + stage.id.hashCode())
    val items = stage.items.map { Cabinet.of(it) }
    val done = LinkedHashSet<String>()
    var inHand: Specimen? = null; private set
    var mistakes = 0; private set
    var over = false; private set
    private val choices = HashMap<String, List<String>>()
    private val events = ArrayList<Ev>()

    val ordered get() = stage.mode == Mode.HARDNESS || stage.mode == Mode.TIMELINE

    /** The right order, for the modes that have one. */
    val order: List<Specimen>
        get() = when (stage.mode) {
            Mode.HARDNESS -> items.sortedBy { (it.measure as Measure.Hardness).let { h -> (h.low + h.high) / 2 } }
            Mode.TIMELINE -> items.sortedByDescending { (it.measure as Measure.Age).from }
            else -> items
        }

    /** Three names for the one in hand: the true one and two of its own kind, in a shuffled order that stays the same. */
    fun names(s: Specimen): List<String> = choices.getOrPut(s.id) {
        val others = Cabinet.ALL.filter { it !== s && it.group == s.group }.shuffled(rnd).take(2).map { it.name }
        (others + s.name).shuffled(rnd)
    }

    /** A pinch on a specimen on the tray. */
    fun pick(id: String) {
        if (over || id in done) return
        val s = items.firstOrNull { it.id == id } ?: return
        inHand = s
        events += Ev.Picked(id)
    }

    /** Puts the one in hand back on the tray, unjudged. */
    fun putBack() {
        inHand = null
    }

    /** In the order modes: the one in hand is named as next. */
    fun placeNext() {
        val s = inHand ?: return
        if (over || !ordered) return
        val next = order.first { it.id !in done }
        if (next === s) right(s) else wrong(s, orderClue(s, next))
    }

    private fun orderClue(s: Specimen, next: Specimen) = when (stage.mode) {
        Mode.HARDNESS -> "${s.name}比${next.name}硬，${next.name}更软，应该排在前面。"
        else -> "${next.name}出现得比${s.name}更早。"
    }

    fun answerName(name: String) {
        val s = inHand ?: return
        if (over || stage.mode != Mode.NAME) return
        if (name == s.name) right(s) else wrong(s, s.clue)
    }

    fun answerGroup(g: Group) {
        val s = inHand ?: return
        if (over || stage.mode != Mode.SORT) return
        if (g == s.group) right(s) else wrong(s, g.title + "：" + g.hint)
    }

    private fun right(s: Specimen) {
        done += s.id
        inHand = null
        events += Ev.Right(s.id)
        if (done.size == items.size) {
            over = true
            events += Ev.Won
        }
    }

    private fun wrong(s: Specimen, clue: String) {
        mistakes++
        events += Ev.Wrong(s.id, clue)
    }

    fun drain(): List<Ev> = events.toList().also { events.clear() }

    /** 3 stars with no wrong guess, 2 with up to two. */
    fun stars() = if (!over) 0 else if (mistakes == 0) 3 else if (mistakes <= 2) 2 else 1
}

/** Knows the answers: for the tests and the emulator walk. */
class Scholar(private val p: Play) {
    fun step() {
        if (p.over) return
        val next = if (p.ordered) p.order.first { it.id !in p.done } else p.items.first { it.id !in p.done }
        p.pick(next.id)
        when (p.stage.mode) {
            Mode.NAME -> p.answerName(next.name)
            Mode.SORT -> p.answerGroup(next.group)
            else -> p.placeNext()
        }
    }
}
