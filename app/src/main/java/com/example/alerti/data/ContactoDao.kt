package com.example.alerti.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(contacto: Contacto): Long

    @Update
    suspend fun actualizar(contacto: Contacto)

    @Delete
    suspend fun eliminar(contacto: Contacto)

    @Query("SELECT * FROM contactos ORDER BY prioridad ASC")
    fun obtenerTodos(): Flow<List<Contacto>>

    @Query("SELECT * FROM contactos ORDER BY prioridad ASC")
    suspend fun obtenerTodosUnaVez(): List<Contacto>
}