package com.alberto.edubus.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.alberto.edubus.auth.AuthManager
import com.alberto.edubus.data.BusRepository
import com.alberto.edubus.model.Parada
import com.alberto.edubus.model.RutaBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.alberto.edubus.BusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    viewModel: BusViewModel,
    onCerrarSesion: () -> Unit,
    onPerfil: () -> Unit,
    onJugarTrivia: (Int) -> Unit // <--- AÑADIDO EL PARÁMETRO AQUÍ
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val authManager = remember { AuthManager() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Text("Menú EduBus", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Personalizar Perfil") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onPerfil() }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        authManager.cerrarSesion(onCerrarSesion)
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Líneas Disponibles", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
                )
            }
        ) { paddingValues ->
            // Le pasamos la función al contenido
            ContenidoRutas(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues),
                onJugarTrivia = onJugarTrivia // <--- PASAMOS EL PARÁMETRO HACIA ABAJO
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContenidoRutas(
    viewModel: BusViewModel,
    modifier: Modifier = Modifier,
    onJugarTrivia: (Int) -> Unit // <--- AÑADIDO EL PARÁMETRO AQUÍ TAMBIÉN
) {
    val context = LocalContext.current
    var rutaDatos by remember { mutableStateOf<RutaBus?>(null) }
    var lineasExpandidas by remember { mutableStateOf(mapOf<String, Boolean>()) }

    var sentidoIda by remember { mutableStateOf(true) }
    var origenSeleccionado by remember { mutableStateOf<Parada?>(null) }
    var destinoSeleccionado by remember { mutableStateOf<Parada?>(null) }
    var expandidoOrigen by remember { mutableStateOf(false) }
    var expandidoDestino by remember { mutableStateOf(false) }

    val tiempoDisponible by viewModel.tiempoDisponible.collectAsState()

    LaunchedEffect(Unit) {
        val repo = BusRepository(context)
        rutaDatos = repo.obtenerRuta()
    }

    if (rutaDatos == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val paradasActuales = if (sentidoIda) rutaDatos!!.paradasIda else rutaDatos!!.paradasVuelta
        val horariosActuales = rutaDatos!!.obtenerHorariosActuales(sentidoIda)

        LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {

            // --- PLANIFICADOR DE VIAJE ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Planifica tu viaje (Línea ${rutaDatos!!.lineaId})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        TabRow(selectedTabIndex = if (sentidoIda) 0 else 1) {
                            Tab(
                                selected = sentidoIda,
                                onClick = {
                                    sentidoIda = true
                                    origenSeleccionado = null
                                    destinoSeleccionado = null
                                    viewModel.calcularTiempoTotalParaActividades(0, 0)
                                },
                                text = { Text("Hacia Costacabana", fontSize = 12.sp) }
                            )
                            Tab(
                                selected = !sentidoIda,
                                onClick = {
                                    sentidoIda = false
                                    origenSeleccionado = null
                                    destinoSeleccionado = null
                                    viewModel.calcularTiempoTotalParaActividades(0, 0)
                                },
                                text = { Text("Hacia Torrecárdenas", fontSize = 12.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Origen
                        ExposedDropdownMenuBox(
                            expanded = expandidoOrigen,
                            onExpandedChange = { expandidoOrigen = it }
                        ) {
                            OutlinedTextField(
                                value = origenSeleccionado?.nombre ?: "Selecciona origen",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Parada Actual") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoOrigen) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandidoOrigen,
                                onDismissRequest = { expandidoOrigen = false }
                            ) {
                                paradasActuales.forEach { parada ->
                                    DropdownMenuItem(
                                        text = { Text(parada.nombre) },
                                        onClick = {
                                            origenSeleccionado = parada
                                            expandidoOrigen = false
                                            actualizarTiempos(origenSeleccionado, destinoSeleccionado, horariosActuales, viewModel)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Destino
                        ExposedDropdownMenuBox(
                            expanded = expandidoDestino,
                            onExpandedChange = { expandidoDestino = it }
                        ) {
                            OutlinedTextField(
                                value = destinoSeleccionado?.nombre ?: "Selecciona destino",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Parada de Destino") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoDestino) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = expandidoDestino,
                                onDismissRequest = { expandidoDestino = false }
                            ) {
                                paradasActuales.forEach { parada ->
                                    DropdownMenuItem(
                                        text = { Text(parada.nombre) },
                                        onClick = {
                                            destinoSeleccionado = parada
                                            expandidoDestino = false
                                            actualizarTiempos(origenSeleccionado, destinoSeleccionado, horariosActuales, viewModel)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- RESULTADO DEL ALGORITMO Y BOTÓN DE TRIVIA ---
            if (tiempoDisponible > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("¡Tienes $tiempoDisponible minutos!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Actividades sugeridas para tu trayecto:")
                            Spacer(modifier = Modifier.height(16.dp))

                            // BOTÓN QUE LANZA LA TRIVIA
                            Button(
                                onClick = { onJugarTrivia(tiempoDisponible) }, // <--- AQUÍ SE USA LA FUNCIÓN
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Jugar Trivia Rápida ($tiempoDisponible min)")
                            }
                        }
                    }
                }
            } else if (origenSeleccionado != null && destinoSeleccionado != null) {
                item {
                    Text("⚠️ El destino seleccionado es anterior al origen. Por favor, cambia las paradas.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 24.dp))
                }
            }

            // --- LÍNEAS ---
            item {
                Text("Detalle de la ruta:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                ItemLineaBus(
                    ruta = rutaDatos!!,
                    expandido = lineasExpandidas[rutaDatos!!.lineaId] == true,
                    onClick = {
                        val id = rutaDatos!!.lineaId
                        val actual = lineasExpandidas[id] ?: false
                        lineasExpandidas = lineasExpandidas.toMutableMap().apply { put(id, !actual) }
                    }
                )
            }
        }
    }
}

@Composable
fun ItemLineaBus(ruta: RutaBus, expandido: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = ruta.nombreComercial, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            AnimatedVisibility(visible = expandido) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("IDA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val horariosIdaHoy = ruta.obtenerHorariosActuales(esIda = true)
                    ruta.paradasIda.forEach { parada ->
                        ItemParada(parada, ruta.idSurbusFiltro, ruta.lineaId, horariosIdaHoy)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("VUELTA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val horariosVueltaHoy = ruta.obtenerHorariosActuales(esIda = false)
                    ruta.paradasVuelta.forEach { parada ->
                        ItemParada(parada, ruta.idSurbusFiltro, ruta.lineaId, horariosVueltaHoy)
                    }
                }
            }
        }
    }
}

@Composable
fun ItemParada(parada: Parada, idSurbusFiltro: String, nombreCorto: String, horariosSalida: List<String>?) {
    var mostrarDialogoTiempo by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "• ${parada.nombre}", modifier = Modifier.weight(1f))
        TextButton(onClick = { mostrarDialogoTiempo = true }) {
            Text("Tiempo Real")
        }
    }

    if (mostrarDialogoTiempo) {
        DialogoTiempoReal(
            parada = parada,
            idSurbusFiltro = idSurbusFiltro,
            nombreLinea = nombreCorto,
            horariosSalida = horariosSalida,
            onDismiss = { mostrarDialogoTiempo = false }
        )
    }
}

@Composable
fun DialogoTiempoReal(
    parada: Parada,
    idSurbusFiltro: String,
    nombreLinea: String,
    horariosSalida: List<String>?,
    onDismiss: () -> Unit
) {
    val tiempoOffline = remember { calcularTiempoEstimadoOffline(parada.tiempoAcumulado, horariosSalida) }

    var tiempoRestante by remember { mutableStateOf("") }
    var origenDato by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }
    var refrescarKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refrescarKey) {
        cargando = true
        delay(5000)
        if (cargando) {
            tiempoRestante = tiempoOffline
            origenDato = "Modo Offline (Predicción guardada)"
            cargando = false
        }
    }

    key(refrescarKey) {
        Box(modifier = Modifier.size(0.dp)) {
            AndroidView(factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36"

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            postDelayed({
                                evaluateJavascript(
                                    """
                                    (function() {
                                        var el = document.querySelector('span[id^="waitTime$idSurbusFiltro"]');
                                        if (el) {
                                            var txt = el.innerText.trim();
                                            return (txt !== "" && txt !== "Calculando...") ? txt : "Sin buses próximos";
                                        }
                                        return "No disponible";
                                    })();
                                    """.trimIndent()
                                ) { valor ->
                                    if (!cargando) return@evaluateJavascript

                                    val limpio = valor.replace("\"", "")
                                    if (limpio != "null" && limpio.isNotBlank() && limpio != "No disponible" && limpio != "Sin buses próximos") {
                                        tiempoRestante = limpio
                                        origenDato = "Tiempo Real (Surbus GPS)"
                                    } else {
                                        tiempoRestante = tiempoOffline
                                        origenDato = "Modo Offline (Predicción guardada)"
                                    }
                                    cargando = false
                                }
                            }, 2500)
                        }
                    }
                    loadUrl("https://www.surbusalmeria.es/tiempos-de-espera/parada/${parada.idParada}")
                }
            })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Línea $nombreLinea", fontWeight = FontWeight.Bold)
                if (!cargando) {
                    IconButton(onClick = {
                        cargando = true
                        refrescarKey++
                    }) {
                        Icon(Icons.Default.Autorenew, contentDescription = "Refrescar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Parada: ${parada.nombre}", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))

                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Conectando con servidores...", fontSize = 16.sp, color = Color.Gray)
                } else {
                    Text(
                        text = tiempoRestante,
                        fontSize = if (tiempoRestante.contains("Aprox.")) 26.sp else 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (origenDato.contains("Offline")) Color.Gray else Color(0xFF2E7D32),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = origenDato, fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

fun calcularTiempoEstimadoOffline(
    tiempoAcumulado: Int,
    horariosSalidaCabecera: List<String>?
): String {
    if (horariosSalidaCabecera.isNullOrEmpty()) return "Horarios no disponibles"

    val ahora = LocalTime.now()
    val formatter = DateTimeFormatter.ofPattern("H:mm")

    val ahoraMinutos = if (ahora.hour < 4) {
        (ahora.hour * 60) + ahora.minute + 1440
    } else {
        (ahora.hour * 60) + ahora.minute
    }

    var menorTiempoEspera: Int = Int.MAX_VALUE

    for (horaString in horariosSalidaCabecera) {
        try {
            val match = Regex("(\\d{1,2})[:.-](\\d{2})").find(horaString)
            if (match != null) {
                val (horaStr, minStr) = match.destructured
                var salidaMinutos = (horaStr.toInt() * 60) + minStr.toInt()

                if (salidaMinutos < 4 * 60) {
                    salidaMinutos += 1440
                }

                val llegadaMinutos = salidaMinutos + tiempoAcumulado
                val minutosRestantes = llegadaMinutos - ahoraMinutos

                if (minutosRestantes in 0..120) {
                    if (minutosRestantes < menorTiempoEspera) {
                        menorTiempoEspera = minutosRestantes
                    }
                }
            }
        } catch (e: Exception) {
            continue
        }
    }

    return if (menorTiempoEspera != Int.MAX_VALUE) {
        "Aprox. $menorTiempoEspera min (Offline)"
    } else {
        "Fin del servicio por hoy"
    }
}

fun actualizarTiempos(origen: Parada?, destino: Parada?, horariosSalida: List<String>?, viewModel: BusViewModel) {
    if (origen != null && destino != null) {
        val tiempoTrayecto = destino.tiempoAcumulado - origen.tiempoAcumulado

        if (tiempoTrayecto > 0) {
            val textoEspera = calcularTiempoEstimadoOffline(origen.tiempoAcumulado, horariosSalida)
            val esperaMinutos = Regex("\\d+").find(textoEspera)?.value?.toInt() ?: 0

            viewModel.calcularTiempoTotalParaActividades(esperaMinutos, tiempoTrayecto)
        } else {
            viewModel.calcularTiempoTotalParaActividades(0, 0)
        }
    }
}