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
import com.alberto.edubus.viewmodel.MinijuegosViewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMinijuegos(
    busViewModel: BusViewModel,
    onVolver: () -> Unit,
    minijuegosViewModel: MinijuegosViewModel = viewModel()
) {
    val palabraActual by minijuegosViewModel.palabraActual.collectAsState()
    val palabraDesordenada by minijuegosViewModel.palabraDesordenada.collectAsState()
    val juegoTerminado by minijuegosViewModel.juegoTerminado.collectAsState()
    val puntuacion by minijuegosViewModel.puntuacion.collectAsState() // Leemos del ViewModel
    val segundosRestantes by busViewModel.segundosRestantes.collectAsState()

    var respuestaUsuario by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf(false) }

    val minutosReloj = segundosRestantes / 60
    val segundosReloj = segundosRestantes % 60
    val locale = LocalConfiguration.current.locales[0]
    val textoReloj = remember(minutosReloj, segundosReloj, locale) { String.format(locale, "%02d:%02d", minutosReloj, segundosReloj) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EduBus Minijuegos", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                // --- NUEVO: ESTO EMPUJA EL CONTENIDO CUANDO SALE EL TECLADO ---
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Marcador de tiempo y puntos (Fijo arriba)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (segundosRestantes < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("⏳ $textoReloj", color = if (segundosRestantes < 60) Color.White else MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                }
                Text("🏆 $puntuacion pts", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.tertiary)
            }

            if (juegoTerminado) {
                Spacer(modifier = Modifier.weight(1f))
                Text("¡Has resuelto todos los anagramas!", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Puntos ganados: $puntuacion", fontSize = 20.sp, modifier = Modifier.padding(vertical = 24.dp), fontWeight = FontWeight.Bold)
                Button(onClick = onVolver, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Salir y Guardar") }
                Spacer(modifier = Modifier.weight(2f))
            } else {

                // --- NUEVO: "Muelles" (weight) en lugar de espacios fijos ---
                Spacer(modifier = Modifier.weight(0.5f))

                Text("Descifra la palabra oculta:", fontSize = 16.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Text(palabraDesordenada, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp), color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("💡 Pista: ${palabraActual.pista}", fontSize = 16.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.weight(1f)) // Muelle central grande

                OutlinedTextField(
                    value = respuestaUsuario,
                    onValueChange = { respuestaUsuario = it.uppercase(); mensajeError = false },
                    label = { Text("Escribe tu respuesta") },
                    singleLine = true,
                    isError = mensajeError,
                    modifier = Modifier.fillMaxWidth(),
                    // Acción de "Hecho" en el teclado
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            if (minijuegosViewModel.comprobarRespuesta(respuestaUsuario)) {
                                respuestaUsuario = ""; mensajeError = false
                            } else { mensajeError = true }
                        }
                    )
                )

                if (mensajeError) {
                    Text("Palabra incorrecta.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { minijuegosViewModel.saltarPalabra(); respuestaUsuario = ""; mensajeError = false }, modifier = Modifier.weight(1f).height(56.dp)) { Text("Saltar") }
                    Button(
                        onClick = {
                            if (minijuegosViewModel.comprobarRespuesta(respuestaUsuario)) {
                                respuestaUsuario = ""; mensajeError = false
                            } else { mensajeError = true }
                        }, modifier = Modifier.weight(1f).height(56.dp)) { Text("Comprobar") }
                }

                Spacer(modifier = Modifier.weight(0.5f)) // Muelle inferior
            }
        }
    }
}