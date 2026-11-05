package com.vrgames.specimen.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpecimenTest {
    @Test
    fun everyLevelCanBeDoneWithoutAMistake() {
        for (s in Stages.ALL) {
            val p = Play(s)
            val b = Scholar(p)
            var n = 0
            while (!p.over && n < 20) { b.step(); n++ }
            assertTrue(s.id, p.over)
            assertEquals(s.id, 3, p.stars())
        }
    }

    @Test
    fun everySpecimenAppearsAndIsComplete() {
        val used = Stages.ALL.flatMap { it.items }.toSet()
        assertEquals("the cabinet can be filled", Cabinet.ALL.map { it.id }.toSet(), used)
        for (s in Cabinet.ALL) {
            assertEquals(s.id, 3, s.look.size)
            assertTrue("${s.id} narration ${s.narration.length}", s.narration.length in 70..180)
            assertTrue(s.id, s.wow.isNotEmpty() && s.clue.isNotEmpty() && s.latin.isNotEmpty())
        }
    }

    @Test
    fun aNameQuestionHasThreeChoicesOfTheSameKindAndStaysPut() {
        val p = Play(Stages.byId("a1"))
        val s = p.items[0]
        val a = p.names(s)
        assertEquals(3, a.size)
        assertTrue(s.name in a)
        assertEquals(a, p.names(s))
        val kinds = Cabinet.ALL.filter { it.name in a }.map { it.group }.toSet()
        assertEquals(setOf(s.group), kinds)
    }

    @Test
    fun aWrongGuessCostsAStarAndGivesAClueAndKeepsItInHand() {
        val p = Play(Stages.byId("a1"))
        val s = p.items[0]
        p.pick(s.id)
        p.answerName(p.names(s).first { it != s.name })
        assertEquals(1, p.mistakes)
        assertEquals(s, p.inHand)
        assertTrue(p.drain().any { it is Ev.Wrong && it.clue.isNotEmpty() })
        p.answerName(s.name)
        assertFalse(s.id in setOf<String>() )
        assertTrue(s.id in p.done)
        p.pick(p.items[1].id)
        p.answerName(p.items[1].name)
        assertEquals(2, p.stars())
    }

    @Test
    fun sortChecksTheDrawer() {
        val p = Play(Stages.byId("a3"))
        val m = Cabinet.of("morpho")
        p.pick(m.id)
        p.answerGroup(Group.BEETLE)
        assertEquals(1, p.mistakes)
        p.answerGroup(Group.WINGED)
        assertTrue(m.id in p.done)
    }

    @Test
    fun hardnessRunsSoftToHardAndTimelineOldToNew() {
        val h = Play(Stages.byId("c2"))
        assertEquals(listOf("fluorite", "pyrite", "quartz"), h.order.map { it.id })
        h.pick("quartz")
        h.placeNext()
        assertEquals("not first", 1, h.mistakes)
        val t = Play(Stages.byId("d2"))
        assertEquals(listOf("trilobite", "ammonite", "amber"), t.order.map { it.id })
    }

    @Test
    fun aHardnessOrderHasNoTies() {
        for (s in Stages.ALL.filter { it.mode == Mode.HARDNESS }) {
            val order = Play(s).order.map { it.measure as Measure.Hardness }
            // Ranges that overlap could be argued either way: each must lie clear below the next.
            for (i in 1 until order.size) assertTrue("${s.id} $order", order[i - 1].high < order[i].low)
        }
    }

    @Test
    fun pickingADoneOneDoesNothing() {
        val p = Play(Stages.byId("a1"))
        val s = p.items[0]
        p.pick(s.id)
        p.answerName(s.name)
        p.pick(s.id)
        assertEquals(null, p.inHand)
        assertNotNull(p.items[1])
    }
}
