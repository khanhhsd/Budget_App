package com.example.test1application.model

import java.util.UUID

enum class TransactionType {
    INCOME, EXPENSE, PIGGY_BANK
}

enum class TransactionNeedType(val label: String) {
    ESSENTIAL("Thiết yếu"),
    DISCRETIONARY("Mong muốn")
}

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val budgetLimit: Double = 5000000.0
)

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val categoryId: String,
    val type: TransactionType,
    val needType: TransactionNeedType = TransactionNeedType.ESSENTIAL
)
