package com.juku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.juku.app.data.repository.JukuRepository
import com.juku.app.ui.MainScreen
import com.juku.app.ui.theme.JukuAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: JukuRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = JukuRepository(applicationContext)

        setContent {
            JukuAppTheme {
                MainScreen(repository = repository)
            }
        }
    }
}
