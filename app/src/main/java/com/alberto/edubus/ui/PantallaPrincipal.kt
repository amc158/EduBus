package com.alberto.edubus.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.viewinterop.AndroidView
import com.alberto.edubus.viewmodel.BusViewModel
import com.alberto.edubus.auth.AuthManager
import com.alberto.edubus.data.BusRepository
import com.alberto.edubus.model.Parada
import com.alberto.edubus.model.RutaBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    viewModel: BusViewModel,
    onCerrarSesion: () -> Unit,
    onPerfil: () -> Unit,
    onJugarTrivia: (Int) -> Unit,
    onJugarMinijuego: (Int) -> Unit,
    onEscucharPodcast: (Int) -> Unit
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
                    label = { Text("Mi Perfil y Progreso") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onPerfil() }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
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
            ContenidoRutas(
                viewModel = viewModel,
                modifier = Modifier.padding(paddingValues),
                onJugarTrivia = onJugarTrivia,
                onJugarMinijuego = onJugarMinijuego,
                onEscucharPodcast = onEscucharPodcast
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContenidoRutas(
    viewModel: BusViewModel,
    modifier: Modifier = Modifier,
    onJugarTrivia: (Int) -> Unit,
    onJugarMinijuego: (Int) -> Unit,
    onEscucharPodcast: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val origenSeleccionado by viewModel.origenSeleccionado.collectAsState()
    val destinoSeleccionado by viewModel.destinoSeleccionado.collectAsState()
    val segundosRestantes by viewModel.segundosRestantes.collectAsState()

    var rutaDatos by remember { mutableStateOf<RutaBus?>(null) }
    var lineasExpandidas by remember { mutableStateOf(mapOf<String, Boolean>()) }

    var sentidoIda by remember { mutableStateOf(true) }
    var expandidoOrigen by remember { mutableStateOf(false) }
    var expandidoDestino by remember { mutableStateOf(false) }

    var buscandoTiempoReal by remember { mutableStateOf(false) }
    var idParadaABuscar by remember { mutableStateOf<String?>(null) }

    val minutosReloj = segundosRestantes / 60
    val segundosReloj = segundosRestantes % 60
    val locale = LocalConfiguration.current.locales[0]

    val textoCuentaAtras = remember(minutosReloj, segundosReloj, locale) {
        String.format(locale, "%02d:%02d", minutosReloj, segundosReloj)
    }

    var mostrarHub by remember { mutableStateOf(false) }
    val hubSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            item {
                if (origenSeleccionado != null && destinoSeleccionado != null && segundosRestantes > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (buscandoTiempoReal) "Actualizando tiempo real..." else "Tu viaje está en marcha",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (buscandoTiempoReal) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Aprox. $textoCuentaAtras",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (segundosRestantes < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Text("Tiempo estimado de llegada", fontSize = 12.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${origenSeleccionado?.nombre} ➔ ${destinoSeleccionado?.nombre}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = {
                                    buscandoTiempoReal = false
                                    viewModel.resetearViaje()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cambiar trayecto")
                            }
                        }
                    }

                    Button(
                        onClick = { mostrarHub = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("¡Empezar Actividad Educativa!", fontWeight = FontWeight.Bold)
                    }

                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("¿A dónde vamos hoy?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            TabRow(selectedTabIndex = if (sentidoIda) 0 else 1, containerColor = Color.Transparent) {
                                Tab(selected = sentidoIda, onClick = { sentidoIda = true; viewModel.resetearViaje() }, text = { Text("Ida") })
                                Tab(selected = !sentidoIda, onClick = { sentidoIda = false; viewModel.resetearViaje() }, text = { Text("Vuelta") })
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Selector Origen
                            ExposedDropdownMenuBox(
                                expanded = expandidoOrigen,
                                onExpandedChange = { expandidoOrigen = it }
                            ) {
                                OutlinedTextField(
                                    value = origenSeleccionado?.nombre ?: "Selecciona tu parada",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Estoy en...") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoOrigen) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = expandidoOrigen, onDismissRequest = { expandidoOrigen = false }) {
                                    paradasActuales.forEach { parada ->
                                        DropdownMenuItem(
                                            text = { Text(parada.nombre) },
                                            onClick = {
                                                viewModel.establecerParadas(parada, destinoSeleccionado)
                                                expandidoOrigen = false

                                                // LÓGICA: Si el destino queda atrás respecto al nuevo origen, reseteamos el destino
                                                if (destinoSeleccionado != null) {
                                                    val indiceOrigen = paradasActuales.indexOf(parada)
                                                    val indiceDestino = paradasActuales.indexOf(destinoSeleccionado)
                                                    if (indiceDestino <= indiceOrigen) {
                                                        viewModel.establecerParadas(parada, null)
                                                    }
                                                }

                                                actualizarTiempos(origenSeleccionado, destinoSeleccionado, horariosActuales, viewModel)

                                                if (destinoSeleccionado != null) {
                                                    buscandoTiempoReal = true
                                                    idParadaABuscar = parada.idParada
                                                    scope.launch {
                                                        delay(15000)
                                                        buscandoTiempoReal = false
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Selector Destino
                            ExposedDropdownMenuBox(
                                expanded = expandidoDestino,
                                onExpandedChange = { if (origenSeleccionado != null) expandidoDestino = it }
                            ) {
                                OutlinedTextField(
                                    value = destinoSeleccionado?.nombre ?: "Selecciona destino",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Me bajo en...") },
                                    enabled = origenSeleccionado != null, // Bloqueado hasta elegir origen
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandidoDestino) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = expandidoDestino, onDismissRequest = { expandidoDestino = false }) {
                                    // LÓGICA: Solo mostramos paradas posteriores al origen
                                    val indiceOrigen = paradasActuales.indexOf(origenSeleccionado)
                                    val paradasValidas = paradasActuales.filterIndexed { index, _ -> index > indiceOrigen }

                                    paradasValidas.forEach { parada ->
                                        DropdownMenuItem(
                                            text = { Text(parada.nombre) },
                                            onClick = {
                                                viewModel.establecerParadas(origenSeleccionado, parada)
                                                expandidoDestino = false

                                                actualizarTiempos(origenSeleccionado, destinoSeleccionado, horariosActuales, viewModel)

                                                if (origenSeleccionado != null) {
                                                    buscandoTiempoReal = true
                                                    idParadaABuscar = origenSeleccionado!!.idParada
                                                    scope.launch {
                                                        delay(15000)
                                                        buscandoTiempoReal = false
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Detalle de la ruta:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                // LÓGICA: Pasamos 'sentidoIda' para que el detalle sepa qué mostrar
                ItemLineaBus(
                    ruta = rutaDatos!!,
                    expandido = lineasExpandidas[rutaDatos!!.lineaId] == true,
                    sentidoIda = sentidoIda,
                    onClick = {
                        val id = rutaDatos!!.lineaId
                        val actual = lineasExpandidas[id] ?: false
                        lineasExpandidas = lineasExpandidas.toMutableMap().apply { put(id, !actual) }
                    }
                )
            }
        }

        if (buscandoTiempoReal && idParadaABuscar != null) {
            BuscadorTiempoRealInvisible(
                idParada = idParadaABuscar!!,
                idSurbusFiltro = rutaDatos!!.idSurbusFiltro,
                onResultado = { minutos ->
                    if (buscandoTiempoReal) {
                        buscandoTiempoReal = false
                        actualizarTiempos(origenSeleccionado, destinoSeleccionado, horariosActuales, viewModel, tiempoRealWeb = minutos)
                    }
                }
            )
        }

        if (mostrarHub) {
            ModalBottomSheet(onDismissRequest = { mostrarHub = false }, sheetState = hubSheetState) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val tiempoEnMinutos = segundosRestantes / 60
                    Text("Opciones para tu trayecto", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Tiempo restante: $tiempoEnMinutos min", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { mostrarHub = false; onJugarTrivia(tiempoEnMinutos) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp)) {
                        Text("🧠 Jugar Trivia Educativa (Gana XP)")
                    }
                    Button(onClick = { mostrarHub = false; onJugarMinijuego(tiempoEnMinutos) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                        Text("🎮 Minijuegos 100% Offline")
                    }
                    Button(onClick = { mostrarHub = false; onEscucharPodcast(tiempoEnMinutos) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text("🎧 Escuchar Podcast IA", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun BuscadorTiempoRealInvisible(idParada: String, idSurbusFiltro: String, onResultado: (Int?) -> Unit) {
    AndroidView(factory = { ctx ->
        @Suppress("SetJavaScriptEnabled")
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    postDelayed({
                        evaluateJavascript("(function() { var el = document.querySelector('span[id^=\"waitTime$idSurbusFiltro\"]'); return el ? el.innerText.trim() : null; })()") { valor ->
                            val limpio = valor.replace("\"", "")
                            val minutos = Regex("\\d+").find(limpio)?.value?.toIntOrNull()
                            onResultado(minutos)
                        }
                    }, 2500)
                }
            }
            loadUrl("https://www.surbusalmeria.es/tiempos-de-espera/parada/$idParada")
        }
    }, modifier = Modifier.size(0.dp))
}

fun actualizarTiempos(
    origen: Parada?,
    destino: Parada?,
    horariosSalida: List<String>?,
    viewModel: BusViewModel,
    tiempoRealWeb: Int? = null
) {
    if (origen != null && destino != null) {
        val tiempoTrayectoOficial = destino.tiempoAcumulado - origen.tiempoAcumulado
        if (tiempoTrayectoOficial > 0) {
            val minutosEspera = if (tiempoRealWeb != null) {
                tiempoRealWeb
            } else {
                val textoEspera = calcularTiempoEstimadoOffline(origen.tiempoAcumulado, horariosSalida)
                Regex("\\d+").find(textoEspera)?.value?.toInt() ?: 0
            }
            viewModel.calcularTiempoTotalParaActividades(minutosEspera, tiempoTrayectoOficial)
        } else {
            viewModel.calcularTiempoTotalParaActividades(0, 0)
        }
    }
}

@Composable
fun ItemLineaBus(ruta: RutaBus, expandido: Boolean, sentidoIda: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = ruta.nombreComercial, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            AnimatedVisibility(visible = expandido) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // LÓGICA: Filtramos el detalle de la ruta según la pestaña elegida (Ida o Vuelta)
                    if (sentidoIda) {
                        Text("PARADAS DE IDA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        ruta.paradasIda.forEach { ItemParada(it, ruta.idSurbusFiltro, ruta.lineaId, ruta.obtenerHorariosActuales(true)) }
                    } else {
                        Text("PARADAS DE VUELTA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        ruta.paradasVuelta.forEach { ItemParada(it, ruta.idSurbusFiltro, ruta.lineaId, ruta.obtenerHorariosActuales(false)) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ItemParada(parada: Parada, idSurbusFiltro: String, nombreCorto: String, horariosSalida: List<String>?) {
    var mostrarDialogoTiempo by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "• ${parada.nombre}", modifier = Modifier.weight(1f))
        TextButton(onClick = { mostrarDialogoTiempo = true }) { Text("Tiempo Real") }
    }
    if (mostrarDialogoTiempo) {
        DialogoTiempoReal(parada, idSurbusFiltro, nombreCorto, horariosSalida, onDismiss = { mostrarDialogoTiempo = false })
    }
}

@Composable
fun DialogoTiempoReal(parada: Parada, idSurbusFiltro: String, nombreLinea: String, horariosSalida: List<String>?, onDismiss: () -> Unit) {
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
            origenDato = "Modo Offline (Predicción)"
            cargando = false
        }
    }

    key(refrescarKey) {
        Box(modifier = Modifier.size(0.dp)) {
            AndroidView(factory = { ctx ->
                @Suppress("SetJavaScriptEnabled")
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            postDelayed({
                                evaluateJavascript("(function() { var el = document.querySelector('span[id^=\"waitTime$idSurbusFiltro\"]'); return el ? el.innerText.trim() : null; })()") { valor ->
                                    if (!cargando) return@evaluateJavascript
                                    val limpio = valor.replace("\"", "")
                                    if (limpio != "null" && limpio.isNotBlank()) {
                                        tiempoRestante = limpio
                                        origenDato = "Tiempo Real (Surbus GPS)"
                                    } else {
                                        tiempoRestante = tiempoOffline
                                        origenDato = "Modo Offline (Predicción)"
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
        title = { Text("Línea $nombreLinea", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Parada: ${parada.nombre}", color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                if (cargando) {
                    CircularProgressIndicator()
                } else {
                    Text(text = tiempoRestante, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = if (origenDato.contains("Offline")) Color.Gray else Color(0xFF2E7D32))
                    Text(text = origenDato, fontSize = 12.sp, color = Color.Gray)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

fun calcularTiempoEstimadoOffline(tiempoAcumulado: Int, horariosSalidaCabecera: List<String>?): String {
    if (horariosSalidaCabecera.isNullOrEmpty()) return "Horarios no disponibles"
    val ahora = LocalTime.now()
    val ahoraMinutos = if (ahora.hour < 4) (ahora.hour * 60) + ahora.minute + 1440 else (ahora.hour * 60) + ahora.minute
    var menorTiempoEspera = Int.MAX_VALUE
    for (horaString in horariosSalidaCabecera) {
        try {
            val match = Regex("(\\d{1,2})[:.-](\\d{2})").find(horaString)
            if (match != null) {
                val (h, m) = match.destructured
                var salidaMin = (h.toInt() * 60) + m.toInt()
                if (salidaMin < 4 * 60) salidaMin += 1440
                val llegadaMin = salidaMin + tiempoAcumulado
                val restantes = llegadaMin - ahoraMinutos
                if (restantes in 0..120 && restantes < menorTiempoEspera) menorTiempoEspera = restantes
            }
        } catch (e: Exception) { continue }
    }
    return if (menorTiempoEspera != Int.MAX_VALUE) "Aprox. $menorTiempoEspera min" else "Fin del servicio"
}