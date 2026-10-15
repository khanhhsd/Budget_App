package com.example.test1application.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test1application.ui.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: ExpenseViewModel) {
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val monthlySavings by viewModel.monthlySavings.collectAsState()

    val yearlyIncomeProj by viewModel.yearlyIncomeProjection.collectAsState()
    val yearlyExpenseProj by viewModel.yearlyExpenseProjection.collectAsState()
    val yearlySavingsProj by viewModel.yearlySavings.collectAsState()

    val piggyBankTotal by viewModel.piggyBankTotal.collectAsState()

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val savingsRate = if (totalIncome > 0) (monthlySavings / totalIncome * 100).toFloat().coerceIn(-100f, 100f) else 0f

    var tapCount by remember { mutableStateOf(0) }
    var showPiggyDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isAnimated by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isAnimated) 1.4f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { isAnimated = false }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thống kê & Tiết kiệm", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month & Year Selector
            item {
                MonthYearSelector(
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    onMonthYearChanged = { year, month ->
                        viewModel.setSelectedMonthYear(year, month)
                    }
                )
            }

            // Savings Rate Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Tỷ lệ tiết kiệm tháng này",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", savingsRate),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (savingsRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }

            // Monthly Statistics Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Thống kê Tháng",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StatRow(label = "Tổng thu nhập", amount = currencyFormat.format(totalIncome), color = Color(0xFF34D399))
                        StatRow(label = "Tổng chi tiêu", amount = currencyFormat.format(totalExpense), color = Color(0xFFF87171))
                        HorizontalDivider()
                        StatRow(label = "Tiết kiệm ròng", amount = currencyFormat.format(monthlySavings), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Yearly Statistics Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Thống kê Dự báo Năm (Trung bình các tháng)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        StatRow(label = "Thu nhập ước tính (Năm)", amount = currencyFormat.format(yearlyIncomeProj), color = Color(0xFF34D399))
                        StatRow(label = "Chi tiêu ước tính (Năm)", amount = currencyFormat.format(yearlyExpenseProj), color = Color(0xFFF87171))
                        HorizontalDivider()
                        StatRow(label = "Tiết kiệm ước tính (Năm)", amount = currencyFormat.format(yearlySavingsProj), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Piggy Bank Icon at the bottom, centered, large
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Heo đất tiết kiệm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "(Nhấn 3 lần để đập lợn)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🐷",
                        fontSize = 72.sp,
                        modifier = Modifier
                            .scale(scale)
                            .clickable {
                                isAnimated = true
                                tapCount++
                                if (tapCount >= 3) {
                                    tapCount = 0
                                    showPiggyDialog = true
                                }
                            }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Dialog 1: Show total piggy bank amount & regular button to trigger confirm dialog
    if (showPiggyDialog) {
        AlertDialog(
            onDismissRequest = { showPiggyDialog = false },
            title = { Text("🐷 Heo đất tiết kiệm", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tổng số tiền đã đút lợn:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = currencyFormat.format(piggyBankTotal),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPiggyDialog = false
                        showConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔴 Đập lợn", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPiggyDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đóng")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Dialog 2: Final Confirmation before resetting
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("⚠️ Xác nhận đập vỡ heo đất", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
            text = {
                Text(
                    text = "Bạn có thực sự chắc chắn muốn đập vỡ heo đất và thiết lập lại số tiền tiết kiệm về 0 không? Hành động này không thể hoàn tác.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPiggyBank()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Xác nhận đập lợn", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Hủy")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun StatRow(label: String, amount: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = color)
    }
}
