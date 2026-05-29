package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Int)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}

@Dao
interface SecurityDao {
    @Query("SELECT * FROM security_settings WHERE id = 1 LIMIT 1")
    suspend fun getSecuritySettings(): SecuritySettingsEntity?

    @Query("SELECT * FROM security_settings WHERE id = 1 LIMIT 1")
    fun getSecuritySettingsFlow(): Flow<SecuritySettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecuritySettings(settings: SecuritySettingsEntity)
}
