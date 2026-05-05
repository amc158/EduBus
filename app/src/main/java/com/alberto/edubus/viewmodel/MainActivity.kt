package com.alberto.edubus.viewmodel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alberto.edubus.ui.*
import com.alberto.edubus.ui.theme.EduBusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EduBusTheme {
                NavegacionEduBus()
            }
        }
    }
}

@Composable
fun NavegacionEduBus() {
    val navController = rememberNavController()
    val busViewModel: BusViewModel = viewModel()

    val segundos by busViewModel.segundosRestantes.collectAsState()
    val origen by busViewModel.origenSeleccionado.collectAsState()
    val destino by busViewModel.destinoSeleccionado.collectAsState()

    LaunchedEffect(segundos) {
        if (segundos == 0 && origen != null && destino != null) {
            busViewModel.resetearViaje()

            navController.navigate("llegada") {
                // Borra TODA la pila de navegación. Pantalla limpia.
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "portada") {
        composable("portada") {
            PantallaPortada(
                onNavegarLogin = { navController.navigate("login") },
                onNavegarRegistro = { navController.navigate("registro") },
                onLoginSuccess = { navController.navigate("rutas") { popUpTo("portada") { inclusive = true } } }
            )
        }

        composable("login") {
            PantallaLogin(
                onLoginSuccess = { navController.navigate("rutas") { popUpTo("portada") { inclusive = true } } },
                onNavegarRegistro = { navController.navigate("registro") },
                onVolver = { navController.popBackStack() }
            )
        }

        composable("registro") {
            PantallaRegistro(
                onRegistroSuccess = { navController.navigate("rutas") { popUpTo("portada") { inclusive = true } } },
                onVolver = { navController.popBackStack() }
            )
        }

        composable("rutas") {
            PantallaPrincipal(
                viewModel = busViewModel,
                onCerrarSesion = { navController.navigate("portada") { popUpTo(0) } },
                onPerfil = { navController.navigate("perfil") },
                onJugarTrivia = { navController.navigate("trivia") },
                onJugarMinijuego = { navController.navigate("minijuegos") },
                onEscucharPodcast = { navController.navigate("seleccion_podcast") }
            )
        }

        composable("llegada") {
            PantallaLlegada(
                onFinalizar = {
                    navController.navigate("rutas") {
                        // Al darle a "Entendido", reconstruye la app limpia
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("trivia") {
            PantallaTrivia(
                viewModel = viewModel(),
                busViewModel = busViewModel,
                onVolver = { navController.popBackStack() }
            )
        }

        composable("minijuegos") {
            PantallaMinijuegos(
                busViewModel = busViewModel,
                onVolver = { navController.popBackStack() }
            )
        }

        composable("seleccion_podcast") {
            PantallaSeleccionPodcast(
                onTemaSeleccionado = { temaId ->
                    val tiempoExacto = busViewModel.segundosRestantes.value
                    navController.navigate("podcast/$temaId/$tiempoExacto")
                },
                onVolver = { navController.popBackStack() }
            )
        }

        composable("podcast/{temaId}/{segundos}") { backStackEntry ->
            val temaId = backStackEntry.arguments?.getString("temaId") ?: "actualidad"
            val segundosStr = backStackEntry.arguments?.getString("segundos") ?: "120"
            val segundosExactosAlClic = segundosStr.toIntOrNull() ?: 120

            val podcastViewModel: PodcastViewModel = viewModel()

            LaunchedEffect(temaId) {
                val tiempoGeneracionAproximado = 15
                val segundosParaLaIA = maxOf(30, segundosExactosAlClic - tiempoGeneracionAproximado)
                podcastViewModel.crearPodcastAMedida(temaId, segundosParaLaIA)
            }

            PantallaPodcast(
                busViewModel = busViewModel,
                podcastViewModel = podcastViewModel,
                onVolver = { navController.popBackStack() }
            )
        }

        composable("perfil") {
            PantallaPerfil(onVolver = { navController.popBackStack() })
        }
    }
}