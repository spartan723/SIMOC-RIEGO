package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.MaintenanceRecordDao
import com.example.data.dao.PumpDao
import com.example.data.dao.VibrationReadingDao
import com.example.data.model.MaintenanceRecord
import com.example.data.model.PumpProfile
import com.example.data.model.VibrationReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        PumpProfile::class,
        VibrationReading::class,
        MaintenanceRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AgroDatabase : RoomDatabase() {
    abstract fun pumpDao(): PumpDao
    abstract fun vibrationReadingDao(): VibrationReadingDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AgroDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AgroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AgroDatabase::class.java,
                    "agrovigia_database"
                )
                .addCallback(AgroDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AgroDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        private suspend fun populateInitialData(database: AgroDatabase) {
            val pumpDao = database.pumpDao()
            val maintenanceDao = database.maintenanceRecordDao()
            val readingDao = database.vibrationReadingDao()

            // Default pump for agricultural community
            val pumpId = pumpDao.insertPump(
                PumpProfile(
                    id = 1,
                    name = "Bomba Pozo 01 (Sector Riego Sur)",
                    sector = "Cooperativa Agrícola Valle Verde",
                    pumpType = "Bomba Centrífuga 7.5 HP",
                    baselineRms = 1.90f,
                    baselineFreq = 49.5f,
                    yellowThresholdMultiplier = 1.35f,
                    redThresholdMultiplier = 2.0f,
                    alertPhoneNumber = "+5491123456789",
                    isRunning = true,
                    isCalibrated = true,
                    totalOperatingHours = 248.0f
                )
            )

            // Seed sample past preventive maintenance demonstrating avoided failures & savings
            maintenanceDao.insertRecord(
                MaintenanceRecord(
                    pumpId = 1,
                    timestamp = System.currentTimeMillis() - 15 * 86400000L,
                    actionType = "Engrase y lubricación preventiva",
                    description = "Vibración subió a 2.4 m/s² (amarillo). Se realizó lubricación con grasa litio en rodamientos antes de recalentamiento.",
                    estimatedRepairSavedUsd = 180.0,
                    technicianName = "Juan Pérez (Productor)"
                )
            )

            maintenanceDao.insertRecord(
                MaintenanceRecord(
                    pumpId = 1,
                    timestamp = System.currentTimeMillis() - 45 * 86400000L,
                    actionType = "Reemplazo preventivo de rodamientos SKF",
                    description = "Detección temprana de juego mecánico a 3.8 m/s². Se evitó enclavamiento de eje y quema de bobinado.",
                    estimatedRepairSavedUsd = 650.0,
                    technicianName = "Taller Rural Los Pinos"
                )
            )

            // Seed historical readings
            val now = System.currentTimeMillis()
            val history = listOf(
                Pair(1.92f, "NORMAL"),
                Pair(1.88f, "NORMAL"),
                Pair(1.95f, "NORMAL"),
                Pair(2.10f, "NORMAL"),
                Pair(2.25f, "NORMAL"),
                Pair(2.55f, "ADVERTENCIA"),
                Pair(2.68f, "ADVERTENCIA"),
                Pair(1.91f, "NORMAL"),
                Pair(1.89f, "NORMAL")
            )
            history.forEachIndexed { index, pair ->
                readingDao.insertReading(
                    VibrationReading(
                        pumpId = 1,
                        timestamp = now - (history.size - index) * 3600000L,
                        rms = pair.first,
                        peakFrequency = 49.8f,
                        peakToPeak = pair.first * 2.8f,
                        status = pair.second,
                        isPumpOn = true,
                        notes = "Ciclo automático de medición",
                        mode = "BOMBA"
                    )
                )
            }
        }
    }
}
