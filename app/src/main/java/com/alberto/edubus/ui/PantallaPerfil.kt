package com.alberto.edubus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPerfil(onVolver: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sesión iniciada como:", fontSize = 14.sp)
            Text(text = user?.email ?: "Usuario invitado", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(32.dp))

            // Aquí puedes añadir más opciones como "Cambiar nombre" o "Ver estadísticas"
            OutlinedButton(onClick = { /* Lógica para borrar datos localmente si fuera necesario */ }) {
                Text("Borrar historial de búsqueda")
            }
        }
    }
}