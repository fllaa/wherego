package app.wherego.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class WheregoDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `emoji` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `isPreset` INTEGER NOT NULL,
                        `archived` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_categories_archived` ON `categories` (`archived`)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions` (
                        `id` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `amountMinor` INTEGER NOT NULL,
                        `currency` TEXT NOT NULL,
                        `fxRateToBase` TEXT NOT NULL,
                        `amountBaseMinor` INTEGER NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `occurredOn` TEXT NOT NULL,
                        `occurredAt` INTEGER,
                        `recurringId` TEXT,
                        `receiptId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        `dirty` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_occurredOn` ON `transactions` (`occurredOn`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_dirty` ON `transactions` (`dirty`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_deletedAt` ON `transactions` (`deletedAt`)",
                )
            }
        }
    }
}
