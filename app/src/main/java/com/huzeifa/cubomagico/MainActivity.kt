package com.huzeifa.cubomagico

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.room.Room
import com.huzeifa.cubomagico.data.CubeDatabase
import com.huzeifa.cubomagico.data.SolveTime
import com.huzeifa.cubomagico.solver.NativeSolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val Background = Color(0xFF0E1116)
private val Panel = Color(0xFF171D25)
private val PanelLight = Color(0xFF202936)
private val Accent = Color(0xFF39D6A4)
private val Muted = Color(0xFF9AA8B8)
private val Faces = listOf('U', 'R', 'F', 'D', 'L', 'B')

private fun stickerColor(letter: Char): Color = when (letter) {
    'U' -> Color(0xFFF5F5F5)
    'R' -> Color(0xFFE63946)
    'F' -> Color(0xFF2BB673)
    'D' -> Color(0xFFFFD500)
    'L' -> Color(0xFFFF8C1A)
    else -> Color(0xFF1E6FD9)
}
private fun solvedFacelets() = Faces.joinToString("") { it.toString().repeat(9) }
private fun makeScramble(): String {
    val faces = listOf('U', 'R', 'F', 'D', 'L', 'B')
    val suffixes = listOf("", "'", "2")
    val opposite = mapOf('U' to 'D', 'D' to 'U', 'R' to 'L', 'L' to 'R', 'F' to 'B', 'B' to 'F')
    val moves = mutableListOf<String>()
    var previous = ' '
    repeat(Random.nextInt(20, 26)) {
        var face: Char
        do { face = faces.random() } while (face == previous || opposite[face] == previous)
        moves += face.toString() + suffixes.random()
        previous = face
    }
    return moves.joinToString(" ")
}
private fun formatTime(ms: Long): String {
    val minutes = ms / 60000
    val seconds = (ms % 60000) / 1000
    val centis = (ms % 1000) / 10
    return if (minutes > 0) "%d:%02d.%02d".format(minutes, seconds, centis)
    else "%d.%02d".format(seconds, centis)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent, background = Background, surface = Panel,
                onBackground = Color.White, onSurface = Color.White
            )) { CuboMagicoApp() }
        }
    }
}

private enum class Page { Home, Solver, Solution, Scramble, Timer, History }

