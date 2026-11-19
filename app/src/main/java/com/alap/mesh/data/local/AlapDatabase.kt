package com.alap.mesh.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        FileTransferEntity::class,
        MeshPeerEntity::class,
        PendingPacketEntity::class,
        SeenPacketEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AlapDatabase : RoomDatabase() {
    abstract fun alapDao(): AlapDao

    companion object {
        @Volatile
        private var INSTANCE: AlapDatabase? = null

        fun getInstance(context: Context): AlapDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlapDatabase::class.java,
                    "alap_mesh_v2.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
