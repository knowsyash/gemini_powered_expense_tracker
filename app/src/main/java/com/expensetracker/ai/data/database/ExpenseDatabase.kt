package com.expensetracker.ai.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.ai.data.dao.BudgetDao
import com.expensetracker.ai.data.model.Budget
import com.expensetracker.ai.data.model.ChatMessage
import com.expensetracker.ai.data.model.Expense
import com.expensetracker.ai.data.model.FinancialInsight
import com.expensetracker.ai.data.model.SavingsGoal

@Database(
        entities =
                [
                        Expense::class,
                        SavingsGoal::class,
                        FinancialInsight::class,
                        ChatMessage::class,
                        Budget::class],
        version = 6,
        exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExpenseDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun financialInsightDao(): FinancialInsightDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile private var INSTANCE: ExpenseDatabase? = null

        private val MIGRATION_1_2 =
                object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Create savings_goals table
                        database.execSQL(
                                """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `currentAmount` REAL NOT NULL DEFAULT 0.0,
                        `targetDate` INTEGER NOT NULL,
                        `createdDate` INTEGER NOT NULL,
                        `category` TEXT NOT NULL DEFAULT 'General',
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `description` TEXT NOT NULL DEFAULT '',
                        `priority` INTEGER NOT NULL DEFAULT 1
                    )
                """
                        )

                        // Create financial_insights table
                        database.execSQL(
                                """
                    CREATE TABLE IF NOT EXISTS `financial_insights` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `insightType` TEXT NOT NULL,
                        `relevantData` TEXT NOT NULL,
                        `generatedDate` INTEGER NOT NULL,
                        `isRead` INTEGER NOT NULL DEFAULT 0,
                        `actionTaken` INTEGER NOT NULL DEFAULT 0,
                        `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `aiConfidence` REAL NOT NULL DEFAULT 0.0
                    )
                """
                        )
                    }
                }

        private val MIGRATION_2_3 =
                object : Migration(2, 3) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Add uniqueId column to expenses table
                        database.execSQL(
                                "ALTER TABLE expenses ADD COLUMN uniqueId TEXT NOT NULL DEFAULT ''"
                        )

                        // Update existing records with unique IDs
                        database.execSQL(
                                """
                    UPDATE expenses 
                    SET uniqueId = substr(lower(hex(randomblob(3))), 1, 5) 
                    WHERE uniqueId = ''
                """
                        )
                    }
                }

        private val MIGRATION_3_4 =
                object : Migration(3, 4) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Add currency fields to expenses table
                        database.execSQL("ALTER TABLE expenses ADD COLUMN originalAmount REAL")
                        database.execSQL("ALTER TABLE expenses ADD COLUMN originalCurrency TEXT")
                        database.execSQL("ALTER TABLE expenses ADD COLUMN exchangeRate REAL")
                        database.execSQL("ALTER TABLE expenses ADD COLUMN lastRateUpdate INTEGER")
                    }
                }

        private val MIGRATION_4_5 =
                object : Migration(4, 5) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Create chat_messages table
                        database.execSQL(
                                """
                    CREATE TABLE IF NOT EXISTS `chat_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `message` TEXT NOT NULL,
                        `isUser` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `messageType` TEXT NOT NULL,
                        `metadata` TEXT
                    )
                """
                        )
                    }
                }

        private val MIGRATION_5_6 =
                object : Migration(5, 6) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        // Create budgets table
                        database.execSQL(
                                """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        `createdDate` INTEGER NOT NULL,
                        `description` TEXT
                    )
                """
                        )
                    }
                }

        fun getDatabase(context: Context): ExpenseDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                ExpenseDatabase::class.java,
                                                "expense_database"
                                        )
                                        .addMigrations(
                                                MIGRATION_1_2,
                                                MIGRATION_2_3,
                                                MIGRATION_3_4,
                                                MIGRATION_4_5,
                                                MIGRATION_5_6
                                        )
                                        .build()
                        INSTANCE = instance
                        instance
                    }
        }
    }
}
