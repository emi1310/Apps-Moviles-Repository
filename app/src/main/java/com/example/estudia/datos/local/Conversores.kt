package com.example.estudia.datos.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

// TypeConverters: le enseñan a Room a guardar tipos que SQLite no conoce.
//  - LocalDate se guarda como número de días desde 1970-01-01 (epochDay).
//    Al ser un número, ORDER BY fecha ordena bien y se puede comparar.
//  - LocalTime se guarda como minutos desde la medianoche (18:30 -> 1110).
class Conversores {

    @TypeConverter
    fun fechaANumero(fecha: LocalDate?): Long? = fecha?.toEpochDay()

    @TypeConverter
    fun numeroAFecha(dia: Long?): LocalDate? = dia?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun horaANumero(hora: LocalTime?): Int? = hora?.let { it.hour * 60 + it.minute }

    @TypeConverter
    fun numeroAHora(minutos: Int?): LocalTime? = minutos?.let { LocalTime.of(it / 60, it % 60) }
}
