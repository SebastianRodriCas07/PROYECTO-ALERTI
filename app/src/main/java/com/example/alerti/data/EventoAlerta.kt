package com.example.alerti.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eventos_alerta")
data class EventoAlerta(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fechaHoraMillis: Long,       // momento del evento (System.currentTimeMillis())
    val latitud: Double?,
    val longitud: Double?,
    val magnitudPico: Float,         // magnitud máxima registrada durante el impacto
    val estado: String,              // "CANCELADO" o "CONFIRMADO"
    val contactosNotificados: Int    // cuántos contactos recibieron el SMS
)