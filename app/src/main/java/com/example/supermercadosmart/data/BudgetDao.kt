package com.example.supermercadosmart.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget_settings WHERE id = 1 LIMIT 1")
    fun getBudget(): Flow<BudgetSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(settings: BudgetSettings)
}
