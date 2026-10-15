package com.example.test1application.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test1application.model.Category
import com.example.test1application.model.Transaction
import com.example.test1application.model.TransactionType
import com.example.test1application.ui.viewmodel.CategoryBreakdownItem
import com.example.test1application.ui.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAdd: () -> Unit
) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()

    val essentialExpense by viewModel.essentialExpense.collectAsState()
    val discretionaryExpense by viewModel.discretionaryExpense.collectAsState()

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm giao dịch")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thêm giao dịch", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header / Title & Month Selector
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Quản lý Tài chính",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    MonthYearSelector(
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth,
                        onMonthYearChanged = { year, month ->
                            viewModel.setSelectedMonthYear(year, month)
                        }
                    )
                }
            }

            // Balance Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SỐ DƯ THÁNG NÀY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currencyFormat.format(balance),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SummaryItem(
                                label = "Thu nhập",
                                amount = currencyFormat.format(totalIncome),
                                color = Color(0xFF34D399)
                            )
                            VerticalDivider(
                                modifier = Modifier.height(36.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                            )
                            SummaryItem(
                                label = "Chi tiêu",
                                amount = currencyFormat.format(totalExpense),
                                color = Color(0xFFF87171)
                            )
                        }
                    }
                }
            }

            // Spending Map / Need Breakdown Section Header
            item {
                Text(
                    text = "Bản đồ phân loại chi tiêu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Spending Map Card (Essential, Discretionary, Savings / Remaining)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val baseIncome = if (totalIncome > 0) totalIncome else (if (totalExpense > 0) totalExpense else 1.0)
                        val essentialRatio = (essentialExpense / baseIncome).toFloat().coerceIn(0f, 1f)
                        val discretionaryRatio = (discretionaryExpense / baseIncome).toFloat().coerceIn(0f, 1f)
                        val savingsAmount = maxOf(0.0, balance)
                        val savingsRatio = (savingsAmount / baseIncome).toFloat().coerceIn(0f, 1f)

                        NeedRow(
                            title = "Thiết yếu",
                            amount = currencyFormat.format(essentialExpense),
                            ratio = essentialRatio,
                            color = Color(0xFF3B82F6)
                        )
                        NeedRow(
                            title = "Mong muốn",
                            amount = currencyFormat.format(discretionaryExpense),
                            ratio = discretionaryRatio,
                            color = Color(0xFFEC4899)
                        )
                        NeedRow(
                            title = "Tiết kiệm (Tiền còn lại)",
                            amount = currencyFormat.format(savingsAmount),
                            ratio = savingsRatio,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }

            // Spending Breakdown Section Header
            item {
                Text(
                    text = "Biểu đồ tỷ lệ chi tiêu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Spending Breakdown Pie Chart Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val activeBreakdown = categoryBreakdown.filter { it.spent > 0.0 }
                        if (activeBreakdown.isEmpty()) {
                            Text(
                                text = "Chưa có dữ liệu chi tiêu trong tháng này.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                PieChart(items = activeBreakdown)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            activeBreakdown.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val chartColors = listOf(
                                            Color(0xFF3B82F6),
                                            Color(0xFFEC4899),
                                            Color(0xFF10B981),
                                            Color(0xFFF59E0B),
                                            Color(0xFF8B5CF6),
                                            Color(0xFF06B6D4),
                                            Color(0xFFEF4444)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(chartColors[index % chartColors.size])
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = item.category.icon, fontSize = 16.sp)
                                        }
                                        Text(
                                            text = item.category.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = "${currencyFormat.format(item.spent)} (${String.format(Locale.getDefault(), "%.1f%%", item.percentage * 100)})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Budgets & Warnings Section Header
            item {
                Text(
                    text = "Hạn mức & Cảnh báo chi tiêu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Category Budgets List
            items(categories.filter { it.budgetLimit > 0.0 }) { category ->
                val spent = filteredTransactions
                    .filter { it.categoryId == category.id && it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                val progress = if (category.budgetLimit > 0) (spent / category.budgetLimit).toFloat().coerceIn(0f, 1f) else 0f
                val budgetRatio = if (category.budgetLimit > 0) spent / category.budgetLimit else 0.0

                val warningText = when {
                    budgetRatio >= 1.0 -> "🚨 Vượt hạn mức 100%!"
                    budgetRatio >= 0.8 -> "⚠️ Cảnh báo 80%"
                    budgetRatio >= 0.5 -> "⚡ Đạt 50%"
                    else -> "✅ An toàn"
                }

                val warningColor = when {
                    budgetRatio >= 1.0 -> Color(0xFFEF4444)
                    budgetRatio >= 0.8 -> Color(0xFFF97316)
                    budgetRatio >= 0.5 -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = category.icon, fontSize = 18.sp)
                                }
                                Text(
                                    text = category.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            IconButton(
                                onClick = { editingCategory = category },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Sửa hạn mức",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${currencyFormat.format(spent)} / ${currencyFormat.format(category.budgetLimit)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = warningText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = warningColor
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = warningColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // Recent Transactions Header
            item {
                Text(
                    text = "Giao dịch trong tháng",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Transactions Items
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có giao dịch nào trong tháng này.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredTransactions.take(5)) { tx ->
                    val category = categories.find { it.id == tx.categoryId }
                    TransactionItem(
                        transaction = tx,
                        category = category,
                        currencyFormat = currencyFormat,
                        onDelete = { viewModel.deleteTransaction(tx.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // Edit Budget Dialog
    if (editingCategory != null) {
        var newLimitStr by remember { mutableStateOf(editingCategory!!.budgetLimit.toString()) }
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Đặt hạn mức cho ${editingCategory!!.name}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newLimitStr,
                    onValueChange = { newLimitStr = it },
                    label = { Text("Hạn mức mới (₫)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = newLimitStr.toDoubleOrNull()
                        if (limit != null && limit >= 0) {
                            viewModel.updateCategoryBudget(editingCategory!!.id, limit)
                        }
                        editingCategory = null
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Hủy")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun MonthYearSelector(
    selectedYear: Int,
    selectedMonth: Int,
    onMonthYearChanged: (Int, Int) -> Unit
) {
    val monthNames = listOf("Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (selectedMonth > 0) {
                    onMonthYearChanged(selectedYear, selectedMonth - 1)
                } else {
                    onMonthYearChanged(selectedYear - 1, 11)
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Tháng trước")
            }

            Text(
                text = "${monthNames[selectedMonth]}, $selectedYear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = {
                if (selectedMonth < 11) {
                    onMonthYearChanged(selectedYear, selectedMonth + 1)
                } else {
                    onMonthYearChanged(selectedYear + 1, 0)
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Tháng sau")
            }
        }
    }
}

@Composable
fun NeedRow(title: String, amount: String, ratio: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$amount (${String.format(Locale.getDefault(), "%.1f%%", ratio * 100)})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun PieChart(items: List<CategoryBreakdownItem>) {
    val totalSpent = items.sumOf { it.spent }.toFloat()
    if (totalSpent <= 0f) return

    val chartColors = listOf(
        Color(0xFF3B82F6),
        Color(0xFFEC4899),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF8B5CF6),
        Color(0xFF06B6D4),
        Color(0xFFEF4444)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        items.forEachIndexed { index, item ->
            if (item.spent > 0) {
                val sweepAngle = (item.spent.toFloat() / totalSpent) * 360f
                drawArc(
                    color = chartColors[index % chartColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    category: Category?,
    currencyFormat: NumberFormat,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val isIncome = transaction.type == TransactionType.INCOME

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category?.icon ?: "💰", fontSize = 22.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val needText = if (!isIncome) " • ${transaction.needType.label}" else ""
                    Text(
                        text = "${category?.name ?: "Khác"}$needText • ${dateFormat.format(Date(transaction.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${currencyFormat.format(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) Color(0xFF34D399) else Color(0xFFF87171),
                    style = MaterialTheme.typography.bodyMedium
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
