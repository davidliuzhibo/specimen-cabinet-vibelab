package com.vrgames.specimen.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pico.spatial.ui.design.Text
import com.vrgames.specimen.GUIDE_ID
import com.vrgames.specimen.LEVELS_ID
import com.vrgames.specimen.core.Cabinet
import com.vrgames.specimen.core.Group
import com.vrgames.specimen.core.Stages
import com.vrgames.specimen.game.Game

private val CardBg = Color(0x26FBF3E2)

/** 关卡: every box with its stars (pick one to play), and the three trophies. */
@Composable
fun LevelsWindow() {
    Game.version
    Column(Modifier.fillMaxSize().background(WindowBg).padding(28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WindowHeader(LEVELS_ID, "关卡", 32.sp)
        Text("总星数 ${Game.stars} / ${Game.maxStars}。点一盒就能玩。", fontSize = 22.sp, color = Ink)
        Text(
            (if (Game.bronze) "🥉 铜奖杯：十三盒全整理完　" else "铜奖杯：十三盒全整理完（未得）　") +
                (if (Game.silver) "🥈 银奖杯　" else "银奖杯：三分之二的星（未得）　") +
                (if (Game.gold) "🥇 金奖杯：夜间开放" else "金奖杯：全部三星（未得）"),
            fontSize = 21.sp, color = Mint,
        )
        for ((k, s) in Stages.ALL.withIndex()) {
            if (k == 0 || Stages.ALL[k - 1].act != s.act) Text(Stages.ACTS[s.act], fontSize = 25.sp, color = Sky)
            val open = Game.open(s)
            val st = Game.best[s.id] ?: 0
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardBg)
                    .clickable(enabled = open) { Game.closeWindow(LEVELS_ID); Game.intro(s) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("${k + 1}. ${s.title}", fontSize = 24.sp, color = if (open) Ink else Dim)
                    Text(s.mode.title, fontSize = 19.sp, color = Dim)
                }
                Spacer(Modifier.weight(1f))
                Text(if (open) "★".repeat(st) + "☆".repeat(3 - st) else "未开放", fontSize = 23.sp, color = Gold)
            }
        }
    }
}

/** 标本柜: by drawer. A specimen tells its story and can be heard again, once it has been placed. */
@Composable
fun GuideWindow() {
    Game.version
    Column(Modifier.fillMaxSize().background(WindowBg).padding(28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WindowHeader(GUIDE_ID, "标本柜", 32.sp)
        Text("已有 ${Game.collected.size} / ${Cabinet.ALL.size} 件", fontSize = 22.sp, color = Ink)
        for (g in Group.entries) {
            Text(g.title, fontSize = 25.sp, color = Sky)
            Text(g.hint, fontSize = 19.sp, color = Dim)
            for (s in Cabinet.ALL.filter { it.group == g }) {
                val got = s.id in Game.collected
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardBg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (got) s.name else "？？？", fontSize = 24.sp, color = if (got) Gold else Dim)
                        Spacer(Modifier.weight(1f))
                        if (got) Text(
                            if (Game.speaking == s.id) "■ 停止" else "▶ 听讲解",
                            Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x33E8C468)).clickable { if (Game.speaking == s.id) Game.stopNarration() else Game.narrate(s.id) }.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 20.sp, color = Gold,
                        )
                    }
                    if (got) {
                        Text(s.latin + "　" + s.home, fontSize = 18.sp, color = Dim)
                        Text(s.wow, fontSize = 20.sp, color = Ink)
                    } else Text("还没进柜子。", fontSize = 19.sp, color = Dim)
                }
            }
        }
    }
}
