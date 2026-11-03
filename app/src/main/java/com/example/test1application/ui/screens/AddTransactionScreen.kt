package com.example.test1application.ui.screens

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.test1application.R
import com.example.test1application.model.TransactionNeedType
import com.example.test1application.model.TransactionType
import com.example.test1application.ui.viewmodel.ExpenseViewModel
import com.example.test1application.ui.viewmodel.ScanUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: ExpenseViewModel,
    onBack: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val scanUiState by viewModel.scanUiState.collectAsState()
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedNeedType by remember { mutableStateOf(TransactionNeedType.ESSENTIAL) }
    var selectedCategoryId by remember { mutableStateOf("") }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showScanOptionsDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            viewModel.onImageSelected(context, tempCameraUri!!)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.onImageSelected(context, uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    when (val state = scanUiState) {
        is ScanUiState.Scanning -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(text = stringResource(R.string.scanning_receipt), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        is ScanUiState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetScan() },
                title = { Text(stringResource(R.string.scan_error_title)) },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetScan() }) {
                        Text(stringResource(R.string.manual_entry))
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        viewModel.resetScan()
                        showScanOptionsDialog = true
                    }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            )
        }
        is ScanUiState.Success -> {
            LaunchedEffect(state) {
                selectedType = TransactionType.EXPENSE
                if (state.receipt.amount != null) {
                    amount = state.receipt.amount.toString()
                }
                if (state.receipt.merchant != null) {
                    title = state.receipt.merchant
                }
                viewModel.resetScan()
            }
        }
        is ScanUiState.Idle -> {}
    }

    if (showScanOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showScanOptionsDialog = false },
            title = { Text(stringResource(R.string.scan_receipt)) },
            text = { Text("Chọn phương thức quét hóa đơn") },
            confirmButton = {
                TextButton(onClick = {
                    showScanOptionsDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text(stringResource(R.string.choose_from_gallery))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showScanOptionsDialog = false
                    val permission = android.Manifest.permission.CAMERA
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(permission)
                    }
                }) {
                    Text(stringResource(R.string.take_photo))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm giao dịch", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
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
            // Type Selector (Expense vs Income vs Piggy Bank)
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedType = TransactionType.EXPENSE },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Chi tiêu", fontSize = 13.sp)
                    }
                    SegmentedButton(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Thu nhập", fontSize = 13.sp)
                    }
                    SegmentedButton(
                        selected = selectedType == TransactionType.PIGGY_BANK,
                        onClick = { selectedType = TransactionType.PIGGY_BANK },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("🐷 Đút lợn", fontSize = 13.sp)
                    }
                }
            }

            if (selectedType == TransactionType.PIGGY_BANK) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Số tiền đút lợn (₫)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                            if (parsedAmount > 0.0) {
                                viewModel.addPiggyBankDeposit(parsedAmount)
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Đút lợn ngay 🐷", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                if (selectedType == TransactionType.EXPENSE) {
                    item {
                        Button(
                            onClick = { showScanOptionsDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(R.string.scan_receipt), fontWeight = FontWeight.Bold)
                        }
                    }

                    // Need Type Selector (Essential / Discretionary)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Phân loại chi tiêu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                TransactionNeedType.entries.forEachIndexed { index, needType ->
                                    SegmentedButton(
                                        selected = selectedNeedType == needType,
                                        onClick = { selectedNeedType = needType },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TransactionNeedType.entries.size)
                                    ) {
                                        Text(needType.label, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Title input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (selectedType == TransactionType.EXPENSE) "Tiêu đề / Cửa hàng" else "Tiêu đề / Mô tả") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Amount input
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Số tiền (₫)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Category Selection Header
                item {
                    Text(
                        text = if (selectedType == TransactionType.EXPENSE) "Chọn danh mục (Bắt buộc)" else "Chọn danh mục",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Categories List
                items(categories) { category ->
                    val isSelected = category.id == selectedCategoryId
                    Surface(
                        selected = isSelected,
                        onClick = { selectedCategoryId = category.id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = category.icon, style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Save Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    val isFormValid = if (selectedType == TransactionType.EXPENSE) {
                        selectedCategoryId.isNotBlank() && parsedAmount > 0.0
                    } else {
                        title.isNotBlank() && parsedAmount > 0.0
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                viewModel.addTransaction(
                                    title = title.ifBlank { if (selectedType == TransactionType.EXPENSE) "Chi tiêu" else "Thu nhập" },
                                    amount = parsedAmount,
                                    categoryId = selectedCategoryId.ifBlank { categories.firstOrNull()?.id ?: "1" },
                                    type = selectedType,
                                    needType = selectedNeedType
                                )
                                onBack()
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Lưu giao dịch", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
