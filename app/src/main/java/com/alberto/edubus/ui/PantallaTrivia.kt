package com.alberto.edubus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alberto.edubus.TriviaViewModel

@Composable
fun PantallaTrivia(
    viewModel: TriviaViewModel,
    tiempoDisponible: Int,
    onVolver: () -> Unit
) {
    val preguntas by viewModel.preguntas.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val mensajeCarga by viewModel.mensajeCarga.collectAsState() // <--- Escuchamos el mensaje dinámico

    var indicePregunta by remember { mutableIntStateOf(0) }
    var puntuacion by remember { mutableIntStateOf(0) }
    var juegoTerminado by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cantidadPreguntas = if (tiempoDisponible > 0) tiempoDisponible else 5
        viewModel.cargarPreguntas(cantidadPreguntas)
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("EduBus Trivia", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cargando) {
                Spacer(modifier = Modifier.height(100.dp))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                // Mostramos en qué paso va la IA
                Text(mensajeCarga, color = Color.Gray, textAlign = TextAlign.Center)

            } else if (preguntas.isEmpty()) {
                Text("Error al cargar las preguntas. Revisa tu conexión.")
                Button(onClick = onVolver, modifier = Modifier.padding(top = 16.dp)) { Text("Volver") }
            } else if (juegoTerminado) {
                Spacer(modifier = Modifier.height(50.dp))
                Text("¡Llegando a tu destino!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Has acertado $puntuacion puntos de ${preguntas.size * 10}", fontSize = 20.sp, modifier = Modifier.padding(vertical = 16.dp))
                Button(onClick = onVolver) { Text("Finalizar y Guardar Puntos") }
            } else {
                val preguntaActual = preguntas[indicePregunta]

                val respuestasBarajadas = remember(indicePregunta) {
                    (preguntaActual.incorrectAnswers + preguntaActual.correctAnswer).shuffled()
                }

                Text("Pregunta ${indicePregunta + 1} / ${preguntas.size}", color = Color.Gray)
                Text("Puntuación: $puntuacion", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Spacer(modifier = Modifier.height(24.dp))

                // Ya no llamamos a decodificarTexto aquí, el ViewModel nos lo da limpio y en español
                Text(
                    text = preguntaActual.question,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                )

                respuestasBarajadas.forEach { respuesta ->
                    Button(
                        onClick = {
                            if (respuesta == preguntaActual.correctAnswer) {
                                puntuacion += 10
                            }
                            if (indicePregunta < preguntas.size - 1) {
                                indicePregunta++
                            } else {
                                juegoTerminado = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 56.dp)                    ) {
                        Text(text = respuesta, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}