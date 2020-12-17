package com.klitsie.progressbutton

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.klitsie.progressbutton.ui.ProgressButtonTheme
import com.klitsie.progressbutton.ui.countDownBorder
import com.klitsie.progressbutton.ui.progressBorder
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProgressButtonTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Buttons()
                }
            }
        }
    }
}

@Composable
fun Buttons() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var isLoading by remember { mutableStateOf(false) }
        if (isLoading) {
            LaunchedEffect(subject = isLoading) {
                delay(7000)
                isLoading = false
            }
        }
        OutlinedButton(
            enabled = !isLoading,
            onClick = {
                isLoading = (!isLoading)
            },
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
            ),
            modifier = Modifier.progressBorder(
                isVisible = isLoading,
                shape = MaterialTheme.shapes.small
            )
        ) {
            Text(text = "Indeterminate")
        }
        Spacer(modifier = Modifier.height(16.dp))
        var isCountingDown by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = {
                isCountingDown = !isCountingDown
            },
            modifier = Modifier.countDownBorder(
                duration = 5000,
                isVisible = isCountingDown,
                shape = MaterialTheme.shapes.small,
            ) {
                isCountingDown = false
            }
        ) {
            Text(text = "Countdown")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ProgressButtonTheme {
        Buttons()
    }
}