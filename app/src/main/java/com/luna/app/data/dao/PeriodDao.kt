package com.luna.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.luna.app.data.entity.PeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(period: PeriodEntity): Long

    @Update
    suspend fun updatePeriod(period: PeriodEntity)

    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    fun getAllPeriods(): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods ORDER BY startDate DESC LIMIT 1")
    fun getLatestPeriod(): Flow<PeriodEntity?>
    
    @Query("SELECT * FROM periods ORDER BY startDate DESC LIMIT :limit")
    suspend fun getRecentPeriods(limit: Int): List<PeriodEntity>
}
