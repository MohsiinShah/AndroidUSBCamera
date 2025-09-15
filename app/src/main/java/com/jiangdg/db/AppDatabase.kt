package com.jiangdg.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AdEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun adDao(): AdDao
}
