package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.estudia.EstudiaApp

// Factory de todos los ViewModels de la app: le dice a Android cómo crear
// cada uno, pasándole el Repository por constructor (ViewModelProvider no
// sabe hacerlo solo). El Repository sale de EstudiaApp.
//
// Uso desde un Composable: viewModel<MateriasViewModel>(factory = AppViewModelProvider.Factory)
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { SesionViewModel(estudiaApp().repositorio) }
        initializer { InicioViewModel(estudiaApp().repositorio) }
        initializer { MateriasViewModel(estudiaApp().repositorio) }
        initializer { CalendarioViewModel(estudiaApp().repositorio) }
        initializer { NotasViewModel(estudiaApp().repositorio) }
        initializer { PerfilViewModel(estudiaApp().repositorio) }
        initializer { TemporizadorViewModel(estudiaApp().repositorio) }
    }
}

private fun CreationExtras.estudiaApp(): EstudiaApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EstudiaApp
