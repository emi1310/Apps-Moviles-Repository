package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.estudia.BuildConfig
import com.example.estudia.util.leerJpeg
import com.example.estudia.util.leerBytes
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
        initializer { InicioViewModel(estudiaApp().repositorio, avisosSistema = estudiaApp().avisos) }
        initializer { MateriasViewModel(estudiaApp().repositorio, estudiaApp().avisos) }
        initializer { CalendarioViewModel(estudiaApp().repositorio, estudiaApp().avisos) }
        initializer { NotasViewModel(estudiaApp().repositorio) }
        initializer { PerfilViewModel(estudiaApp().repositorio, estudiaApp().avisos) }
        initializer { TemporizadorViewModel(estudiaApp().repositorio, estudiaApp().avisos) }
        initializer { EstadisticasViewModel(estudiaApp().repositorio) }
        initializer { HorarioViewModel(estudiaApp().repositorio) }
        initializer { LogrosViewModel(estudiaApp().repositorio) }
        initializer { CarreraViewModel(estudiaApp().repositorio) }
        initializer { AsistenciaViewModel(estudiaApp().repositorio) }
        initializer { MatrizViewModel(estudiaApp().repositorio, estudiaApp().avisos) }
        initializer { FinalesViewModel(estudiaApp().repositorio) }
        initializer { BibliografiaViewModel(estudiaApp().repositorio) }
        initializer { MapasViewModel(estudiaApp().repositorio) }
        initializer {
            val app = estudiaApp()
            AsistenteViewModel(
                app.repositorio, app.asistente, app.clavesIA, BuildConfig.GEMINI_API_KEY,
                avisosSistema = app.avisos,
                leerFoto = { uri -> leerJpeg(app, android.net.Uri.parse(uri)) },
                leerArchivo = { uri -> leerBytes(app, android.net.Uri.parse(uri), com.example.estudia.modelo.TAMANO_MAXIMO_PDF) },
                historial = app.historialIA,
                prefijoPlan = app.getString(com.example.estudia.R.string.ia_plan_prefijo)
            )
        }
        initializer { MapaViewModel(estudiaApp().repositorio, createSavedStateHandle().get<Int>("mapaId") ?: 0) }
        initializer { ResumenSemanalViewModel(estudiaApp().repositorio) }
        initializer { BusquedaViewModel(estudiaApp().repositorio) }
        initializer {
            NotaNecesariaViewModel(estudiaApp().repositorio, createSavedStateHandle().get<Int>("materiaId") ?: TODAS_LAS_MATERIAS)
        }
        initializer {
            QuizViewModel(estudiaApp().repositorio, createSavedStateHandle().get<Int>("materiaId") ?: TODAS_LAS_MATERIAS)
        }
        initializer {
            ParesViewModel(estudiaApp().repositorio, createSavedStateHandle().get<Int>("materiaId") ?: TODAS_LAS_MATERIAS)
        }
        // El id de la materia llega por la ruta de navegación (SavedStateHandle).
        initializer {
            RepasoViewModel(estudiaApp().repositorio, createSavedStateHandle().get<Int>("materiaId") ?: TODAS_LAS_MATERIAS)
        }
    }
}

private fun CreationExtras.estudiaApp(): EstudiaApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EstudiaApp
