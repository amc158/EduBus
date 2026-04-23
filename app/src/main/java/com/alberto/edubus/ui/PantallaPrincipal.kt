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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.alberto.edubus.auth.AuthManager
import com.alberto.edubus.data.BusRepository
import com.alberto.edubus.model.Parada
import com.alberto.edubus.model.RutaBus
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    onCerrarSesion: () -> Unit,
    onPerfil: () -> Unit
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
            ContenidoRutas(Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun ContenidoRutas(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var rutaDatos by remember { mutableStateOf<RutaBus?>(null) }
    var lineasExpandidas by remember { mutableStateOf(mapOf<String, Boolean>()) }

    LaunchedEffect(Unit) {
        val repo = BusRepository(context)
        rutaDatos = repo.obtenerRuta()
    }

    if (rutaDatos == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
            item {
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
                    ruta.paradasIda.forEach { parada ->
                        ItemParada(parada, ruta.idSurbusFiltro, ruta.lineaId, ruta.horariosIda)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("VUELTA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    ruta.paradasVuelta.forEach { parada ->
                        ItemParada(parada, ruta.idSurbusFiltro, ruta.lineaId, ruta.horariosVuelta)
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
    // Calculamos el tiempo offline al momento de abrir el diálogo
    val tiempoOffline = remember { calcularTiempoEstimadoOffline(parada.tiempoAcumulado, horariosSalida) }

    // Mostramos la predicción offline mientras se carga el dato real
    var tiempoRestante by remember { mutableStateOf(tiempoOffline) }
    var cargando by remember { mutableStateOf(true) }
    var refrescarKey by remember { mutableIntStateOf(0) }

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
                                    val limpio = valor.replace("\"", "")
                                    // Si Surbus nos da datos válidos, sobrescribimos la estimación offline
                                    if (limpio != "null" && limpio.isNotBlank() && limpio != "No disponible" && limpio != "Sin buses próximos") {
                                        tiempoRestante = limpio
                                    } else {
                                        // Si falla, mantenemos la estimación offline o un mensaje de error si no la había
                                        if (tiempoRestante == "Actualizando...") {
                                            tiempoRestante = tiempoOffline
                                        }
                                    }
                                    cargando = false
                                }
                            }, 3500)
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
                        tiempoRestante = "Actualizando..."
                        refrescarKey++
                    }) {
                        Icon(Icons.Default.Autorenew, contentDescription = "Refrescar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Parada: ${parada.nombre}", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))

                if (cargando) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = tiempoRestante,
                    fontSize = if (tiempoRestante.contains("Aprox.")) 22.sp else 36.sp, // Letra un poco más pequeña si es la predicción
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        tiempoRestante.contains("Aprox.") -> Color.Gray
                        tiempoRestante.contains("min") -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
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

    for (horaString in horariosSalidaCabecera) {
        try {
            val horaSalida = LocalTime.parse(horaString, formatter)
            val horaLlegadaEstimada = horaSalida.plusMinutes(tiempoAcumulado.toLong())

            if (horaLlegadaEstimada.isAfter(ahora)) {
                val minutosRestantes = ChronoUnit.MINUTES.between(ahora, horaLlegadaEstimada)
                return "Aprox. $minutosRestantes min (Offline)"
            }
        } catch (e: Exception) {
            continue
        }
    }

    return "Fin del servicio por hoy"
}