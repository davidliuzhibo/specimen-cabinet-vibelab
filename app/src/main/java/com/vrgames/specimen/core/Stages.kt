package com.vrgames.specimen.core

/** How a level's specimens are worked through. */
enum class Mode(val title: String) {
    /** Each one: name it, from three names. */
    NAME("鉴定"),
    /** Each one: which drawer does it go in. */
    SORT("归类"),
    /** Softest first: which scratches which. */
    HARDNESS("硬度排序"),
    /** Oldest first. */
    TIMELINE("年代排序"),
}

class Stage(val id: String, val act: Int, val title: String, val ask: String, val mode: Mode, val items: List<String>)

object Stages {
    val ACTS = listOf("第一屉 · 甲虫", "第二屉 · 有翅昆虫", "第三屉 · 矿物", "第四屉 · 化石")

    val ALL = listOf(
        Stage("a1", 0, "第一盒标本", "捏托盘上的标本，它会到你面前转起来。看清楚，再选它的名字。", Mode.NAME, listOf("ladybug", "hercules")),
        Stage("a2", 0, "带角的和带钳的", "三只甲虫，看角，看大颚，看身体。", Mode.NAME, listOf("stag", "longhorn", "scarab")),
        Stage("a3", 0, "都是甲虫吗", "这盒混进了别的。翅膀像硬壳的才放进甲虫屉。", Mode.SORT, listOf("hercules", "morpho", "ladybug", "dragonfly")),
        Stage("b1", 1, "翅膀上的颜色", "蝴蝶和蛾。一只靠结构发蓝，一只像枯叶，一只大得吓人。", Mode.NAME, listOf("morpho", "leafwing", "atlas")),
        Stage("b2", 1, "伪装高手", "它们都在假装别的东西。", Mode.NAME, listOf("mantis", "swallow", "dragonfly")),
        Stage("b3", 1, "混在一起了", "昆虫和石头混了一盒，把它们分开。", Mode.SORT, listOf("atlas", "pyrite", "stag", "swallow", "quartz")),
        Stage("c1", 2, "石头也有名字", "三块石头，看颜色，看晶形。", Mode.NAME, listOf("amethyst", "pyrite", "malachite")),
        Stage("c2", 2, "谁能划动谁", "硬的能在软的上面划出痕迹。从最软的开始捏，看卡片上的硬度。", Mode.HARDNESS, listOf("fluorite", "pyrite", "quartz")),
        Stage("c3", 2, "会发光的和会变彩的", "再认三块。", Mode.NAME, listOf("fluorite", "opal", "quartz")),
        Stage("c4", 2, "再排一次硬度", "这次的三块，别被颜色骗了。", Mode.HARDNESS, listOf("malachite", "opal", "amethyst")),
        Stage("d1", 3, "石头里的海", "两块化石和一块树脂，都很古老。", Mode.NAME, listOf("ammonite", "trilobite", "amber")),
        Stage("d2", 3, "谁先来到世上", "从最早出现的开始捏，看卡片上的年代。", Mode.TIMELINE, listOf("amber", "trilobite", "ammonite")),
        Stage("d3", 3, "柜子满了", "最后一盒，什么都有：归好每一屉。", Mode.SORT, listOf("trilobite", "opal", "scarab", "leafwing", "amber", "fluorite")),
    )

    fun byId(id: String) = ALL.first { it.id == id }
}
