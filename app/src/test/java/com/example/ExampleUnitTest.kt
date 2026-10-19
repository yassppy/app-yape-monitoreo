package com.example

import com.example.parser.YapeNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testParseUserScreenshotNotification() {
        val packageName = "com.bcp.innovacxion.yapeapp"
        val title = "Confirmación de Pago"
        val text = "Yape! CYNTHIA PAMELA SABIN MALLQUI te envió un pago por S/ 20"

        assertTrue(YapeNotificationParser.isPotentialPaymentNotification(packageName, title, text))

        val parsed = YapeNotificationParser.parse(packageName, title, text)
        assertNotNull(parsed)
        assertEquals("CYNTHIA PAMELA SABIN MALLQUI", parsed?.sender)
        assertEquals(20.0, parsed?.amount ?: 0.0, 0.001)
        assertEquals("PEN", parsed?.currency)
    }

    @Test
    fun testParseDecimalAmount() {
        val packageName = "com.bcp.innovacxion.yapeapp"
        val title = "Confirmación de Pago"
        val text = "Yape! JUAN CARLOS PEREZ te envió un pago por S/ 35.50"

        val parsed = YapeNotificationParser.parse(packageName, title, text)
        assertNotNull(parsed)
        assertEquals("JUAN CARLOS PEREZ", parsed?.sender)
        assertEquals(35.50, parsed?.amount ?: 0.0, 0.001)
    }

    @Test
    fun testParseYapeoAlternativeFormat() {
        val packageName = "com.bcp.innovacxion.yapeapp"
        val title = "Yape"
        val text = "MARIA ROJAS te yapeó S/ 50"

        val parsed = YapeNotificationParser.parse(packageName, title, text)
        assertNotNull(parsed)
        assertEquals("MARIA ROJAS", parsed?.sender)
        assertEquals(50.0, parsed?.amount ?: 0.0, 0.001)
    }

    @Test
    fun testIgnoreNonPaymentNotification() {
        val packageName = "com.google.android.gm"
        val title = "Nuevo correo de trabajo"
        val text = "Hola, revisa el archivo adjunto por favor."

        val isPayment = YapeNotificationParser.isPotentialPaymentNotification(packageName, title, text)
        assertEquals(false, isPayment)

        val parsed = YapeNotificationParser.parse(packageName, title, text)
        assertNull(parsed)
    }
}
