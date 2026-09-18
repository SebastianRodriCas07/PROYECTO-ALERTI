package com.example.alerti.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoAlertaDao {

    @Insert
    suspend fun insertar(evento: EventoAlerta): Long

    @Query("SELECT * FROM eventos_alerta ORDER BY fechaHoraMillis DESC")
    fun obtenerTodos(): Flow<List<EventoAlerta>>

    @Query("SELECT COUNT(*) FROM eventos_alerta WHERE estado = 'CONFIRMADO'")
    suspend fun contarConfirmados(): Int
}