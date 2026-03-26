package com.alberto.edubus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alberto.edubus.ui.PantallaLogin
import com.alberto.edubus.ui.PantallaPerfil
import com.alberto.edubus.ui.PantallaPortada
import com.alberto.edubus.ui.PantallaPrincipal
import com.alberto.edubus.ui.PantallaRegistro
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

    NavHost(navController = navController, startDestination = "portada") {
        composable("portada") {
            PantallaPortada(
                onNavegarLogin = { navController.navigate("login") },
                onNavegarRegistro = { navController.navigate("registro") },
                onLoginSuccess = {
                    navController.navigate("rutas") { popUpTo("portada") { inclusive = true } }
                }
            )
        }

        composable("login") {
            PantallaLogin(
                onLoginSuccess = {
                    navController.navigate("rutas") { popUpTo("portada") { inclusive = true } }
                },
                onNavegarRegistro = { navController.navigate("registro") },
                onVolver = { navController.popBackStack() }
            )
        }

        composable("registro") {
            PantallaRegistro(
                onRegistroSuccess = {
                    navController.navigate("rutas") { popUpTo("portada") { inclusive = true } }
                },
                onVolver = { navController.popBackStack() }
            )
        }

        composable("rutas") {
            PantallaPrincipal(
                onCerrarSesion = {
                    navController.navigate("portada") {
                        popUpTo(0)
                    }
                },
                onPerfil = { navController.navigate("perfil") }
            )
        }

        composable("perfil") {
            PantallaPerfil(onVolver = { navController.popBackStack() })
        }
    }
    }