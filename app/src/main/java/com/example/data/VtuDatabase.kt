package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Wallet::class, Transaction::class, Beneficiary::class], version = 1, exportSchema = false)
abstract class VtuDatabase : RoomDatabase() {
    abstract fun vtuDao(): VtuDao

    companion object {
        @Volatile
        private var INSTANCE: VtuDatabase? = null

        fun getDatabase(context: Context): VtuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VtuDatabase::class.java,
                    "vtu_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
