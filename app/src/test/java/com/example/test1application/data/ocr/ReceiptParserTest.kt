package com.example.test1application.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReceiptParserTest {

    @Test
    fun testParseReceiptWithTotalKeywordAndDots() {
        val receiptText = """
            CÔNG TY TNHH ABC
            Địa chỉ: 123 Nguyễn Huệ, Q1
            MST: 0123456789
            Ngày: 15/05/2026
            ------------------
            Cà phê sữa: 45.000
            Bánh mì: 25.000
            ------------------
            Tổng cộng: 70.000đ
            Cảm ơn quý khách!
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText)
        assertEquals(70000L, parsed.amount)
        assertEquals(LocalDate.of(2026, 5, 15), parsed.date)
        assertEquals("CÔNG TY TNHH ABC", parsed.merchant)
    }

    @Test
    fun testParseReceiptPrefersTotalOverSubtotal() {
        val receiptText = """
            SUPERMARKET XYZ
            05/06/2026
            Tạm tính: 150,000 VND
            Thuế VAT (10%): 15,000 VND
            Thành tiền: 165.000 VND
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText)
        assertEquals(165000L, parsed.amount)
        assertEquals(LocalDate.of(2026, 6, 5), parsed.date)
        assertEquals("SUPERMARKET XYZ", parsed.merchant)
    }

    @Test
    fun testParseReceiptFallbackLargestAmount() {
        val receiptText = """
            QUÁN ĂN NGON
            Hotline: 0901234567
            SĐT: 02838222333
            250000
        """.trimIndent()

        val parsed = ReceiptParser.parse(receiptText)
        assertEquals(250000L, parsed.amount)
        assertEquals("QUÁN ĂN NGON", parsed.merchant)
    }
}
