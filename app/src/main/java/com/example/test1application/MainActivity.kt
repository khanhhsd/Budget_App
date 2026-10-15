package com.example.test1application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test1application.ui.screens.AddTransactionScreen
import com.example.test1application.ui.screens.AnalyticsScreen
import com.example.test1application.ui.screens.DashboardScreen
import com.example.test1application.ui.screens.TransactionsScreen
import com.example.test1application.ui.theme.Test1ApplicationTheme
import com.example.test1application.ui.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Test1ApplicationTheme {
                val viewModel: ExpenseViewModel = viewModel()
                var currentTab by remember { mutableStateOf("dashboard") } // dashboard, history, analytics
                var isAddingTransaction by remember { mutableStateOf(false) }

                if (isAddingTransaction) {
                    AddTransactionScreen(
                        viewModel = viewModel,
                        onBack = { isAddingTransaction = false }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentTab == "dashboard",
                                    onClick = { currentTab = "dashboard" },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Tổng quan") },
                                    label = { Text("Tổng quan") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == "history",
                                    onClick = { currentTab = "history" },
                                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lịch sử") },
                                    label = { Text("Lịch sử") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == "analytics",
                                    onClick = { currentTab = "analytics" },
                                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Tiết kiệm") },
                                    label = { Text("Tiết kiệm") }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (currentTab) {
                                "dashboard" -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToAdd = { isAddingTransaction = true }
                                )
                                "history" -> TransactionsScreen(
                                    viewModel = viewModel,
                                    onNavigateToAdd = { isAddingTransaction = true }
                                )
                                "analytics" -> AnalyticsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
