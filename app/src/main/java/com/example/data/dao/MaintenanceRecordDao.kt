package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceRecordDao {
    @Query("SELECT * FROM maintenance_records WHERE pumpId = :pumpId ORDER BY timestamp DESC")
    fun getMaintenanceRecords(pumpId: Long): Flow<List<MaintenanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MaintenanceRecord): Long

    @Query("SELECT SUM(estimatedRepairSavedUsd) FROM maintenance_records WHERE pumpId = :pumpId")
    fun getTotalSavings(pumpId: Long): Flow<Double?>
}
