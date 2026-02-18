package com.example.application.reg_auth

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.application.MyApp
import com.example.application.data.network.NetworkConfig
import com.example.application.data.network.toUserMessage
import kotlinx.coroutines.launch

@Composable
fun AuthorizationScreen(
    onNavigateToRegistration: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val app = context.applicationContext as MyApp
    val repo = app.appContainer.authRepository

    val textFieldShape = RoundedCornerShape(24.dp)
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFFC9C9C9),
        unfocusedBorderColor = Color(0xFFC9C9C9),
        focusedPlaceholderColor = Color(0xFF8E8E8E),
        unfocusedPlaceholderColor = Color(0xFF8E8E8E),
        focusedLeadingIconColor = Color(0xFF8E8E8E),
        unfocusedLeadingIconColor = Color(0xFF8E8E8E),
        focusedTrailingIconColor = Color(0xFF8E8E8E),
        unfocusedTrailingIconColor = Color(0xFF8E8E8E),
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Авторизация")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = textFieldShape,
            placeholder = { Text("Введите логин") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Login icon"
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = textFieldColors
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = textFieldShape,
            placeholder = { Text("Введите пароль") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Password icon"
                )
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = "Toggle password visibility"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = textFieldColors
        )

        Spacer(modifier = Modifier.height(24.dp))

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
                        repo.login(email.trim(), password)
                        onLoginSuccess()
                    } catch (e: Throwable) {
                        Log.e("AUTH", "Login error: ${e::class.java.name}: ${e.message}", e)
                        Log.e("AUTH", "BASE_URL=${NetworkConfig.BASE_URL}")
                        errorMessage = toUserMessage(e)
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4A4A4A),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFD9D9D9),
                disabledContentColor = Color(0xFF8E8E8E)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Войти")
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = errorMessage.orEmpty(), color = Color.Red)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val fullText = "Если у вас нет аккаунта, зарегистрируйтесь"
        val linkStart = fullText.indexOf("зарегистрируйтесь")
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
            style = TextStyle(color = Color(0xFF7A7A7A), fontSize = 13.sp),
            onClick = { onNavigateToRegistration() }
        )
    }
}
