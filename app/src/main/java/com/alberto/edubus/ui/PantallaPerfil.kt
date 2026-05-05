package com.alberto.edubus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alberto.edubus.viewmodel.NivelEduBus
import com.alberto.edubus.viewmodel.PerfilViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPerfil(
    onVolver: () -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val user = FirebaseAuth.getInstance().currentUser
    val correo = user?.email ?: "Usuario Invitado"

    val puntos by viewModel.puntosTotales.collectAsState()
    val nivel by viewModel.nivelActual.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Cabecera: Avatar y Título actual
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(nivel, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(correo, fontSize = 14.sp, color = Color.Gray)
                    Text("Total: $puntos XP", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Camino del Viajero",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lista dinámica de niveles
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(viewModel.listaNiveles) { nivelItem ->
                    ItemNivel(nivel = nivelItem, puntosUsuario = puntos)
                }
                item { Spacer(modifier = Modifier.height(24.dp)) } // Espacio al final
            }
        }
    }
}

@Composable
fun ItemNivel(nivel: NivelEduBus, puntosUsuario: Int) {
    val estaBloqueado = puntosUsuario < nivel.puntosMinimos
    val estaCompletado = puntosUsuario >= nivel.puntosNecesarios

    // Cálculo matemático del progreso de la barra (restringido entre 0.0 y 1.0)
    val progreso = when {
        estaCompletado -> 1f
        estaBloqueado -> 0f
        else -> {
            val puntosEnEsteNivel = puntosUsuario - nivel.puntosMinimos
            val rangoNivel = nivel.puntosNecesarios - nivel.puntosMinimos
            (puntosEnEsteNivel.toFloat() / rangoNivel.toFloat()).coerceIn(0f, 1f)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (estaBloqueado) Color.LightGray.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (estaBloqueado) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del candado o la estrella
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (estaBloqueado) Color.Gray else MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                if (estaBloqueado) {
                    Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = Color.White)
                } else {
                    Icon(Icons.Default.Star, contentDescription = "Desbloqueado", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nivel ${nivel.numero}: ${nivel.nombre}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (estaBloqueado) Color.Gray else MaterialTheme.colorScheme.onSurface
                )

                if (!estaBloqueado) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (estaCompletado) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                        trackColor = Color.LightGray
                    )
                    Text(
                        text = if (estaCompletado) "¡Completado!" else "${puntosUsuario}/${nivel.puntosNecesarios} XP",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                        color = if (estaCompletado) Color(0xFF2E7D32) else Color.DarkGray
                    )
                } else {
                    Text("Se desbloquea con ${nivel.puntosMinimos} XP", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}