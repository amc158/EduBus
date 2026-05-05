package com.alberto.edubus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alberto.edubus.viewmodel.BusViewModel
import com.alberto.edubus.viewmodel.PodcastViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPodcast(
    busViewModel: BusViewModel,
    onVolver: () -> Unit,
    podcastViewModel: PodcastViewModel = viewModel()
) {
    val context = LocalContext.current
    val segundosRestantes by busViewModel.segundosRestantes.collectAsState()
    val estaReproduciendo by podcastViewModel.estaReproduciendo.collectAsState()
    val progreso by podcastViewModel.progreso.collectAsState()
    val tituloPodcast by podcastViewModel.temaActual.collectAsState()

    // Estado para saber si la IA está trabajando (basado en el título que envía el VM)
    val estaCargandoIA = tituloPodcast == "La IA está escribiendo tu guion..."

    val locale = LocalConfiguration.current.locales[0]
    val tiempoTexto = String.format(locale, "%02d:%02d", segundosRestantes / 60, segundosRestantes % 60)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduBus Podcast IA", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1. TIEMPO RESTANTE Y ESTADO
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Llegada en: $tiempoTexto",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                if (estaCargandoIA) {
                    Text(
                        text = "Sincronizando con tu trayecto...",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            // 2. CARÁTULA CON ANIMACIÓN SI CARGA
            Box(contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.size(280.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (estaCargandoIA) {
                            // Círculo de carga sobre la carátula
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(60.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(100.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // 3. TÍTULO DINÁMICO
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = tituloPodcast,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Texto de ayuda dinámico
                val descripcion = if (estaCargandoIA)
                    "Analizando noticias y duración del viaje..."
                else
                    "Contenido único generado para este bus"

                Text(
                    text = descripcion,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // 4. BARRA DE PROGRESO (Slider)
            Column {
                Slider(
                    value = progreso,
                    onValueChange = { /* Implementar seek si se desea */ },
                    enabled = !estaCargandoIA, // Desactivar si la IA está trabajando
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 5. CONTROLES
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                IconButton(onClick = { /* -10s */ }, enabled = !estaCargandoIA) {
                    Icon(Icons.Default.Replay10, contentDescription = null, tint = if(estaCargandoIA) Color.DarkGray else Color.White, modifier = Modifier.size(36.dp))
                }

                // Botón Play/Pause principal
                Surface(
                    onClick = { if (!estaCargandoIA) podcastViewModel.toggleReproduccion(context) },
                    shape = CircleShape,
                    color = if (estaCargandoIA) Color.Gray else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (estaCargandoIA) {
                            // Mini spinner dentro del botón si está cargando
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = if (estaReproduciendo) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = { /* +30s */ }, enabled = !estaCargandoIA) {
                    Icon(Icons.Default.Forward30, contentDescription = null, tint = if(estaCargandoIA) Color.DarkGray else Color.White, modifier = Modifier.size(36.dp))
                }
            }

            // 6. AVISO DE PARADA
            AnimatedVisibility(visible = segundosRestantes < 60) {
                Surface(
                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "⚠️ ¡Prepárate! Tu parada está cerca.",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}