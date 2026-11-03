package com.example.test1application.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object ReceiptScanner {

    suspend fun scan(context: Context, uri: Uri): Result<ParsedReceipt> = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromFilePath(context, uri)
            val visionText = recognizer.process(image).await()
            val rawText = visionText.text
            if (rawText.isBlank()) {
                return@withContext Result.failure(Exception("Không tìm thấy văn bản trong ảnh"))
            }
            val parsed = ReceiptParser.parse(rawText)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                recognizer.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
