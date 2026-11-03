package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.estudia.EstudiaApp

// Factory de todos los ViewModels de la app: le dice a Android cómo crear
// cada uno, pasándole el Repository (y AvisosSistema, a los que programan
// recordatorios o usan el temporizador) por constructor: ViewModelProvider
// no sabe hacerlo solo. Los dos salen de EstudiaApp.
//
// Uso desde un Composable: viewModel<MateriasViewModel>(factory = AppViewModelProvider.Factory)
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { SesionViewModel(estudiaApp().repositorio, estudiaApp().avisos) }
        initializer { InicioViewModel(estudiaApp().repositorio) }
        initializer { MateriasViewModel(estudiaApp().repositorio, estudiaApp().avisos) }
        initializer { CalendarioViewModel(estudiaApp().repositorio, estudiaApp().avisos) }
        initializer { NotasViewModel(estudiaApp().repositorio) }
        initializer { PerfilViewModel(estudiaApp().repositorio, estudiaApp().avisos) }
        initializer { TemporizadorViewModel(estudiaApp().repositorio, estudiaApp().avisos) }
        initializer { EstadisticasViewModel(estudiaApp().repositorio) }
    }
}

private fun CreationExtras.estudiaApp(): EstudiaApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EstudiaApp
