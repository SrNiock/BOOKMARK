package com.example.bookmark.ui.screens

import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit, // Navega a la pantalla principal
    onNavigateToRegister: () -> Unit // Navega a la pantalla de registro
) {
    // Estados para guardar lo que el usuario escribe
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    // Estado para controlar si la contraseña se ve o no (el "ojito")
    var passwordVisible by remember { mutableStateOf(false) }

    // Estados para manejar la carga y los errores
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Para lanzar la corrutina de login
    val coroutineScope = rememberCoroutineScope()
    // Instancia de tu repositorio
    val authRepository = remember { AuthRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título
        Text(text = "Iniciar Sesión", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(32.dp))

        // Campo: Correo Electrónico
        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo: Contraseña (con el botón del ojito)
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            // Si passwordVisible es true, muestra el texto. Si es false, pone asteriscos.
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            // Aquí añadimos el icono al final del campo
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )

        // Mensaje de error (solo aparece si hay un error)
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón principal de Entrar
        Button(
            onClick = {
                if (correo.isNotBlank() && contrasena.isNotBlank()) {
                    isLoading = true
                    errorMessage = null

                    // Llamamos a Supabase en segundo plano
                    coroutineScope.launch {
                        val resultado = authRepository.login(correo, contrasena)

                        resultado.onSuccess { usuario ->
                            isLoading = false
                            // ¡Login exitoso! Saltamos a la pantalla de Libros
                            onLoginSuccess()
                        }.onFailure { error ->
                            isLoading = false
                            // Mostramos el error si el usuario no existe o las credenciales están mal
                            errorMessage = "Correo o contraseña incorrectos"
                        }
                    }
                } else {
                    errorMessage = "Por favor, llena todos los campos"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Deshabilita el botón para que no hagan doble clic mientras carga
        ) {
            if (isLoading) {
                // Muestra un circulito de carga si está comprobando los datos
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Entrar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para ir a registrarse
        TextButton(onClick = onNavigateToRegister) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
    }
}