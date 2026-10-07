package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.navigation.DipuAppNavigation
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DipuViewModel
import com.example.ui.viewmodel.DipuViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: DipuViewModel by viewModels {
        val app = application as DipuProxyApplication
        DipuViewModelFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DipuAppNavigation(viewModel = viewModel)
            }
        }
    }
}
