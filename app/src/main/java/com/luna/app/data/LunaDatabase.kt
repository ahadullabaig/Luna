package com.luna.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.luna.app.data.dao.DailyLogDao
import com.luna.app.data.dao.PeriodDao
import com.luna.app.data.entity.DailyLogEntity
import com.luna.app.data.entity.PeriodEntity

@Database(
    entities = [PeriodEntity::class, DailyLogEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LunaDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun dailyLogDao(): DailyLogDao
}
