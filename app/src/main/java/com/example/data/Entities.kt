package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.security.KeystoreHelper

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val encryptedTitle: String,
    val encryptedAmount: String,
    val encryptedCategory: String,
    val encryptedNote: String,
    val timestamp: Long,
    val isIncome: Boolean,
    val encryptedPaymentMethod: String = ""
)

@Entity(tableName = "security_settings")
data class SecuritySettingsEntity(
    @PrimaryKey val id: Int = 1,
    val encryptedPasscode: String = "",
    val isPasscodeEnabled: Boolean = false
)

// Domain model for safe clean JVM logic
data class Transaction(
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val note: String,
    val timestamp: Long,
    val isIncome: Boolean,
    val paymentMethod: String = "Tunai"
)

// Mapping helpers
fun TransactionEntity.toDomain(): Transaction {
    val title = KeystoreHelper.decrypt(encryptedTitle)
    val amountStr = KeystoreHelper.decrypt(encryptedAmount)
    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val category = KeystoreHelper.decrypt(encryptedCategory)
    val note = KeystoreHelper.decrypt(encryptedNote)
    val paymentMethod = if (encryptedPaymentMethod.isNotEmpty()) {
        KeystoreHelper.decrypt(encryptedPaymentMethod)
    } else {
        "Tunai"
    }
    return Transaction(
        id = id,
        title = title,
        amount = amount,
        category = category,
        note = note,
        timestamp = timestamp,
        isIncome = isIncome,
        paymentMethod = paymentMethod
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        encryptedTitle = KeystoreHelper.encrypt(title),
        encryptedAmount = KeystoreHelper.encrypt(amount.toString()),
        encryptedCategory = KeystoreHelper.encrypt(category),
        encryptedNote = KeystoreHelper.encrypt(note),
        timestamp = timestamp,
        isIncome = isIncome,
        encryptedPaymentMethod = KeystoreHelper.encrypt(paymentMethod)
    )
}
