package com.example.estudia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.estudia.datos.RepositorioDatos
import com.example.estudia.datos.SesionDataStore
import com.example.estudia.ui.navegacion.AppNavegacion
import com.example.estudia.ui.theme.EstudiaTheme
import com.example.estudia.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: AppViewModel = viewModel()
            viewModel.iniciar(RepositorioDatos(filesDir))

            val sesionDataStore = remember { SesionDataStore(applicationContext) }

            EstudiaTheme {
                AppNavegacion(viewModel, sesionDataStore)
            }
        }
    }
}
