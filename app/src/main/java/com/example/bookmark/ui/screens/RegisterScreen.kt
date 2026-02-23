package com.example.bookmark.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.supabase.Usuario
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit, // Navegar al inicio tras registrarse
    onNavigateBack: () -> Unit // Volver al login
) {
    // Estados para los campos de texto
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    // Usamos verticalScroll por si la pantalla del móvil es pequeña
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Crear Cuenta", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = nombre, onValueChange = { nombre = it },
            label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = apellidos, onValueChange = { apellidos = it },
            label = { Text("Apellidos") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = nickname, onValueChange = { nickname = it },
            label = { Text("Nickname") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = correo, onValueChange = { correo = it },
            label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = contrasena, onValueChange = { contrasena = it },
            label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            }
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (nombre.isNotBlank() && apellidos.isNotBlank() && nickname.isNotBlank() && correo.isNotBlank() && contrasena.isNotBlank()) {
                    isLoading = true
                    errorMessage = null

                    val nuevoUsuario = Usuario(
                        nombre = nombre,
                        apellidos = apellidos,
                        nickname = nickname,
                        correoElectronico = correo,
                        contrasena = contrasena,
                        perfilPublico = true // Por defecto lo hacemos público
                        // fotoPerfil y fotoBanner ya son null por defecto
                    )

                    coroutineScope.launch {
                        val resultado = authRepository.registrar(nuevoUsuario)
                        resultado.onSuccess {
                            isLoading = false
                            onRegisterSuccess()
                        }.onFailure { error ->
                            isLoading = false
                            errorMessage = "Error al registrar: ${error.message}"
                        }
                    }
                } else {
                    errorMessage = "Por favor, rellena todos los campos"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Registrarse")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}