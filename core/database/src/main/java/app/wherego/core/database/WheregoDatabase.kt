package app.wherego.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class WheregoDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
}
