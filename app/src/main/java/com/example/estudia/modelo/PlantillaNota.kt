package com.example.estudia.modelo

// Plantillas para armar una nota de estudio con estructura (síntesis
// visual): el texto de cada una está en strings.xml (plantilla_*).
//  - Cornell: ideas clave, desarrollo, preguntas y síntesis.
//  - Esquema: temas y subtemas numerados.
//  - Cuadro comparativo: dos conceptos punto por punto.
//  - Línea de tiempo: hechos en orden con por qué importan.
//  - Resumen de clase: lo importante, ejemplos, dudas y lo de la próxima.
//  - Guía de ejercicios: una casilla por ejercicio para ir marcándolos.
enum class PlantillaNota { LIBRE, RESUMEN_CLASE, CORNELL, ESQUEMA, COMPARATIVO, LINEA_TIEMPO, GUIA_EJERCICIOS }

// Pone la plantilla en el contenido. Si está vacío (o tiene sin tocar la
// plantilla que se eligió antes), la reemplaza; si ya hay algo escrito, la
// agrega al final para no borrar nada.
fun aplicarPlantilla(contenido: String, plantillaAnterior: String?, nueva: String): String = when {
    contenido.isBlank() || contenido == plantillaAnterior -> nueva
    nueva.isEmpty() -> contenido
    else -> contenido.trimEnd() + "\n\n" + nueva
}

// Suma una cita (ej: "(Cormen et al., 2009)") al final del contenido.
fun agregarCita(contenido: String, cita: String): String = when {
    contenido.isEmpty() -> cita
    contenido.last().isWhitespace() -> contenido + cita
    else -> "$contenido $cita"
}
