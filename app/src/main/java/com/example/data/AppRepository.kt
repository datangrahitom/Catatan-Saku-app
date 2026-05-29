package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepository(
    private val transactionDao: TransactionDao,
    private val securityDao: SecurityDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()
        .map { entityList ->
            entityList.map { it.toDomain() }
        }

    val securitySettings: Flow<SecuritySettingsEntity?> = securityDao.getSecuritySettingsFlow()

    suspend fun getSecuritySettings(): SecuritySettingsEntity? {
        return securityDao.getSecuritySettings()
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    suspend fun deleteTransaction(id: Int) {
        transactionDao.deleteTransaction(id)
    }

    suspend fun clearAllTransactions() {
        transactionDao.clearAllTransactions()
    }

    suspend fun saveSecuritySettings(settings: SecuritySettingsEntity) {
        securityDao.insertSecuritySettings(settings)
    }
}
