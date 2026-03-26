package com.alberto.edubus.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthManager {
    private val auth: FirebaseAuth = Firebase.auth

    fun registrarUsuario(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Email y contraseña no pueden estar vacíos")
            return
        }

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess()
                else onError(task.exception?.message ?: "Error desconocido")
            }
    }

    fun iniciarSesion(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess()
                else onError(task.exception?.message ?: "Error al iniciar sesión")
            }
    }

    // NUEVA FUNCIÓN PARA GOOGLE
    fun loginConGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess()
                else onError(task.exception?.message ?: "Error al autenticar con Google")
            }
    }

    fun cerrarSesion(onSuccess: () -> Unit) {
        auth.signOut()
        onSuccess()
    }
}