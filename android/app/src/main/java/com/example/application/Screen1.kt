package com.example.application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun Screen1 ( ) {
    Text(
        modifier = Modifier.fillMaxSize().wrapContentHeight(),
        text = "Screen1",
        textAlign = TextAlign.Center
    )
}