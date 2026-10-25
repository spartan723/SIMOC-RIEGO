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
    entities = [PumpProfile::class, VibrationReading::class, MaintenanceRecord::class],
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
                    "bombavigia_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
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

            private suspend fun populateInitialData(db: AgroDatabase) {
                val pumpDao = db.pumpDao()
                val pumpId = pumpDao.insertPump(
                    PumpProfile(
                        name = "Bomba Pozo Profundo #1",
                        sector = "Sector Cuadro 4 - Frutales",
                        pumpType = "Turbina Sumergible 7.5 HP",
                        baselineRms = 1.95f,
                        baselineFreq = 49.5f,
                        yellowThresholdMultiplier = 1.35f,
                        redThresholdMultiplier = 2.0f,
                        alertPhoneNumber = "+5491123456789",
                        isRunning = true,
                        isCalibrated = true,
                        totalOperatingHours = 184.2f
                    )
                )

                val mDao = db.maintenanceRecordDao()
                val now = System.currentTimeMillis()
                mDao.insertRecord(
                    MaintenanceRecord(
                        pumpId = pumpId,
                        timestamp = now - (14L * 86400000L),
                        actionType = "Engrase y sellos mecánicos",
                        description = "Alerta amarilla preventiva por fricción inicial en rodamientos.",
                        estimatedRepairSavedUsd = 420.0,
                        technicianName = "Juan Pérez (Operador de Riego)"
                    )
                )
                mDao.insertRecord(
                    MaintenanceRecord(
                        pumpId = pumpId,
                        timestamp = now - (45L * 86400000L),
                        actionType = "Alineación de acople flexible",
                        description = "Vibración armónica 2x RPM detectada a tiempo.",
                        estimatedRepairSavedUsd = 650.0,
                        technicianName = "Taller Mecánico Rural"
                    )
                )
            }
        }
    }
}
