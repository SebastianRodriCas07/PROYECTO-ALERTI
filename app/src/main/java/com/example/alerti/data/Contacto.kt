package com.example.alerti.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contactos")
data class Contacto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val numero: String,
    val prioridad: Int // 1 = primer contacto a notificar, 2 = segundo, 3 = tercero
)