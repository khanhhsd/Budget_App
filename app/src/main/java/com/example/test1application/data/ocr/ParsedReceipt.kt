package com.example.test1application.data.ocr

import java.time.LocalDate

data class ParsedReceipt(
    val amount: Long?,
    val date: LocalDate?,
    val merchant: String?,
    val rawText: String
)
