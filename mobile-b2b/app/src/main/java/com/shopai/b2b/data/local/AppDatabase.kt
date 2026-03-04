package com.shopai.b2b.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(
    entities = [DiagnosticCaseEntity::class, ShopProfileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diagnosticCaseDao(): DiagnosticCaseDao
    abstract fun shopProfileDao(): ShopProfileDao

    suspend fun clearAllData() = withContext(Dispatchers.IO) { clearAllTables() }
}
