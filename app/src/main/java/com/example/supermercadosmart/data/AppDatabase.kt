package com.example.supermercadosmart.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Item::class, ShoppingList::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun shoppingListDao(): ShoppingListDao

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

        /** Nome da lista criada na migração e na primeira instalação. */
        const val DEFAULT_LIST_NAME = "Minha lista"

        /**
         * Versão 2 → 3: várias listas, cada uma com o seu orçamento.
         * Cria a tabela "shopping_lists" com a lista "Minha lista" (id 1), que recebe o
         * orçamento que já existia e todos os itens atuais. A tabela antiga
         * "budget_settings" deixa de existir.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS shopping_lists (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "maxBudget REAL NOT NULL, " +
                        "createdAt INTEGER NOT NULL)"
                )
                // Orçamento que já existia (se a tabela antiga não estiver lá, fica sem orçamento)
                var oldBudget = 0.0
                val hasBudgetTable = db.query(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'budget_settings'"
                ).use { it.moveToFirst() }
                if (hasBudgetTable) {
                    db.query("SELECT maxBudget FROM budget_settings WHERE id = 1 LIMIT 1").use { cursor ->
                        if (cursor.moveToFirst()) oldBudget = cursor.getDouble(0)
                    }
                }
                db.execSQL(
                    "INSERT INTO shopping_lists (id, name, maxBudget, createdAt) VALUES (1, ?, ?, ?)",
                    arrayOf<Any>(DEFAULT_LIST_NAME, oldBudget, System.currentTimeMillis())
                )
                db.execSQL("ALTER TABLE items ADD COLUMN listId INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_listId ON items (listId)")
                db.execSQL("DROP TABLE IF EXISTS budget_settings")
            }
        }

        /** Primeira instalação: já começa com uma lista vazia. */
        private val createDefaultList = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "INSERT INTO shopping_lists (name, maxBudget, createdAt) VALUES (?, 0, ?)",
                    arrayOf<Any>(DEFAULT_LIST_NAME, System.currentTimeMillis())
                )
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(createDefaultList)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
