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

    @Provides
    @Singleton
    @Suppress("DEPRECATION")
    fun provideLunaDatabase(app: Application): LunaDatabase {
        return Room.databaseBuilder(
            app,
            LunaDatabase::class.java,
            "luna.db"
        )
        .fallbackToDestructiveMigration()
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
