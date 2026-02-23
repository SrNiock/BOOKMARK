package com.example.bookmark.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.bookmark.ui.supabase.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit, // Función que llamaremos cuando el login sea correcto (para cambiar de pantalla)
    onNavigateToRegister: () -> Unit // Función para ir a la pantalla de registro
) {
    // Estados para guardar lo que el usuario escribe
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    // Estados para manejar la carga y los errores
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Para lanzar la corrutina de login
    val coroutineScope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Iniciar Sesión", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(), // Oculta la contraseña con asteriscos
            modifier = Modifier.fillMaxWidth()
        )

        // Mostrar error si lo hay
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (correo.isNotBlank() && contrasena.isNotBlank()) {
                    isLoading = true
                    errorMessage = null

                    // Llamamos a Supabase
                    coroutineScope.launch {
                        val resultado = authRepository.login(correo, contrasena)

                        resultado.onSuccess { usuario ->
                            isLoading = false
                            // ¡Login exitoso! Podrías guardar el usuario en una variable global o DataStore
                            println("Bienvenido: ${usuario.nickname}")
                            onLoginSuccess()
                        }.onFailure { error ->
                            isLoading = false
                            errorMessage = error.message ?: "Error desconocido"
                        }
                    }
                } else {
                    errorMessage = "Por favor, llena todos los campos"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Deshabilita el botón mientras carga
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Entrar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
    }
}