package com.alberto.edubus.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TemaPodcast(val id: String, val titulo: String, val descripcion: String, val icono: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSeleccionPodcast(
    onTemaSeleccionado: (String) -> Unit,
    onVolver: () -> Unit
) {
    val temas = listOf(
        TemaPodcast("actualidad", "Noticias de actualidad", "Lo más importante de hoy.", Icons.Default.Newspaper),
        TemaPodcast("cultura", "Cultura y Curiosidades", "Descubre la historia oculta de nuestras calles.", Icons.Default.Museum),
        TemaPodcast("sostenibilidad", "Mundo Verde", "Consejos para cuidar nuestro planeta.", Icons.Default.Eco)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elige tu Podcast IA", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            item {
                Text(
                    text = "Generados hoy por Inteligencia Artificial",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(temas) { tema ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        .clickable { onTemaSeleccionado(tema.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tema.icono,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = tema.titulo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = tema.descripcion, color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}