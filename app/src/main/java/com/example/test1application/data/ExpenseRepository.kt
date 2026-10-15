package com.example.test1application.data

import com.example.test1application.model.Category
import com.example.test1application.model.Transaction
import com.example.test1application.model.TransactionNeedType
import com.example.test1application.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ExpenseRepository {

    private val initialCategories = listOf(
        Category("1", "Ăn uống", "🍔", 4000000.0),
        Category("2", "Đi lại", "🚗", 2000000.0),
        Category("3", "Hóa đơn", "📄", 1500000.0),
        Category("4", "Mua sắm", "🛍️", 3000000.0),
        Category("5", "Đầu tư", "📈", 5000000.0),
        Category("6", "Lương", "💰", 0.0),
        Category("7", "Thưởng", "🎁", 0.0)
    )

    private val initialTransactions = listOf(
        Transaction(title = "Lương tháng", amount = 18500000.0, categoryId = "6", type = TransactionType.INCOME),
        Transaction(title = "Ăn tối nhà hàng", amount = 850000.0, categoryId = "1", type = TransactionType.EXPENSE, needType = TransactionNeedType.DISCRETIONARY),
        Transaction(title = "Tiền điện nước", amount = 650000.0, categoryId = "3", type = TransactionType.EXPENSE, needType = TransactionNeedType.ESSENTIAL),
        Transaction(title = "Đầu tư chứng khoán", amount = 2000000.0, categoryId = "5", type = TransactionType.EXPENSE, needType = TransactionNeedType.ESSENTIAL),
        Transaction(title = "Mua quần áo mới", amount = 1200000.0, categoryId = "4", type = TransactionType.EXPENSE, needType = TransactionNeedType.DISCRETIONARY)
    )

    private val _categories = MutableStateFlow(initialCategories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _transactions = MutableStateFlow(initialTransactions)
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    fun addTransaction(transaction: Transaction) {
        _transactions.value = listOf(transaction) + _transactions.value
    }

    fun deleteTransaction(id: String) {
        _transactions.value = _transactions.value.filter { it.id != id }
    }

    fun setTransactions(newTxs: List<Transaction>) {
        _transactions.value = newTxs
    }

    fun addCategory(category: Category) {
        _categories.value = _categories.value + category
    }

    fun updateCategoryBudget(categoryId: String, newLimit: Double) {
        _categories.value = _categories.value.map {
            if (it.id == categoryId) it.copy(budgetLimit = newLimit) else it
        }
    }
}
