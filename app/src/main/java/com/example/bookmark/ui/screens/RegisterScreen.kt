package com.example.bookmark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookmark.ui.supaBase.AuthRepository
import com.example.bookmark.ui.supaBase.Usuario
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

    // Colores dinámicos para los campos de texto
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        focusedBorderColor = MaterialTheme.colorScheme.primary, // Borde Naranja al tocar
        unfocusedBorderColor = Color.DarkGray, // Borde gris en reposo
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = Color.Gray
    )

    // Usamos verticalScroll por si la pantalla del móvil es pequeña
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Fondo Negro profundo
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título de la pantalla
        Text(
            text = "Crear Cuenta",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Únete para guardar tu progreso",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = nombre, onValueChange = { nombre = it },
            label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apellidos, onValueChange = { apellidos = it },
            label = { Text("Apellidos") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nickname, onValueChange = { nickname = it },
            label = { Text("Nickname") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = correo, onValueChange = { correo = it.trim() },
            label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = contrasena, onValueChange = { contrasena = it },
            label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp), colors = textFieldColors,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = image,
                        contentDescription = description,
                        tint = if (passwordVisible) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
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
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary, // Naranja
                contentColor = MaterialTheme.colorScheme.onPrimary // Negro
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 3.dp
                )
            } else {
                Text(
                    "REGISTRARSE",
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onNavigateBack) {
            Text(
                text = "¿Ya tienes cuenta? Inicia sesión",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}