package com.example.test1application.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.test1application.model.TransactionType
import com.example.test1application.ui.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAdd: () -> Unit
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, INCOME, EXPENSE

    val finalTransactions = filteredTransactions.filter { tx ->
        val matchesSearch = tx.title.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "INCOME" -> tx.type == TransactionType.INCOME
            "EXPENSE" -> tx.type == TransactionType.EXPENSE
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử giao dịch", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month & Year Selector
            MonthYearSelector(
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                onMonthYearChanged = { year, month ->
                    viewModel.setSelectedMonthYear(year, month)
                }
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm kiếm giao dịch...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Tất cả") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedFilter == "EXPENSE",
                    onClick = { selectedFilter = "EXPENSE" },
                    label = { Text("Chi tiêu") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedFilter == "INCOME",
                    onClick = { selectedFilter = "INCOME" },
                    label = { Text("Thu nhập") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Transaction List
            if (finalTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không có giao dịch nào trong tháng này.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(finalTransactions) { tx ->
                        val category = categories.find { it.id == tx.categoryId }
                        TransactionItem(
                            transaction = tx,
                            category = category,
                            currencyFormat = currencyFormat,
                            onDelete = { viewModel.deleteTransaction(tx.id) }
                        )
                    }
                }
            }
        }
    }
}
