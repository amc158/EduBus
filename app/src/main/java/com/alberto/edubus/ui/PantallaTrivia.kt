package com.alberto.edubus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alberto.edubus.viewmodel.BusViewModel
import com.alberto.edubus.viewmodel.TriviaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTrivia(
    viewModel: TriviaViewModel,
    busViewModel: BusViewModel,
    onVolver: () -> Unit
) {
    val preguntas by viewModel.preguntas.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val mensajeCarga by viewModel.mensajeCarga.collectAsState()
    val puntuacion by viewModel.puntuacion.collectAsState() // Leemos del VM
    val segundosGlobales by busViewModel.segundosRestantes.collectAsState()

    var indicePregunta by remember { mutableIntStateOf(0) }
    var juegoTerminado by remember { mutableStateOf(false) }
    var opcionSeleccionada by remember { mutableStateOf<String?>(null) }
    var mostrarResultado by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val minutosReloj = segundosGlobales / 60
    val segundosReloj = segundosGlobales % 60
    val locale = LocalConfiguration.current.locales[0]
    val textoReloj = remember(minutosReloj, segundosReloj, locale) { String.format(locale, "%02d:%02d", minutosReloj, segundosReloj) }

    LaunchedEffect(Unit) {
        val cantidadPreguntas = (segundosGlobales / 45).coerceIn(5, 50)
        viewModel.cargarPreguntas(cantidadPreguntas)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduBus Trivia", color = Color.White) },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (segundosGlobales < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("⏳ $textoReloj", color = if (segundosGlobales < 60) Color.White else MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text("🏆 $puntuacion pts", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.tertiary)
            }

            if (cargando) {
                Spacer(modifier = Modifier.height(100.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(mensajeCarga, color = Color.Gray, textAlign = TextAlign.Center)

            } else if (preguntas.isEmpty()) {
                Text("Error al cargar las preguntas. Revisa tu conexión.")
                Button(onClick = onVolver, modifier = Modifier.padding(top = 16.dp)) { Text("Volver") }

            } else if (juegoTerminado) {
                Spacer(modifier = Modifier.height(50.dp))
                Text("¡Partida Completada!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Has conseguido $puntuacion puntos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 24.dp))
                Button(onClick = onVolver, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Text("Finalizar y Guardar Puntos", color = MaterialTheme.colorScheme.onTertiary) }
            } else {
                val preguntaActual = preguntas[indicePregunta]
                val respuestasBarajadas = remember(indicePregunta) { (preguntaActual.incorrectAnswers + preguntaActual.correctAnswer).shuffled() }

                Text("Pregunta ${indicePregunta + 1} / ${preguntas.size}", color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                Text(preguntaActual.question, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp))

                respuestasBarajadas.forEach { respuesta ->
                    val colorFondo = if (mostrarResultado) {
                        when {
                            respuesta == preguntaActual.correctAnswer -> Color(0xFF2E7D32)
                            respuesta == opcionSeleccionada -> Color(0xFFD32F2F)
                            else -> Color.LightGray
                        }
                    } else { MaterialTheme.colorScheme.primary }

                    Button(
                        onClick = {
                            if (!mostrarResultado) {
                                opcionSeleccionada = respuesta
                                mostrarResultado = true
                                if (respuesta == preguntaActual.correctAnswer) viewModel.sumarPuntoCorrecto()

                                scope.launch {
                                    delay(1500)
                                    if (indicePregunta < preguntas.size - 1) indicePregunta++ else juegoTerminado = true
                                    mostrarResultado = false
                                    opcionSeleccionada = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorFondo),
                        enabled = !mostrarResultado || respuesta == opcionSeleccionada || respuesta == preguntaActual.correctAnswer,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 56.dp)
                    ) {
                        Text(respuesta, fontSize = 16.sp, textAlign = TextAlign.Center, color = if (mostrarResultado && respuesta != preguntaActual.correctAnswer && respuesta != opcionSeleccionada) Color.DarkGray else Color.White)
                    }
                }
            }
        }
    }
}