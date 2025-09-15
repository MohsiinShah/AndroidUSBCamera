package com.jiangdg.module

import android.content.Context
import androidx.room.Room
import com.jiangdg.db.AdDao
import com.jiangdg.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ads_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideAdDao(db: AppDatabase): AdDao = db.adDao()
}
