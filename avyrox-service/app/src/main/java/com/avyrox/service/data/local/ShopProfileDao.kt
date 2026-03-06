package com.avyrox.service.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopProfileDao {

    @Query("SELECT * FROM shop_profile WHERE id = 'current' LIMIT 1")
    fun observe(): Flow<ShopProfileEntity?>

    @Query("SELECT * FROM shop_profile WHERE id = 'current' LIMIT 1")
    suspend fun get(): ShopProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ShopProfileEntity)
}
