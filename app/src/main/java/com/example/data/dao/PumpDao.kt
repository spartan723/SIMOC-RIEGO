package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PumpProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface PumpDao {
    @Query("SELECT * FROM pump_profiles WHERE id = :id LIMIT 1")
    fun getPumpById(id: Long): Flow<PumpProfile?>

    @Query("SELECT * FROM pump_profiles ORDER BY id ASC")
    fun getAllPumps(): Flow<List<PumpProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPump(pump: PumpProfile): Long

    @Update
    suspend fun updatePump(pump: PumpProfile)

    @Query("UPDATE pump_profiles SET baselineRms = :baselineRms, baselineFreq = :baselineFreq, isCalibrated = 1, lastCalibrationTime = :timestamp WHERE id = :pumpId")
    suspend fun updateBaseline(pumpId: Long, baselineRms: Float, baselineFreq: Float, timestamp: Long)

    @Query("UPDATE pump_profiles SET isRunning = :isRunning WHERE id = :pumpId")
    suspend fun updateRunningState(pumpId: Long, isRunning: Boolean)
}
