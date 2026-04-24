package com.luna.app.data.repo

import com.luna.app.data.dao.DailyLogDao
import com.luna.app.data.dao.PeriodDao
import com.luna.app.data.entity.DailyLogEntity
import com.luna.app.data.entity.PeriodEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CycleRepository @Inject constructor(
    private val periodDao: PeriodDao,
    private val dailyLogDao: DailyLogDao
) {
    // --- Period Operations ---

    fun getAllPeriods(): Flow<List<PeriodEntity>> {
        return periodDao.getAllPeriods()
    }

    fun getLatestPeriod(): Flow<PeriodEntity?> {
        return periodDao.getLatestPeriod()
    }

    suspend fun insertPeriod(period: PeriodEntity): Long {
        return periodDao.insertPeriod(period)
    }

    suspend fun updatePeriod(period: PeriodEntity) {
        periodDao.updatePeriod(period)
    }

    suspend fun getRecentPeriods(limit: Int): List<PeriodEntity> {
        return periodDao.getRecentPeriods(limit)
    }

    // --- Daily Log Operations ---

    fun getDailyLog(date: LocalDate): Flow<DailyLogEntity?> {
        return dailyLogDao.getDailyLog(date)
    }

    fun getDailyLogsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyLogEntity>> {
        return dailyLogDao.getDailyLogsBetween(startDate, endDate)
    }

    suspend fun upsertDailyLog(log: DailyLogEntity) {
        dailyLogDao.upsertDailyLog(log)
    }
}
