package com.example.resto.data

import android.content.Context
import android.provider.CalendarContract.Instances
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.resto.data.makanan.Makanan
import com.example.resto.data.makanan.MakananDao
import java.util.concurrent.locks.Lock

@Database(entities = [Makanan::class], version = 1)
abstract class RestoDatabase : RoomDatabase() {

    abstract fun getMakananDao(): MakananDao

    companion object{
        @Volatile
        private var Instance: RestoDatabase? = null
        private val Lock = Any()

        operator fun invoke(context : Context) = Instance ?: synchronized(Lock){
            Instance ?: buildDatabase(context).also {
                Instance = it
            }
        }

        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            RestoDatabase::class.java,
            "resto-db"
        ).build()
    }
}