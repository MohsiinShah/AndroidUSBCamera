package com.jiangdg.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AdDao {

    @Query("SELECT * FROM ads")
    suspend fun getAllAds(): List<AdEntity>
    @Query("SELECT * FROM ads ORDER BY slotTime ASC")
    fun observeAllAds(): Flow<List<AdEntity>>

    @Query("SELECT * FROM ads WHERE slotTime = :slotTime LIMIT 1")
    suspend fun getAdForSlot(slotTime: String): AdEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ads: List<AdEntity>)

    @Update
    suspend fun update(ad: AdEntity): Int

    @Query("DELETE FROM ads")
    suspend fun clearAll()
}

