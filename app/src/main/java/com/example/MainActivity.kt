package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FinanceViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room local database and repositories
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AppRepository(database.transactionDao(), database.securityDao())
        
        // Instantiate ViewModel
        val viewModel: FinanceViewModel by viewModels {
            FinanceViewModelFactory(repository, applicationContext)
        }
        
        enableEdgeToEdge()
        
        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val accentColor by viewModel.accentColor.collectAsState()
            val fontScaleSetting by viewModel.fontScale.collectAsState()

            val fontScaleMultiplier = when(fontScaleSetting) {
                "Kecil" -> 0.85f
                "Besar" -> 1.15f
                else -> 1f
            }

            MyApplicationTheme(
                themeType = appTheme,
                accentType = accentColor,
                fontScaleMultiplier = fontScaleMultiplier
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val baseDensity = androidx.compose.ui.platform.LocalDensity.current
                    val customDensity = androidx.compose.ui.unit.Density(
                        density = baseDensity.density,
                        fontScale = baseDensity.fontScale * fontScaleMultiplier
                    )
                    
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalDensity provides customDensity
                    ) {
                        DashboardScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
