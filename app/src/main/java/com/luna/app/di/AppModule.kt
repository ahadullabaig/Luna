package com.luna.app.di

import android.app.Application
import androidx.room.Room
import com.luna.app.data.LunaDatabase
import com.luna.app.data.dao.DailyLogDao
import com.luna.app.data.dao.PeriodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * No `fallbackToDestructiveMigration()`. It used to be here, and it was fine while Luna
     * only ever ran from Android Studio — but it means that bumping the schema version without
     * writing a migration silently deletes the database and rebuilds it empty. Correct key,
     * correct versionCode, install succeeds, and the user opens the app to find every period
     * they logged is gone. No error, no crash, nothing to notice.
     *
     * Luna has no export and no backup — `allowBackup` is false by design — so there is no way
     * back from that. Without the fallback, a forgotten migration throws
     * IllegalStateException on the developer's own phone instead. A crash is recoverable; a
     * wipe on someone else's phone is not.
     *
     * To change the schema: bump `version` in [LunaDatabase], add a Migration object here, and
     * commit the new JSON that KSP writes to app/schemas/.
     */
    @Provides
    @Singleton
    fun provideLunaDatabase(app: Application): LunaDatabase {
        return Room.databaseBuilder(
            app,
            LunaDatabase::class.java,
            "luna.db"
        )
        .build()
    }

    @Provides
    @Singleton
    fun providePeriodDao(db: LunaDatabase): PeriodDao {
        return db.periodDao()
    }

    @Provides
    @Singleton
    fun provideDailyLogDao(db: LunaDatabase): DailyLogDao {
        return db.dailyLogDao()
    }
}
