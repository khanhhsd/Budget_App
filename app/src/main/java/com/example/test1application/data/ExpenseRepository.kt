package com.example.test1application.data

import android.content.Context
import android.util.Log
import com.example.test1application.model.Category
import com.example.test1application.model.Transaction
import com.example.test1application.model.TransactionNeedType
import com.example.test1application.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object ExpenseRepository {

    private const val TAG = "ExpenseRepository"
    private const val FILE_NAME = "expense_data.json"
    private const val SCHEMA_VERSION = 1

    private val defaultCategories = listOf(
        Category("1", "Ăn uống", "🍔", 4000000.0),
        Category("2", "Đi lại", "🚗", 2000000.0),
        Category("3", "Hóa đơn", "📄", 1500000.0),
        Category("4", "Mua sắm", "🛍️", 3000000.0),
        Category("5", "Đầu tư", "📈", 5000000.0),
        Category("6", "Lương", "💰", 0.0),
        Category("7", "Thưởng", "🎁", 0.0)
    )

    private val _categories = MutableStateFlow(defaultCategories)
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    @Volatile
    private var dataFile: File? = null

    /**
     * Load dữ liệu từ filesDir. Chạy đồng bộ (file nhỏ) để UI không bị nháy danh sách rỗng.
     * Gọi nhiều lần thì chỉ load lần đầu.
     */
    @Synchronized
    fun init(context: Context) {
        if (dataFile != null) return
        val file = File(context.applicationContext.filesDir, FILE_NAME)
        val (cats, txs) = load(file)
        _categories.value = cats
        _transactions.value = txs
        dataFile = file
    }

    fun addTransaction(transaction: Transaction) {
        _transactions.update { listOf(transaction) + it }
        persist()
    }

    fun deleteTransaction(id: String) {
        _transactions.update { list -> list.filter { it.id != id } }
        persist()
    }

    fun setTransactions(newTxs: List<Transaction>) {
        _transactions.value = newTxs
        persist()
    }

    fun addCategory(category: Category) {
        _categories.update { it + category }
        persist()
    }

    fun updateCategoryBudget(categoryId: String, newLimit: Double) {
        _categories.update { list ->
            list.map { if (it.id == categoryId) it.copy(budgetLimit = newLimit) else it }
        }
        persist()
    }

    // ---------------------------------------------------------------------
    // Ghi file
    // ---------------------------------------------------------------------

    private fun persist() {
        val file = dataFile
        if (file == null) {
            Log.w(TAG, "persist() trước khi init(): dữ liệu chỉ nằm trong bộ nhớ")
            return
        }
        ioScope.launch {
            writeMutex.withLock {
                // Đọc snapshot bên trong lock để lần ghi sau cùng luôn chứa trạng thái mới nhất.
                val json = toJson(_categories.value, _transactions.value)
                try {
                    writeAtomically(file, json.toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Không ghi được ${file.name}", e)
                }
            }
        }
    }

    /** Ghi ra file tạm rồi rename, để file chính không bao giờ bị ghi dở. */
    private fun writeAtomically(target: File, content: String) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
        if (!tmp.renameTo(target)) {
            // Một số hệ thống file không cho rename đè; xóa file cũ rồi thử lại.
            target.delete()
            if (!tmp.renameTo(target)) {
                throw java.io.IOException("Không rename được ${tmp.name} -> ${target.name}")
            }
        }
    }

    // ---------------------------------------------------------------------
    // Đọc file
    // ---------------------------------------------------------------------

    private fun load(file: File): Pair<List<Category>, List<Transaction>> {
        if (!file.exists()) {
            // Lần chạy đầu: chỉ có danh mục mặc định, không có giao dịch mẫu.
            return defaultCategories to emptyList()
        }
        return try {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val cats = parseCategories(root.optJSONArray("categories"))
            val txs = parseTransactions(root.optJSONArray("transactions"))
            (cats.ifEmpty { defaultCategories }) to txs
        } catch (e: Exception) {
            Log.e(TAG, "File ${file.name} bị hỏng, dùng dữ liệu mặc định", e)
            backupCorruptFile(file)
            defaultCategories to emptyList()
        }
    }

    /** Giữ lại bản file hỏng để không bị lần ghi kế tiếp đè mất hoàn toàn. */
    private fun backupCorruptFile(file: File) {
        try {
            val backup = File(file.parentFile, "${file.name}.corrupt")
            backup.delete()
            file.renameTo(backup)
        } catch (e: Exception) {
            Log.e(TAG, "Không sao lưu được file hỏng", e)
        }
    }

    private fun parseCategories(array: JSONArray?): List<Category> {
        if (array == null) return emptyList()
        val result = ArrayList<Category>(array.length())
        for (i in 0 until array.length()) {
            try {
                val o = array.getJSONObject(i)
                result += Category(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    icon = o.optString("icon", "📌"),
                    budgetLimit = o.optDouble("budgetLimit", 0.0).takeIf { it.isFinite() } ?: 0.0
                )
            } catch (e: Exception) {
                Log.w(TAG, "Bỏ qua danh mục lỗi ở vị trí $i", e)
            }
        }
        return result
    }

    private fun parseTransactions(array: JSONArray?): List<Transaction> {
        if (array == null) return emptyList()
        val result = ArrayList<Transaction>(array.length())
        for (i in 0 until array.length()) {
            try {
                val o = array.getJSONObject(i)
                val amount = o.getDouble("amount")
                require(amount.isFinite()) { "amount không hợp lệ" }
                result += Transaction(
                    id = o.getString("id"),
                    title = o.optString("title", "Giao dịch"),
                    amount = amount,
                    timestamp = o.getLong("timestamp"),
                    categoryId = o.optString("categoryId", ""),
                    type = parseEnum(o.optString("type"), TransactionType.EXPENSE),
                    needType = parseEnum(o.optString("needType"), TransactionNeedType.ESSENTIAL)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Bỏ qua giao dịch lỗi ở vị trí $i", e)
            }
        }
        return result
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private fun toJson(categories: List<Category>, transactions: List<Transaction>): JSONObject {
        val cats = JSONArray()
        categories.forEach { c ->
            cats.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("icon", c.icon)
                    .put("budgetLimit", c.budgetLimit)
            )
        }
        val txs = JSONArray()
        transactions.forEach { t ->
            txs.put(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("amount", t.amount)
                    .put("timestamp", t.timestamp)
                    .put("categoryId", t.categoryId)
                    .put("type", t.type.name)
                    .put("needType", t.needType.name)
            )
        }
        return JSONObject()
            .put("version", SCHEMA_VERSION)
            .put("categories", cats)
            .put("transactions", txs)
    }
}
