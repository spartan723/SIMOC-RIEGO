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
    private val readingDao: VibrationReadingDao,
    private val maintenanceDao: MaintenanceRecordDao
) {
    fun getPump(pumpId: Long = 1): Flow<PumpProfile?> = pumpDao.getPumpById(pumpId)

    fun getAllPumps(): Flow<List<PumpProfile>> = pumpDao.getAllPumps()

    fun getRecentReadings(pumpId: Long = 1): Flow<List<VibrationReading>> =
        readingDao.getRecentReadings(pumpId)

    fun getTrendReadings(pumpId: Long = 1): Flow<List<VibrationReading>> =
        readingDao.getTrendReadings(pumpId)

    fun getAlertEvents(pumpId: Long = 1): Flow<List<VibrationReading>> =
        readingDao.getAlertEvents(pumpId)

    fun getMaintenanceRecords(pumpId: Long = 1): Flow<List<MaintenanceRecord>> =
        maintenanceDao.getMaintenanceRecords(pumpId)

    fun getTotalSavings(pumpId: Long = 1): Flow<Double?> =
        maintenanceDao.getTotalSavings(pumpId)

    suspend fun saveReading(reading: VibrationReading): Long =
        readingDao.insertReading(reading)

    suspend fun updatePump(pump: PumpProfile) =
        pumpDao.updatePump(pump)

    suspend fun updateBaseline(pumpId: Long, baselineRms: Float, baselineFreq: Float) {
        pumpDao.updateBaseline(pumpId, baselineRms, baselineFreq, System.currentTimeMillis())
    }

    suspend fun updateRunningState(pumpId: Long, isRunning: Boolean) {
        pumpDao.updateRunningState(pumpId, isRunning)
    }

    suspend fun addMaintenanceRecord(record: MaintenanceRecord): Long =
        maintenanceDao.insertRecord(record)

    suspend fun clearHistory(pumpId: Long = 1) =
        readingDao.clearReadingsForPump(pumpId)
}
