package com.example.application.reg_auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.rememberCoroutineScope
import com.example.application.MyApp
import com.example.application.data.network.NetworkConfig
import com.example.application.data.network.toUserMessage
import kotlinx.coroutines.launch
import android.util.Log

@Composable
fun RegistrationScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as MyApp
    val repo = app.appContainer.authRepository

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Регистрация")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Введите email и пароль"
                    return@Button
                }
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        repo.register(email.trim(), password)
                        onRegisterSuccess()
                    } catch (e: Throwable) {
                        Log.e("REG", "Register error: ${e::class.java.name}: ${e.message}", e)
                        Log.e("REG", "BASE_URL=${NetworkConfig.BASE_URL}")
                        errorMessage = toUserMessage(e)
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Row {
                if (isLoading) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("Зарегистрироваться")
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = errorMessage.orEmpty(), color = Color.Red)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val fullText = "Уже есть аккаунт? Войти"
        val linkStart = fullText.indexOf("Войти")
        val linkEnd = fullText.length
        val clickableText = buildAnnotatedString {
            append(fullText)
            if (linkStart >= 0) {
                addStyle(
                    style = SpanStyle(textDecoration = TextDecoration.Underline),
                    start = linkStart,
                    end = linkEnd
                )
            }
        }

        ClickableText(
            text = clickableText,
            onClick = { onNavigateToLogin() }
        )
    }
}

