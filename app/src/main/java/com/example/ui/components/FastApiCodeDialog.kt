package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val FASTAPI_SAMPLE_CODE = """
# main.py - Servidor FastAPI para recibir notificaciones de Yape en tiempo real
from fastapi import FastAPI, Header, HTTPException, status
from pydantic import BaseModel
from typing import Optional
import uvicorn
from datetime import datetime

app = FastAPI(title="Yape Webhook Receiver", version="1.0.0")

class YapePaymentPayload(BaseModel):
    event: str
    payment_id: int
    sender: str
    amount: float
    currency: str = "PEN"
    timestamp: int
    datetime_iso: str
    raw_title: str
    raw_message: str
    app_source: str
    device_name: str

@app.post("/api/yape-webhook")
async def receive_yape_payment(
    payload: YapePaymentPayload,
    x_api_key: Optional[str] = Header(None),
    authorization: Optional[str] = Header(None)
):
    print("=" * 50)
    print(f"💰 ¡NUEVO PAGO YAPE RECIBIDO!")
    print(f"👤 Remitente: {payload.sender}")
    print(f"💵 Monto: {payload.currency} {payload.amount:.2f}")
    print(f"🕒 Fecha: {payload.datetime_iso}")
    print(f"📱 Dispositivo: {payload.device_name}")
    print(f"💬 Mensaje Original: {payload.raw_message}")
    print("=" * 50)

    # Aquí puedes:
    # 1. Guardar en base de datos (PostgreSQL, SQLite, MongoDB)
    # 2. Despachar un WebSocket a tu Punto de Venta (POS)
    # 3. Marcar la orden o pedido como PAGADO
    # 4. Imprimir voucher térmico automáticamente

    return {
        "status": "success",
        "message": "Pago procesado correctamente",
        "payment_id": payload.payment_id,
        "amount_received": payload.amount,
        "processed_at": datetime.utcnow().isoformat()
    }

if __name__ == "__main__":
    # Ejecutar en 0.0.0.0 para recibir peticiones de la red local WiFi
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
""".trimIndent()

@Composable
fun FastApiCodeDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DataObject,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Código FastAPI Listo para Usar")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Copia este script en tu servidor Python para recibir los pagos en tiempo real con FastAPI y Uvicorn:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = FASTAPI_SAMPLE_CODE,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFA6E3A1),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Instalación de dependencias:\npip install fastapi uvicorn pydantic",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("FastAPI Code", FASTAPI_SAMPLE_CODE)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copiar Código")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
