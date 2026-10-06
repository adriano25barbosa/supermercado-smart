package com.example.supermercadosmart.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Item::class, BudgetSettings::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Versão 1 → 2: acrescenta a coluna "category" sem apagar a lista.
         * Os itens que já existiam recebem a categoria adivinhada pelo nome.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE items ADD COLUMN category TEXT NOT NULL DEFAULT '${Category.OUTROS.name}'"
                )
                val guesses = mutableListOf<Pair<Long, String>>()
                db.query("SELECT id, name FROM items").use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val name = cursor.getString(1) ?: ""
                        guesses += id to Category.guess(name).name
                    }
                }
                for ((id, category) in guesses) {
                    db.execSQL(
                        "UPDATE items SET category = ? WHERE id = ?",
                        arrayOf<Any>(category, id)
                    )
                }
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "supermercado_smart_db"
                )
                    // Migrações reais preservam a lista; o fallback só vale se faltar alguma.
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
