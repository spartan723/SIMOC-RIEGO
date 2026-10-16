package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VibrationReading
import kotlinx.coroutines.flow.Flow

@Dao
interface VibrationReadingDao {
    @Query("SELECT * FROM vibration_readings WHERE pumpId = :pumpId ORDER BY timestamp DESC LIMIT 100")
    fun getRecentReadings(pumpId: Long): Flow<List<VibrationReading>>

    @Query("SELECT * FROM vibration_readings WHERE pumpId = :pumpId ORDER BY timestamp DESC LIMIT 20")
    fun getTrendReadings(pumpId: Long): Flow<List<VibrationReading>>

    @Query("SELECT * FROM vibration_readings WHERE pumpId = :pumpId AND status IN ('ADVERTENCIA', 'CRITICO') ORDER BY timestamp DESC")
    fun getAlertEvents(pumpId: Long): Flow<List<VibrationReading>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: VibrationReading): Long

    @Query("DELETE FROM vibration_readings WHERE pumpId = :pumpId")
    suspend fun clearReadingsForPump(pumpId: Long)
}