@Composable
private fun CuboMagicoApp() {
    var page by remember { mutableStateOf(Page.Home) }
    var splash by remember { mutableStateOf(true) }
    var scramble by remember { mutableStateOf(makeScramble()) }
    var solution by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { delay(1800); splash = false }
    Surface(Modifier.fillMaxSize(), color = Background) {
        if (splash) SplashScreen() else Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            if (page != Page.Home) {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹  Voltar", color = Accent, modifier = Modifier.clickable {
                        page = if (page == Page.Solution) Page.Solver else Page.Home
                    }.padding(8.dp))
                    Spacer(Modifier.weight(1f))
                    Text("CUBO MÁGICO", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            when (page) {
                Page.Home -> HomeScreen(
                    { page = Page.Solver }, { page = Page.Timer },
                    { page = Page.Scramble }, { page = Page.History }
                )
                Page.Solver -> SolverScreen { solution = it; page = Page.Solution }
                Page.Solution -> SolutionScreen(solution)
                Page.Scramble -> ScrambleScreen(scramble, { scramble = makeScramble() }, { page = Page.Timer })
                Page.Timer -> TimerScreen(scramble) { page = Page.History }
                Page.History -> HistoryScreen()
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Column(
        Modifier.fillMaxSize().background(Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CubeMark()
        Spacer(Modifier.height(26.dp))
        Text("CUBO MÁGICO", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        Text("CRIADO POR HUZEIFA", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
private fun CubeMark() {
    Column {
        repeat(3) { row ->
            Row {
                repeat(3) { col ->
                    val palette = listOf(Color.White, Color(0xFFE63946), Color(0xFF2BB673), Color(0xFFFFD500), Color(0xFFFF8C1A), Color(0xFF1E6FD9))
                    Box(Modifier.padding(3.dp).size(34.dp).clip(RoundedCornerShape(7.dp)).background(palette[(row * 3 + col) % palette.size]))
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onSolver: () -> Unit, onTimer: () -> Unit, onScramble: () -> Unit, onHistory: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        Text("Domina o cubo.", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
        Text("Resolve, aprende e melhora os teus tempos.", color = Muted, fontSize = 15.sp)
        Box(Modifier.fillMaxWidth().height(188.dp).clip(RoundedCornerShape(24.dp)).background(Panel)) {
            Cube3DPreview(Modifier.fillMaxSize())
            Text("3×3", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp))
        }
        HomeAction("🧩", "Resolver cubo", "Introduz as cores e calcula a solução", onSolver)
        HomeAction("⏱", "Cronómetro", "Treina e guarda os teus tempos", onTimer)
        HomeAction("🔀", "Embaralhar", "Gera uma sequência para treinar", onScramble)
        HomeAction("📊", "Histórico", "Consulta recordes e médias", onHistory)
        Text("Android 5.0+  •  armeabi-v7a + arm64-v8a", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 22.dp))
    }
}

@Composable
private fun HomeAction(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 27.sp)
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            Text("›", color = Accent, fontSize = 26.sp)
        }
    }
}

@Composable
private fun SolverScreen(onSolved: (String) -> Unit) {
    val stickers = remember { mutableStateListOf<Char>().apply { addAll(solvedFacelets().toList()) } }
    var selected by remember { mutableStateOf('U') }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val counts = Faces.associateWith { face -> stickers.count { it == face } }
    val validCounts = counts.values.all { it == 9 }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text("Introduzir o cubo", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("Escolhe uma cor e toca nos adesivos. Os centros são fixos.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        repeat(2) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(3) { col ->
                    FaceGrid(row * 3 + col, stickers, selected, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(9.dp))
        }
        Text("Paleta de cores", color = Color.White, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Faces.forEach { face ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(stickerColor(face))
                            .then(if (selected == face) Modifier.border(3.dp, Accent, CircleShape) else Modifier)
                            .clickable { selected = face }
                    )
                    Text(counts[face].toString() + "/9", color = Muted, fontSize = 10.sp)
                }
            }
        }
        Text(if (validCounts) "Contagem correta: 9 adesivos por cor" else "Cada cor precisa de 9 adesivos.", color = if (validCounts) Accent else Color(0xFFFFB36B), fontSize = 12.sp)
        if (message.isNotBlank()) Text(message, color = Color(0xFFFFB36B), modifier = Modifier.padding(top = 8.dp))
        Button(
            onClick = {
                if (!validCounts) message = "Corrige a contagem das seis cores antes de resolver."
                else {
                    busy = true
                    message = "A preparar o solver..."
                    val state = stickers.joinToString("")
                    scope.launch {
                        val result = withContext(Dispatchers.Default) {
                            try { NativeSolver.solve(state) }
                            catch (error: Throwable) { "Erro ao iniciar o solver: " + (error.message ?: "desconhecido") }
                        }
                        busy = false
                        if (result.startsWith("Erro", true) || result.contains("Error")) message = result
                        else { message = ""; onSolved(result) }
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)
        ) { Text(if (busy) "A resolver..." else "RESOLVER CUBO", fontWeight = FontWeight.Black) }
        Text("Ordem interna: U, R, F, D, L, B. O solver também verifica se a combinação de peças é fisicamente possível.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun FaceGrid(
    faceIndex: Int,
    stickers: androidx.compose.runtime.snapshots.SnapshotStateList<Char>,
    selected: Char,
    modifier: Modifier = Modifier
) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Panel).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Faces[faceIndex].toString(), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        repeat(3) { row ->
            Row {
                repeat(3) { col ->
                    val index = faceIndex * 9 + row * 3 + col
                    Box(
                        Modifier.padding(1.dp).weight(1f).height(27.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(stickerColor(stickers[index]))
                            .border(1.dp, Color(0xFF0B0E12), RoundedCornerShape(4.dp))
                            .clickable(enabled = index % 9 != 4) { stickers[index] = selected }
                    )
                }
            }
        }
    }
}

@Composable
private fun SolutionScreen(solution: String) {
    val moves = solution.trim().split(Regex("\\s+")).filter { it.isNotBlank() && !it.startsWith("(") }
    var selectedMove by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text("Solução encontrada", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text(moves.size.toString() + " movimentos • algoritmo two-phase", color = Accent, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(20.dp)).background(Panel)) {
            Cube3DPreview(Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(15.dp))
        Text("Sequência de movimentos", color = Color.White, fontWeight = FontWeight.Bold)
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            moves.chunked(5).forEachIndexed { chunkIndex, chunk ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    chunk.forEachIndexed { moveIndex, move ->
                        val absolute = chunkIndex * 5 + moveIndex
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (absolute == selectedMove) Accent else PanelLight)
                                .clickable { selectedMove = absolute }.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(move, color = if (absolute == selectedMove) Background else Color.White, fontWeight = FontWeight.Bold) }
                    }
                    repeat(5 - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text("R = direita, U = cima, F = frente, D = baixo, L = esquerda, B = trás. ' indica sentido anti-horário; 2 indica meia-volta.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
        Text("Movimento selecionado: " + (moves.getOrNull(selectedMove) ?: "—"), color = Accent, modifier = Modifier.padding(top = 10.dp))
        Button(onClick = { selectedMove = (selectedMove + 1).coerceAtMost((moves.size - 1).coerceAtLeast(0)) }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Text("PRÓXIMO MOVIMENTO")
        }
    }
}

@Composable
private fun ScrambleScreen(scramble: String, onGenerate: () -> Unit, onTimer: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
        Text("Embaralhamento", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("Sequência gerada com 20 a 25 movimentos.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)).background(Panel)) { Cube3DPreview(Modifier.fillMaxSize()) }
        Text(scramble, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, modifier = Modifier.padding(vertical = 24.dp))
        Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) { Text("GERAR OUTRO") }
        Button(onClick = onTimer, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)) {
            Text("IR PARA O CRONÓMETRO")
        }
    }
}

@Composable
private fun TimerScreen(scramble: String, onHistory: () -> Unit) {
    val context = LocalContext.current
    val database = remember { Room.databaseBuilder(context, CubeDatabase::class.java, "cube-times.db").build() }
    val dao = database.solveTimeDao()
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    var startAt by remember { mutableStateOf(0L) }
    var elapsed by remember { mutableStateOf(0L) }
    var lastId by remember { mutableStateOf<Long?>(null) }
    var penalty by remember { mutableStateOf("OK") }

    LaunchedEffect(running) {
        while (running) {
            elapsed = SystemClock.elapsedRealtime() - startAt
            delay(12)
        }
    }

    Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
        Text("Cronómetro", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("Toca e solta para iniciar; toca novamente para parar.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Text(scramble, color = Muted, fontSize = 13.sp, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(14.dp))
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp).clip(RoundedCornerShape(24.dp))
                .background(if (ready) Color(0xFF14392F) else Panel)
                .pointerInput(running, scramble) {
                    detectTapGestures(onPress = {
                        if (running) {
                            elapsed = SystemClock.elapsedRealtime() - startAt
                            running = false
                            penalty = "OK"
                            lastId = null
                            scope.launch {
                                lastId = dao.insert(SolveTime(timeMillis = elapsed, scramble = scramble, createdAt = System.currentTimeMillis(), penalty = "OK"))
                            }
                        } else {
                            ready = true
                            val released = tryAwaitRelease()
                            ready = false
                            if (released) {
                                elapsed = 0L
                                startAt = SystemClock.elapsedRealtime()
                                running = true
                            }
                        }
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatTime(elapsed), color = if (ready) Accent else Color.White, fontSize = 56.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Light)
                Text(when {
                    running -> "A CRONOMETRAR"
                    ready -> "PRONTO..."
                    elapsed > 0 -> "TEMPO REGISTADO"
                    else -> "TOCA E SOLTA"
                }, color = if (ready) Accent else Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                if (penalty != "OK") Text(penalty, color = Color(0xFFFFB36B), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (!running && elapsed > 0L) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { penalty = "+2"; lastId?.let { id -> scope.launch { dao.updatePenalty(id, "+2") } } }, modifier = Modifier.weight(1f)) { Text("+2") }
                Button(onClick = { penalty = "DNF"; lastId?.let { id -> scope.launch { dao.updatePenalty(id, "DNF") } } }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A3038))) { Text("DNF") }
            }
            Button(onClick = { elapsed = 0L; penalty = "OK"; lastId = null }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("LIMPAR RESULTADO") }
        }
        Button(onClick = onHistory, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp), colors = ButtonDefaults.buttonColors(containerColor = PanelLight)) { Text("VER HISTÓRICO") }
    }
}

@Composable
private fun HistoryScreen() {
    val context = LocalContext.current
    val database = remember { Room.databaseBuilder(context, CubeDatabase::class.java, "cube-times.db").build() }
    val dao = database.solveTimeDao()
    var entries by remember { mutableStateOf(emptyList<SolveTime>()) }
    LaunchedEffect(Unit) { entries = dao.latest() }
    val valid = entries.filter { it.penalty != "DNF" }
    val best = valid.minOfOrNull { it.timeMillis + if (it.penalty == "+2") 2000 else 0 }
    val average = if (valid.isNotEmpty()) valid.map { it.timeMillis + if (it.penalty == "+2") 2000 else 0 }.average().toLong() else null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 22.dp)) {
        Text("Histórico", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text(entries.size.toString() + " tentativas guardadas", color = Muted, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("MELHOR", best?.let { formatTime(it) } ?: "—", Modifier.weight(1f))
            StatCard("MÉDIA", average?.let { formatTime(it) } ?: "—", Modifier.weight(1f))
        }
        if (entries.isEmpty()) Text("Ainda não existem tempos. Vai ao cronómetro para guardar a primeira resolução.", color = Muted)
        else entries.forEach { entry ->
            Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(formatTime(entry.timeMillis), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(entry.createdAt)), color = Muted, fontSize = 11.sp)
                    }
                    Text(entry.penalty, color = if (entry.penalty == "DNF") Color(0xFFFF7D86) else Accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Cube3DPreview(modifier: Modifier = Modifier) {
    AndroidView(modifier = modifier, factory = { context -> CubeGLView(context) })
}
