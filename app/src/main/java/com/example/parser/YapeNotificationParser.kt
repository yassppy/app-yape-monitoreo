package com.example.parser

import java.util.regex.Pattern

data class ParsedPayment(
    val sender: String,
    val amount: Double,
    val currency: String = "PEN",
    val rawTitle: String,
    val rawBody: String,
    val packageName: String
)

object YapeNotificationParser {

    private val KNOWN_PACKAGES = listOf(
        "com.bcp.innovacxion.yapeapp",
        "com.bcp.innovacxion.yape",
        "com.bcp.yape",
        "pe.interbank.plin",
        "pe.com.scotiabank.bancamovil"
    )

    // Regex 1: "Yape! [Nombre] te envió [un pago por] S/ [Monto]" (with optional exclamation marks, accents, etc.)
    private val REGEX_STANDARD_YAPE = Pattern.compile(
        """(?:¡?Yape!?\s*)?(.+?)\s+te\s+(?:ha\s+)?envi[oó]\s+(?:un\s+pago\s+por\s+)?S/\.?\s*([0-9]+(?:[.,][0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex 2: "[Nombre] te yapeó S/ [Monto]"
    private val REGEX_YAPEO = Pattern.compile(
        """(.+?)\s+te\s+(?:acaba\s+de\s+)?yape[oó]\s+S/\.?\s*([0-9]+(?:[.,][0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex 3: "Te acaban de yapear S/ [Monto] de [Nombre]"
    private val REGEX_TE_ACABAN_YAPEAR = Pattern.compile(
        """Te\s+acaban?\s+de\s+yapear\s+S/\.?\s*([0-9]+(?:[.,][0-9]{1,2})?)(?:\s+de\s+(.+))?""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex 4: Plin pattern: "Plin! [Nombre] te envió S/ [Monto]"
    private val REGEX_PLIN = Pattern.compile(
        """(?:¡?Plin!?\s*)?(.+?)\s+te\s+(?:envi[oó]|transfiri[oó])\s+(?:un\s+pago\s+por\s+)?S/\.?\s*([0-9]+(?:[.,][0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    // Fallback amount pattern: finds "S/ 20", "S/. 20.00", "S/20"
    private val REGEX_AMOUNT_FALLBACK = Pattern.compile(
        """S/\.?\s*([0-9]+(?:[.,][0-9]{1,2})?)""",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Normalizes text by replacing non-breaking spaces, NBSP (\u00A0), multiple spaces, and trimming.
     */
    fun normalizeText(input: String): String {
        return input.replace('\u00A0', ' ')
            .replace('\u2007', ' ')
            .replace('\u202F', ' ')
            .replace('\u200B', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Checks if this notification appears to be related to Yape or digital wallet payment.
     */
    fun isPotentialPaymentNotification(packageName: String, title: String, text: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val combined = "$title $text".lowercase()

        val isTargetPackage = KNOWN_PACKAGES.any { lowerPkg.contains(it) } || lowerPkg.contains("yape")
        val mentionsYape = combined.contains("yape")
        val mentionsPayment = combined.contains("te envió") || 
                              combined.contains("te envio") || 
                              combined.contains("te yapeó") || 
                              combined.contains("te yapeo") || 
                              combined.contains("pago por s/") || 
                              combined.contains("confirmación de pago") ||
                              combined.contains("confirmacion de pago") ||
                              combined.contains("te transfirió") ||
                              combined.contains("te transfirio")

        return isTargetPackage || mentionsYape || mentionsPayment
    }

    /**
     * Parses notification content into a structured ParsedPayment or null if not a payment confirmation.
     */
    fun parse(packageName: String, rawTitle: String, rawText: String): ParsedPayment? {
        val title = normalizeText(rawTitle)
        val text = normalizeText(rawText)
        val combined = if (text.isNotBlank()) text else title
        if (combined.isBlank()) return null

        // 1. Try standard Yape message
        val m1 = REGEX_STANDARD_YAPE.matcher(combined)
        if (m1.find()) {
            val senderRaw = m1.group(1)?.trim() ?: "Desconocido"
            val amountStr = m1.group(2)?.replace(',', '.') ?: "0"
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            if (amount > 0.0) {
                return ParsedPayment(
                    sender = cleanSenderName(senderRaw),
                    amount = amount,
                    currency = "PEN",
                    rawTitle = rawTitle,
                    rawBody = rawText,
                    packageName = packageName
                )
            }
        }

        // 2. Try "X te yapeó S/ Y"
        val m2 = REGEX_YAPEO.matcher(combined)
        if (m2.find()) {
            val senderRaw = m2.group(1)?.trim() ?: "Desconocido"
            val amountStr = m2.group(2)?.replace(',', '.') ?: "0"
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            if (amount > 0.0) {
                return ParsedPayment(
                    sender = cleanSenderName(senderRaw),
                    amount = amount,
                    currency = "PEN",
                    rawTitle = rawTitle,
                    rawBody = rawText,
                    packageName = packageName
                )
            }
        }

        // 3. Try "Te acaban de yapear S/ Y de X"
        val m3 = REGEX_TE_ACABAN_YAPEAR.matcher(combined)
        if (m3.find()) {
            val amountStr = m3.group(1)?.replace(',', '.') ?: "0"
            val senderRaw = m3.group(2)?.trim() ?: "Desconocido"
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            if (amount > 0.0) {
                return ParsedPayment(
                    sender = cleanSenderName(senderRaw),
                    amount = amount,
                    currency = "PEN",
                    rawTitle = rawTitle,
                    rawBody = rawText,
                    packageName = packageName
                )
            }
        }

        // 4. Try Plin
        val m4 = REGEX_PLIN.matcher(combined)
        if (m4.find()) {
            val senderRaw = m4.group(1)?.trim() ?: "Desconocido"
            val amountStr = m4.group(2)?.replace(',', '.') ?: "0"
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            if (amount > 0.0) {
                return ParsedPayment(
                    sender = cleanSenderName(senderRaw),
                    amount = amount,
                    currency = "PEN",
                    rawTitle = rawTitle,
                    rawBody = rawText,
                    packageName = packageName
                )
            }
        }

        // 5. Fallback: If it mentions payment or Yape and contains S/ amount
        if (isPotentialPaymentNotification(packageName, title, text)) {
            val amountMatcher = REGEX_AMOUNT_FALLBACK.matcher(combined)
            if (amountMatcher.find()) {
                val amountStr = amountMatcher.group(1)?.replace(',', '.') ?: "0"
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                if (amount > 0.0) {
                    val senderCandidate = extractSenderFallback(combined, title)
                    return ParsedPayment(
                        sender = senderCandidate,
                        amount = amount,
                        currency = "PEN",
                        rawTitle = rawTitle,
                        rawBody = rawText,
                        packageName = packageName
                    )
                }
            }
        }

        return null
    }

    private fun cleanSenderName(raw: String): String {
        return raw.replace(Regex("""^¡?Yape!?:?\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+te\s+.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\.\.\.|…"""), "")
            .replace(Regex("""[^\p{L}\p{Nd}\s.]"""), "")
            .trim()
            .ifEmpty { "Cliente Yape" }
    }

    private fun extractSenderFallback(text: String, title: String): String {
        if (title.isNotBlank() && !title.equals("Confirmación de Pago", ignoreCase = true) && !title.equals("Yape", ignoreCase = true)) {
            return cleanSenderName(title.trim())
        }
        val beforeTeEnvio = text.substringBefore("te envi", "").substringBefore("te yape", "")
        val cleaned = cleanSenderName(beforeTeEnvio)
        return cleaned.ifEmpty { "Cliente Yape" }
    }
}
