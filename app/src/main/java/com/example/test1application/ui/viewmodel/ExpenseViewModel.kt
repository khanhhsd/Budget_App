package com.example.test1application.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test1application.data.ExpenseRepository
import com.example.test1application.model.Category
import com.example.test1application.model.Transaction
import com.example.test1application.model.TransactionNeedType
import com.example.test1application.model.TransactionType
import kotlinx.coroutines.flow.*
import java.util.*

enum class BudgetWarningLevel {
    NORMAL, WARNING_50, ALERT_80, EXCEEDED_100
}

data class CategoryBreakdownItem(
    val category: Category,
    val spent: Double,
    val percentage: Float,
    val warningLevel: BudgetWarningLevel
)

class ExpenseViewModel : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = ExpenseRepository.transactions
    val categories: StateFlow<List<Category>> = ExpenseRepository.categories

    // Selected Year & Month (Default to current)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0-based
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    fun setSelectedMonthYear(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
    }

    // Filtered transactions for selected month/year (excluding piggy bank deposits from monthly budget)
    val filteredTransactions: StateFlow<List<Transaction>> = combine(transactions, _selectedYear, _selectedMonth) { txs, year, month ->
        txs.filter { tx ->
            if (tx.type == TransactionType.PIGGY_BANK) return@filter false
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val balance: StateFlow<Double> = combine(totalIncome, totalExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Piggy Bank Total (Independent of month and year)
    val piggyBankTotal: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == TransactionType.PIGGY_BANK }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addPiggyBankDeposit(amount: Double) {
        if (amount <= 0) return
        val tx = Transaction(
            title = "Đút lợn tiết kiệm",
            amount = amount,
            categoryId = "piggy",
            type = TransactionType.PIGGY_BANK,
            needType = TransactionNeedType.ESSENTIAL
        )
        ExpenseRepository.addTransaction(tx)
    }

    fun resetPiggyBank() {
        val currentTxs = ExpenseRepository.transactions.value
        val remaining = currentTxs.filter { it.type != TransactionType.PIGGY_BANK }
        ExpenseRepository.setTransactions(remaining)
    }

    // Expense breakdown by Need Type for selected month
    val essentialExpense: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE && it.needType == TransactionNeedType.ESSENTIAL }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val discretionaryExpense: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE && it.needType == TransactionNeedType.DISCRETIONARY }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category breakdown with percentages and warning levels for selected month
    val categoryBreakdown: StateFlow<List<CategoryBreakdownItem>> = combine(filteredTransactions, categories) { txs, cats ->
        val totalExp = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        cats.filter { it.budgetLimit > 0.0 }.map { cat ->
            val spent = txs.filter { it.categoryId == cat.id && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val percentage = if (totalExp > 0) (spent / totalExp).toFloat() else 0f
            val budgetRatio = if (cat.budgetLimit > 0) spent / cat.budgetLimit else 0.0
            val warningLevel = when {
                budgetRatio >= 1.0 -> BudgetWarningLevel.EXCEEDED_100
                budgetRatio >= 0.8 -> BudgetWarningLevel.ALERT_80
                budgetRatio >= 0.5 -> BudgetWarningLevel.WARNING_50
                else -> BudgetWarningLevel.NORMAL
            }
            CategoryBreakdownItem(cat, spent, percentage, warningLevel)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySavings: StateFlow<Double> = balance

    val yearlyIncomeProjection: StateFlow<Double> = transactions.map { list ->
        val nonPiggy = list.filter { it.type != TransactionType.PIGGY_BANK }
        if (nonPiggy.isEmpty()) return@map 0.0
        val monthlyMap = nonPiggy.filter { it.type == TransactionType.INCOME }.groupBy { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }
        if (monthlyMap.isEmpty()) return@map 0.0
        val avgMonthly = monthlyMap.values.sumOf { monthlyTxs -> monthlyTxs.sumOf { it.amount } } / monthlyMap.size
        avgMonthly * 12
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val yearlyExpenseProjection: StateFlow<Double> = transactions.map { list ->
        val nonPiggy = list.filter { it.type != TransactionType.PIGGY_BANK }
        if (nonPiggy.isEmpty()) return@map 0.0
        val monthlyMap = nonPiggy.filter { it.type == TransactionType.EXPENSE }.groupBy { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }
        if (monthlyMap.isEmpty()) return@map 0.0
        val avgMonthly = monthlyMap.values.sumOf { monthlyTxs -> monthlyTxs.sumOf { it.amount } } / monthlyMap.size
        avgMonthly * 12
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val yearlySavings: StateFlow<Double> = combine(yearlyIncomeProjection, yearlyExpenseProjection) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addTransaction(title: String, amount: Double, categoryId: String, type: TransactionType, needType: TransactionNeedType) {
        if (amount <= 0) return
        val tx = Transaction(
            title = title.trim().ifBlank { "Giao dịch" },
            amount = amount,
            categoryId = categoryId,
            type = type,
            needType = needType
        )
        ExpenseRepository.addTransaction(tx)
    }

    fun deleteTransaction(id: String) {
        ExpenseRepository.deleteTransaction(id)
    }

    fun updateCategoryBudget(categoryId: String, newLimit: Double) {
        ExpenseRepository.updateCategoryBudget(categoryId, newLimit)
    }

    fun addCategory(name: String, icon: String, budgetLimit: Double) {
        if (name.isBlank()) return
        val cat = Category(
            id = System.currentTimeMillis().toString(),
            name = name.trim(),
            icon = icon.ifBlank { "📌" },
            budgetLimit = budgetLimit
        )
        ExpenseRepository.addCategory(cat)
    }
}
