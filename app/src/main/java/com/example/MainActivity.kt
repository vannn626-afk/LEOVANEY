package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.LeovaneyDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.leovaneyDataStore
import com.example.data.repository.LeovaneyRepository
import com.example.ui.LeovaneyApp
import com.example.ui.LeovaneyViewModel
import com.example.ui.theme.LeovaneyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = LeovaneyDatabase.getInstance(applicationContext)
        val preferencesRepository = UserPreferencesRepository(applicationContext.leovaneyDataStore)
        val repository = LeovaneyRepository(database, preferencesRepository)
        val viewModel = ViewModelProvider(
            this,
            LeovaneyViewModel.provideFactory(repository)
        )[LeovaneyViewModel::class.java]

        setContent {
            LeovaneyTheme {
                LeovaneyApp(viewModel = viewModel)
            }
        }
    }
}
