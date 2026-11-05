package com.vrgames.specimen.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.PhysicalLengthConverter
import com.vrgames.specimen.GUIDE_ID
import com.vrgames.specimen.LEVELS_ID
import com.vrgames.specimen.core.Cabinet
import com.vrgames.specimen.core.Group
import com.vrgames.specimen.core.Measure
import com.vrgames.specimen.core.Mode
import com.vrgames.specimen.core.Stages
import com.vrgames.specimen.game.Game

const val PANEL_CARD = "card"
const val PANEL_INTRO = "intro"
const val PANEL_HUD = "hud"
const val PANEL_RESULT = "result"
const val PANEL_SHOW = "show"
const val PANEL_MENU = "menu"
const val PANEL_NOTE = "note"
const val PANEL_ASK = "ask"

val Ink = Color(0xFFFBF3E2)
val Gold = Color(0xFFE8C468)
val Sky = Color(0xFFA8D0C8)
val Mint = Color(0xFF9FE3A8)
val Dim = Color(0xFFCFC4B2)
val Alarm = Color(0xFFFF7B5C)
private val PanelBg = Color(0xEC22170F)
/** Window backdrop: the system glass can be light, and every text color here is meant for dark. */
val WindowBg = Color(0xF422170F)

@Composable
fun PhysicalLengthConverter.m(meters: Float): Dp = lengthToDp(meters, LengthUnit.Meters)

@Composable
fun PhysicalLengthConverter.sz(meters: Float) = m(meters).value.sp

@Composable
private fun PanelBox(c: PhysicalLengthConverter, width: Float, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(c.m(0.016f))
    Column(
        Modifier.width(c.m(width)).clip(shape).background(PanelBg).border(c.m(0.0015f), Color(0x88E8C468), shape).padding(c.m(0.016f)),
        verticalArrangement = Arrangement.spacedBy(c.m(0.008f)),
    ) { content() }
}

/** The step that moves the game on. */
@Composable
fun CallToAction(c: PhysicalLengthConverter, text: String, color: Color = Gold, onClick: () -> Unit) {
    val shape = RoundedCornerShape(c.m(0.014f))
    Text(
        text,
        Modifier.clip(shape).background(color).border(c.m(0.002f), Color(0xFFFFF4E0), shape)
            .clickable { onClick() }.padding(horizontal = c.m(0.024f), vertical = c.m(0.013f)),
        fontSize = c.sz(0.024f),
        color = Color(0xFF1A1420),
    )
}

@Composable
fun Pill(c: PhysicalLengthConverter, text: String, color: Color = Dim, onClick: () -> Unit) {
    val shape = RoundedCornerShape(c.m(0.012f))
    Text(
        text,
        Modifier.clip(shape).border(c.m(0.0015f), color, shape).clickable { onClick() }.padding(horizontal = c.m(0.014f), vertical = c.m(0.008f)),
        fontSize = c.sz(0.017f),
        color = color,
    )
}

private fun stars(n: Int) = "★".repeat(n) + "☆".repeat(3 - n)

@Composable
fun CardPanel(c: PhysicalLengthConverter) {
    PanelBox(c, 0.62f) {
        Game.version // Read in here: the same arguments, so the box would skip its content.
        Text("标本柜", fontSize = c.sz(0.046f), color = Gold)
        Text("一位老博物学家留下一张书桌、一个空柜子，还有几盒没整理的标本。甲虫、蝴蝶、矿物、化石，把它们认出来，放回该放的地方。", fontSize = c.sz(0.018f), color = Ink)
        Text("怎么玩", fontSize = c.sz(0.02f), color = Mint)
        Text("· 捏托盘上的标本：它会升到你面前慢慢转。\n· 看左边的卡片，听讲解；在右边回答。\n· 答错了会少星，但会给提示。", fontSize = c.sz(0.0165f), color = Ink)
        if (Game.stars > 0) Text("柜子里已有 ${Game.collected.size} / ${Cabinet.ALL.size} 件，${Game.stars} / ${Game.maxStars} 颗星", fontSize = c.sz(0.016f), color = Mint)
        Row(horizontalArrangement = Arrangement.spacedBy(c.m(0.01f)), verticalAlignment = Alignment.CenterVertically) {
            CallToAction(c, if (Game.stars > 0) "接着整理 →" else "打开第一盒 →") { Game.start() }
            if (Game.gold) Pill(c, "夜间开放", Gold) { Game.show() }
        }
    }
}

