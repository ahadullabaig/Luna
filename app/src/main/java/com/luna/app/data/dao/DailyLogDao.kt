package com.luna.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.luna.app.data.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface DailyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyLog(log: DailyLogEntity)

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun getDailyLog(date: LocalDate): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getDailyLogsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyLogEntity>>
}
