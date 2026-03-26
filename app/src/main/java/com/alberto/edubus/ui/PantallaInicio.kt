package com.alberto.edubus.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// IMPORT DE COIL (NUEVO)
import coil.compose.AsyncImage
import com.alberto.edubus.R
import com.alberto.edubus.auth.AuthManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PantallaPortada(
    onNavegarLogin: () -> Unit,
    onNavegarRegistro: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager() }
    var mensajeError by remember { mutableStateOf("") }

    var cuentaRecordada by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    var isLoadingSilently by remember { mutableStateOf(true) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    LaunchedEffect(Unit) {
        googleSignInClient.silentSignIn().addOnCompleteListener { task ->
            if (task.isSuccessful) cuentaRecordada = task.result
            else cuentaRecordada = null
            isLoadingSilently = false
        }
    }

    val launcherGoogle = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                authManager.loginConGoogle(token, onSuccess = onLoginSuccess, onError = { mensajeError = it })
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501) mensajeError = "Error de conexión con Google: ${e.statusCode}"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.edubus_logo),
            contentDescription = "Logo EduBus",
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNavegarLogin,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("INICIAR SESIÓN", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onNavegarRegistro, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("CREAR CUENTA", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("O", color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        // --- BOTÓN DE GOOGLE CON CLIC LARGO ---
        if (!isLoadingSilently) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(50)) // Le damos forma de píldora
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50)) // Borde como el OutlinedButton
                    .combinedClickable(
                        onClick = {
                            // CLIC NORMAL: Intenta usar la cuenta recordada
                            launcherGoogle.launch(googleSignInClient.signInIntent)
                        },
                        onLongClick = {
                            // MANTENER PULSADO: Olvida la cuenta y pide elegir una nueva
                            Toast.makeText(context, "Cambiando de cuenta...", Toast.LENGTH_SHORT).show()

                            googleSignInClient.signOut().addOnCompleteListener {
                                // Al terminar de borrar, lanzamos la ventana de selección
                                launcherGoogle.launch(googleSignInClient.signInIntent)
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    if (cuentaRecordada?.photoUrl != null) {
                        AsyncImage(
                            model = cuentaRecordada?.photoUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.size(24.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_google_logo)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = if (cuentaRecordada != null) "CONTINUAR COMO ${cuentaRecordada?.displayName?.uppercase()}" else "CONTINUAR CON GOOGLE",
                        color = Color.DarkGray,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }

        if (mensajeError.isNotEmpty()) {
            Text(text = mensajeError, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
fun PantallaLogin(
    onLoginSuccess: () -> Unit,
    onNavegarRegistro: () -> Unit, // Ruta de emergencia si falla
    onVolver: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
    var mostrarBotonRegistro by remember { mutableStateOf(false) }

    val authManager = remember { AuthManager() }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Iniciar Sesión", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

        if (mensajeError.isNotEmpty()) {
            Text(text = mensajeError, color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                authManager.iniciarSesion(email, password,
                    onSuccess = onLoginSuccess,
                    onError = {
                        mensajeError = "Usuario no encontrado o contraseña incorrecta."
                        mostrarBotonRegistro = true // Activa el botón de registro
                    }
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("ENTRAR")
        }

        if (mostrarBotonRegistro) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onNavegarRegistro, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onVolver) {
            Text("Volver a la portada")
        }
    }
}

@Composable
fun PantallaRegistro(
    onRegistroSuccess: () -> Unit, // Te lleva directo a la app
    onVolver: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
    val authManager = remember { AuthManager() }

    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Crear Cuenta", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

        if (mensajeError.isNotEmpty()) {
            Text(text = mensajeError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            authManager.registrarUsuario(email, pass,
                onSuccess = onRegistroSuccess,
                onError = { mensajeError = it }
            )
        }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("REGISTRARSE")
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onVolver) { Text("Cancelar") }
    }
}