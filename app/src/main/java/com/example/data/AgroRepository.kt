package com.example.data

import com.example.data.dao.MaintenanceRecordDao
import com.example.data.dao.PumpDao
import com.example.data.dao.VibrationReadingDao
import com.example.data.model.MaintenanceRecord
import com.example.data.model.PumpProfile
import com.example.data.model.VibrationReading
import kotlinx.coroutines.flow.Flow

class AgroRepository(
    private val pumpDao: PumpDao,
    private val vibrationReadingDao: VibrationReadingDao,
    private val maintenanceRecordDao: MaintenanceRecordDao
) {
    fun getPump(id: Long): Flow<PumpProfile?> = pumpDao.getPump(id)

    fun getAllPumps(): Flow<List<PumpProfile>> = pumpDao.getAllPumps()

    suspend fun insertPump(pump: PumpProfile): Long = pumpDao.insertPump(pump)

    suspend fun updatePump(pump: PumpProfile) = pumpDao.updatePump(pump)

    fun getRecentReadings(pumpId: Long): Flow<List<VibrationReading>> =
        vibrationReadingDao.getRecentReadings(pumpId)

    fun getAlertEvents(pumpId: Long): Flow<List<VibrationReading>> =
        vibrationReadingDao.getAlertEvents(pumpId)

    suspend fun insertReading(reading: VibrationReading): Long =
        vibrationReadingDao.insertReading(reading)

    fun getMaintenanceRecords(pumpId: Long): Flow<List<MaintenanceRecord>> =
        maintenanceRecordDao.getMaintenanceRecords(pumpId)

    fun getTotalSavings(pumpId: Long): Flow<Double?> =
        maintenanceRecordDao.getTotalSavings(pumpId)

    suspend fun insertMaintenance(record: MaintenanceRecord): Long =
        maintenanceRecordDao.insertRecord(record)
}
