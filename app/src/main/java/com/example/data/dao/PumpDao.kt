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
    fun getPump(id: Long): Flow<PumpProfile?>

    @Query("SELECT * FROM pump_profiles")
    fun getAllPumps(): Flow<List<PumpProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPump(pump: PumpProfile): Long

    @Update
    suspend fun updatePump(pump: PumpProfile)
}
