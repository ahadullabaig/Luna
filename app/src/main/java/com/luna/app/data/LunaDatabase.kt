package com.luna.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.luna.app.data.dao.DailyLogDao
import com.luna.app.data.dao.PeriodDao
import com.luna.app.data.entity.DailyLogEntity
import com.luna.app.data.entity.PeriodEntity

/**
 * Schemas are exported to app/schemas/ and committed. That JSON is what makes a correct
 * migration writable later: without it there is no record of what version N looked like, and
 * a migration to N+1 is guesswork. The KSP argument that sets the directory is in
 * app/build.gradle.kts.
 *
 * Bumping [version] without adding a Migration now fails loudly at runtime rather than
 * destroying the database — see AppModule.provideLunaDatabase.
 */
@Database(
    entities = [PeriodEntity::class, DailyLogEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class LunaDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun dailyLogDao(): DailyLogDao
}