@Composable
fun IntroPanel(c: PhysicalLengthConverter) {
    PanelBox(c, 0.56f) {
        Game.version
        val s = Game.stage
        Text("第 ${Stages.ALL.indexOf(s) + 1} 盒 · ${s.title}", fontSize = c.sz(0.034f), color = Gold)
        Text(Stages.ACTS[s.act] + "　" + s.mode.title, fontSize = c.sz(0.016f), color = Sky)
        Text(s.ask, fontSize = c.sz(0.0185f), color = Ink)
        Text("三星：一次也不错。", fontSize = c.sz(0.015f), color = Dim)
        Row(horizontalArrangement = Arrangement.spacedBy(c.m(0.01f)), verticalAlignment = Alignment.CenterVertically) {
            CallToAction(c, "开始 →") { Game.begin() }
            Pill(c, "返回") { Game.toCard() }
        }
    }
}

/** The real size of a thing beside a one-yuan coin. */
@Composable
private fun SizeGraphic(c: PhysicalLengthConverter, m: Measure.Size) {
    Text(m.label, fontSize = c.sz(0.0145f), color = Sky)
    Canvas(Modifier.fillMaxWidth().height(c.m(0.03f))) {
        val scale = size.width / 26f // 26 cm across the card
        val y = size.height / 2
        drawCircle(Color(0xFFD0D0D0), radius = 1.25f * scale, center = Offset(1.3f * scale, y))
        val x0 = 3.4f * scale
        drawRoundRect(Color(0xFFE8C468), topLeft = Offset(x0, y - size.height * 0.2f), size = Size((m.cm.toFloat() * scale).coerceAtMost(size.width - x0), size.height * 0.4f), cornerRadius = CornerRadius(4f, 4f))
    }
    Text("左边的圆是一元硬币", fontSize = c.sz(0.0115f), color = Dim)
}

/** Mohs scale one to ten, this one lit. */
@Composable
private fun HardnessGraphic(c: PhysicalLengthConverter, m: Measure.Hardness) {
    Text(if (m.low == m.high) "莫氏硬度 ${m.low}" else "莫氏硬度 ${m.low}–${m.high}", fontSize = c.sz(0.0145f), color = Sky)
    Canvas(Modifier.fillMaxWidth().height(c.m(0.022f))) {
        val w = size.width / 10
        for (k in 1..10) {
            val on = k >= kotlin.math.floor(m.low).toInt() && k <= kotlin.math.ceil(m.high).toInt()
            drawRoundRect(if (on) Color(0xFFE8C468) else Color(0x33FFFFFF), topLeft = Offset((k - 1) * w + 2f, 0f), size = Size(w - 4f, size.height), cornerRadius = CornerRadius(3f, 3f))
        }
    }
    Text("指甲约 2.5　铜币约 3.5　小刀约 5.5　钻石 10", fontSize = c.sz(0.0115f), color = Dim)
}

/** Its span on a line of the last 550 million years. */
@Composable
private fun AgeGraphic(c: PhysicalLengthConverter, m: Measure.Age) {
    Text(if (m.from == m.to) "约 ${m.from / 100.0} 亿年前" else "${m.from / 100.0} 亿年前 到 ${m.to / 100.0} 亿年前", fontSize = c.sz(0.0145f), color = Sky)
    Canvas(Modifier.fillMaxWidth().height(c.m(0.024f))) {
        val h = size.height
        drawRect(Color(0x33FFFFFF), topLeft = Offset(0f, h * 0.35f), size = Size(size.width, h * 0.3f))
        fun x(ma: Int) = size.width * (1 - ma / 550f)
        // The dinosaurs' time, for scale.
        drawRect(Color(0x558FE3B0), topLeft = Offset(x(230), h * 0.35f), size = Size(x(66) - x(230), h * 0.3f))
        val w = (x(m.to) - x(m.from)).coerceAtLeast(6f)
        drawRect(Color(0xFFE8C468), topLeft = Offset(x(m.from), h * 0.15f), size = Size(w, h * 0.7f))
    }
    Text("5.5 亿年前 ←　绿色是恐龙时代　→ 今天", fontSize = c.sz(0.0115f), color = Dim)
}

