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
        SyncStateEntity::class,
        BudgetEntity::class,
        RecurringEntity::class,
        ReceiptEntity::class,
        GoalEntity::class,
        FxRateEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class WheregoDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun goalDao(): GoalDao
    abstract fun fxRateDao(): FxRateDao


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

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_state` (
                        `collection` TEXT NOT NULL,
                        `lastPullEpoch` INTEGER NOT NULL,
                        `lastPushEpoch` INTEGER NOT NULL,
                        PRIMARY KEY(`collection`)
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` TEXT NOT NULL,
                        `categoryId` TEXT,
                        `amountMinor` INTEGER NOT NULL,
                        `currency` TEXT NOT NULL,
                        `yearMonth` TEXT NOT NULL,
                        `rollover` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_rules` (
                        `id` TEXT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `amountMinor` INTEGER NOT NULL,
                        `currency` TEXT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `freq` TEXT NOT NULL,
                        `interval` INTEGER NOT NULL,
                        `dayOfMonth` INTEGER,
                        `weekday` INTEGER,
                        `startOn` TEXT NOT NULL,
                        `endOn` TEXT,
                        `nextOn` TEXT NOT NULL,
                        `remindDaysBefore` INTEGER NOT NULL,
                        `autoPost` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_recurring_rules_nextOn` ON `recurring_rules` (`nextOn`)",
                )
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `receipts` (
                        `id` TEXT NOT NULL,
                        `transactionId` TEXT NOT NULL,
                        `localPath` TEXT NOT NULL,
                        `remotePath` TEXT,
                        `ocrRaw` TEXT NOT NULL,
                        `ocrAmountMinor` INTEGER,
                        `uploaded` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_receipts_transactionId` ON `receipts` (`transactionId`)",
                )
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `allocatedMinor` INTEGER NOT NULL,
                        `currency` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fx_rates` (
                        `currency` TEXT NOT NULL,
                        `rateToBase` TEXT NOT NULL,
                        `fetchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`currency`)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
