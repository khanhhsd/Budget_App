package com.example.test1application.data.ocr

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.regex.Pattern

object ReceiptParser {

    private val totalKeywords = listOf("tổng cộng", "tong cong", "tổng", "tong", "thành tiền", "thanh tien", "thanh toán", "thanh toan", "total", "amount", "sum")
    private val subtotalKeywords = listOf("tạm tính", "tam tinh", "subtotal")

    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH)
    )

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val amount = parseAmount(lines)
        val date = parseDate(rawText)
        val merchant = parseMerchant(lines)

        return ParsedReceipt(
            amount = amount,
            date = date,
            merchant = merchant,
            rawText = rawText
        )
    }

    private fun parseAmount(lines: List<String>): Long? {
        val candidateAmounts = mutableListOf<Pair<Long, Int>>() // Pair(amount, priority/score)

        // 1. Scan line by line for keywords
        for (line in lines) {
            val lowerLine = line.lowercase(Locale.ROOT)
            val hasTotalKeyword = totalKeywords.any { lowerLine.contains(it) }
            val hasSubtotalKeyword = subtotalKeywords.any { lowerLine.contains(it) }

            if (hasTotalKeyword || hasSubtotalKeyword) {
                val extracted = extractNumbersFromLine(line)
                for ((num, token) in extracted) {
                    if (isValidAmount(num, token)) {
                        val priority = if (hasTotalKeyword) 2 else 1
                        candidateAmounts.add(Pair(num, priority))
                    }
                }
            }
        }

        if (candidateAmounts.isNotEmpty()) {
            candidateAmounts.sortWith(compareByDescending<Pair<Long, Int>> { it.second }.thenByDescending { it.first })
            return candidateAmounts.first().first
        }

        // 2. Fallback: extract all valid numbers from all lines and find the largest plausible amount
        val allNumbers = mutableListOf<Long>()
        for (line in lines) {
            val extracted = extractNumbersFromLine(line)
            for ((num, token) in extracted) {
                if (isValidAmount(num, token)) {
                    allNumbers.add(num)
                }
            }
        }

        return allNumbers.maxOrNull()
    }

    private fun extractNumbersFromLine(line: String): List<Pair<Long, String>> {
        val numbers = mutableListOf<Pair<Long, String>>()
        val matcher = Pattern.compile("([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)").matcher(line)
        while (matcher.find()) {
            val token = matcher.group(1) ?: continue
            val cleanToken = token.replace(".", "").replace(",", "")
            val value = cleanToken.toLongOrNull()
            if (value != null) {
                numbers.add(Pair(value, token))
            }
        }
        return numbers
    }

    private fun isValidAmount(num: Long, tokenStr: String): Boolean {
        if (num <= 0 || num > 999999999L) return false
        if (num in 2000L..2100L) return false
        if (tokenStr.startsWith("0") && tokenStr.length >= 10) return false
        if (num >= 1000000000L) return false
        return true
    }

    private fun parseDate(rawText: String): LocalDate? {
        val dateRegex = Regex("\\b(\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}|\\d{4}[./-]\\d{1,2}[./-]\\d{1,2})\\b")
        val matches = dateRegex.findAll(rawText)

        for (match in matches) {
            val dateStr = match.value
            for (formatter in dateFormats) {
                try {
                    val parsed = LocalDate.parse(dateStr, formatter)
                    if (parsed.year in 2000..2100) {
                        return parsed
                    }
                } catch (e: DateTimeParseException) {
                    // Try next
                }
            }
        }
        return null
    }

    private fun parseMerchant(lines: List<String>): String? {
        for (i in 0 until minOf(5, lines.size)) {
            val line = lines[i]
            val lower = line.lowercase(Locale.ROOT)
            if (lower.contains("tel") || lower.contains("đt") || lower.contains("hotline") ||
                lower.contains("mst") || lower.contains("mã số thuế") ||
                line.length < 3 || line.all { !it.isLetter() }) {
                continue
            }
            return line
        }
        return lines.firstOrNull { it.length >= 3 && it.any { c -> c.isLetter() } }
    }
}