/** The specimen's card: its name once known, the things to look at, a graphic, and the narrator's button. */
@Composable
fun HudPanel(c: PhysicalLengthConverter, toggle: (String) -> Unit) {
    PanelBox(c, 0.34f) {
        Game.version
        val p = Game.play ?: return@PanelBox
        Text("第 ${Stages.ALL.indexOf(p.stage) + 1} 盒 · ${p.stage.title}　${p.done.size}/${p.items.size}", fontSize = c.sz(0.018f), color = Gold)
        val s = p.inHand
        if (s == null) {
            Text("捏托盘上的一件标本。", fontSize = c.sz(0.019f), color = Ink)
        } else {
            val known = p.stage.mode != Mode.NAME
            Text(if (known) s.name else "这是什么？", fontSize = c.sz(0.028f), color = Gold)
            if (known) Text(s.latin, fontSize = c.sz(0.0135f), color = Dim)
            Text("看这里", fontSize = c.sz(0.0145f), color = Mint)
            for (l in s.look) Text("· $l", fontSize = c.sz(0.0165f), color = Ink)
            when (val m = s.measure) {
                is Measure.Size -> SizeGraphic(c, m)
                is Measure.Hardness -> HardnessGraphic(c, m)
                is Measure.Age -> AgeGraphic(c, m)
            }
            Text("产地：" + s.home, fontSize = c.sz(0.0135f), color = Dim)
            Pill(c, "把它放回托盘") { Game.putBack() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(c.m(0.006f))) {
            Pill(c, "标本柜", Gold) { toggle(GUIDE_ID) }
            Pill(c, "关卡") { toggle(LEVELS_ID) }
            Pill(c, "重来") { Game.retry() }
        }
    }
}

/** The question about what is in hand. */
@Composable
fun AskPanel(c: PhysicalLengthConverter) {
    PanelBox(c, 0.32f) {
        Game.version
        val p = Game.play ?: return@PanelBox
        // The last specimen of a box ends it at once: its story stays on offer beside the result.
        if (Game.phase == Game.Phase.RESULT) {
            Game.justPlaced?.let { j ->
                val x = Cabinet.of(j)
                Text(x.name + " 入柜", fontSize = c.sz(0.02f), color = Mint)
                Text(x.wow, fontSize = c.sz(0.0165f), color = Ink)
                NarrationPill(c, j)
            }
            return@PanelBox
        }
        val s = p.inHand
        Game.lastClue?.let { Text(it, fontSize = c.sz(0.0165f), color = Alarm) }
        if (s == null) {
            Text(
                when (p.stage.mode) {
                    Mode.NAME -> "认出它们"
                    Mode.SORT -> "放进该放的抽屉"
                    Mode.HARDNESS -> "从最软的开始"
                    Mode.TIMELINE -> "从最早出现的开始"
                },
                fontSize = c.sz(0.022f), color = Gold,
            )
            Text(p.stage.ask, fontSize = c.sz(0.0155f), color = Ink)
            Game.justPlaced?.let { j ->
                val x = Cabinet.of(j)
                Text(x.name + " 入柜", fontSize = c.sz(0.02f), color = Mint)
                Text(x.wow, fontSize = c.sz(0.0165f), color = Ink)
                NarrationPill(c, j)
            }
        } else when (p.stage.mode) {
            Mode.NAME -> {
                Text("它叫什么？", fontSize = c.sz(0.022f), color = Gold)
                for (n in p.names(s)) CallToAction(c, n, Color(0xFFE8D8A8)) { Game.answerName(n) }
            }
            Mode.SORT -> {
                Text("放进哪一屉？", fontSize = c.sz(0.022f), color = Gold)
                for (g in Group.entries) CallToAction(c, g.title, Color(0xFFE8D8A8)) { Game.answerGroup(g) }
                NarrationPill(c, s.id)
            }
            else -> {
                Text(if (p.stage.mode == Mode.HARDNESS) "它是剩下的里面最软的吗？" else "它是剩下的里面最早的吗？", fontSize = c.sz(0.02f), color = Gold)
                CallToAction(c, "是，排上去") { Game.placeNext() }
                NarrationPill(c, s.id)
            }
        }
    }
}

@Composable
private fun NarrationPill(c: PhysicalLengthConverter, id: String) {
    if (Game.speaking == id) Pill(c, "■ 停止讲解", Alarm) { Game.stopNarration() }
    else Pill(c, "▶ 听讲解", Gold) { Game.narrate(id) }
}

@Composable
fun ResultPanel(c: PhysicalLengthConverter, toggle: (String) -> Unit) {
    PanelBox(c, 0.56f) {
        Game.version
        val p = Game.play ?: return@PanelBox
        Text("${p.stage.title}　${stars(p.stars())}", fontSize = c.sz(0.034f), color = Gold)
        Text(if (p.mistakes == 0) "一次也没错。" else "错了 ${p.mistakes} 次。三星要一次也不错。", fontSize = c.sz(0.018f), color = Ink)
        val tr = Game.trophies
        if (tr > Game.trophiesBefore) Text(
            when (tr) {
                1 -> "得到铜奖杯！十三盒全部整理完。"
                2 -> "得到银奖杯！"
                else -> "得到金奖杯！解锁「夜间开放」。"
            },
            fontSize = c.sz(0.019f), color = Mint,
        )
        Text("柜子里已有 ${Game.collected.size} / ${Cabinet.ALL.size} 件　总星数 ${Game.stars} / ${Game.maxStars}", fontSize = c.sz(0.015f), color = Dim)
        Row(horizontalArrangement = Arrangement.spacedBy(c.m(0.01f)), verticalAlignment = Alignment.CenterVertically) {
            if (Stages.ALL.indexOf(p.stage) < Stages.ALL.size - 1) CallToAction(c, "下一盒 →") { Game.next() }
            Pill(c, "再来一次", Gold) { Game.retry() }
            Pill(c, "标本柜", Sky) { toggle(GUIDE_ID) }
            if (Game.gold) Pill(c, "夜间开放", Gold) { Game.show() }
        }
    }
}

@Composable
fun ShowPanel(c: PhysicalLengthConverter) {
    PanelBox(c, 0.32f) {
        Text("夜间开放", fontSize = c.sz(0.03f), color = Gold)
        Text("十三盒全部三星。灯熄了，蝴蝶飞起来了。", fontSize = c.sz(0.017f), color = Ink)
        Pill(c, "结束") { Game.endShow() }
    }
}

@Composable
fun NotePanel(c: PhysicalLengthConverter) {
    val n = Game.note ?: return
    val shape = RoundedCornerShape(c.m(0.012f))
    Text(n, Modifier.widthIn(max = c.m(0.7f)).clip(shape).background(PanelBg).padding(c.m(0.012f)), fontSize = c.sz(0.024f), color = Gold)
}

@Composable
fun MenuPanel(c: PhysicalLengthConverter, toggle: (String) -> Unit, faceMe: () -> Unit, quit: () -> Unit, version: String) {
    Game.version
    if (!Game.menuOpen) {
        Pill(c, "菜单", Gold) { Game.menuOpen = true }
        return
    }
    PanelBox(c, 0.28f) {
        Game.version
        Pill(c, if (Game.window == LEVELS_ID) "收起关卡" else "关卡（选关、奖杯）", Gold) { toggle(LEVELS_ID) }
        Pill(c, if (Game.window == GUIDE_ID) "收起标本柜" else "标本柜") { toggle(GUIDE_ID) }
        if (Game.gold) Pill(c, "夜间开放", Gold) { Game.menuOpen = false; Game.show() }
        Pill(c, "把书桌摆到我正前方") { faceMe() }
        Pill(c, if (Game.musicOn) "音乐：开" else "音乐：关") { Game.toggleMusic() }
        RestartPill(c)
        Pill(c, "退出") { quit() }
        Text("进度自动保存。版本 $version", fontSize = c.sz(0.0125f), color = Dim)
        Pill(c, "收起菜单") { Game.menuOpen = false }
    }
}

@Composable
private fun RestartPill(c: PhysicalLengthConverter) {
    if (Game.confirmRestart) {
        Row(horizontalArrangement = Arrangement.spacedBy(c.m(0.006f))) {
            Pill(c, "确定清空进度", Alarm) { Game.confirmRestart = false; Game.menuOpen = false; Game.restart() }
            Pill(c, "算了") { Game.confirmRestart = false }
        }
    } else Pill(c, "清空进度从头开始") { Game.confirmRestart = true }
}

@Composable
fun WindowHeader(id: String, title: String, size: TextUnit) {
    val copy = remember { Any() }
    DisposableEffect(copy) {
        Game.windowShown(id, copy)
        onDispose { Game.windowGone(id, copy) }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = size, color = Gold)
        Spacer(Modifier.weight(1f))
        Button(onClick = { Game.closeWindow(id) }) { Text("关闭", fontSize = 22.sp) }
    }
}
