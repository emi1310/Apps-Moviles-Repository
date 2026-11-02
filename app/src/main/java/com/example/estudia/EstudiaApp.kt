package com.example.estudia

import android.app.Application
import com.example.estudia.datos.RepositorioEstudiaImpl
import com.example.estudia.notificaciones.Notificaciones

// Clase Application: se crea una sola vez cuando arranca el proceso de la
// app, antes que cualquier pantalla. Funciona como "contenedor" de las
// dependencias compartidas (inyección de dependencias manual): el
// Repository vive acá y lo usan todos los ViewModels y los Workers.
class EstudiaApp : Application() {

    // by lazy: se crea la primera vez que alguien lo pide.
    val repositorio: RepositorioEstudiaImpl by lazy { RepositorioEstudiaImpl.crear(this) }

    override fun onCreate() {
        super.onCreate()
        // El canal tiene que existir antes de mostrar cualquier notificación.
        Notificaciones.crearCanal(this)
    }
}
