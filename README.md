<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# MVP Yape Monitor

Aplicación Android que **monitorea las notificaciones de pagos recibidos en Yape**, guarda un historial y **reenvía cada pago en tiempo real a tu propio backend** (por ejemplo, FastAPI) mediante una petición HTTP POST.

Ver la app en AI Studio: https://ai.studio/apps/5cd33f8e-f78e-45d0-93bd-6d77dcfee351

## Características

- **Monitoreo:** detecta las notificaciones de pago de Yape (paquete `com.bcp.innovacxion.yapeapp`) y extrae remitente, monto y fecha.
- **Historial:** registro de los pagos detectados dentro de la app.
- **Webhook hacia tu backend:** envía cada pago como JSON por POST a la URL que configures.
- **Token secreto (API Key):** se envía en los headers `Authorization: Bearer <token>` y `X-API-Key: <token>`.
- **Probar conexión:** botón que envía un evento `test_ping` para verificar que tu servidor responde.
- **Atajos de URL:** rutas rápidas (`/webhook/yape`, `/api/yape-webhook`) y la IP especial del emulador (`10.0.2.2`).

## Cómo funciona

```
Yape (notificación) → Yape Monitor (Android) → POST JSON → Tu backend (FastAPI, n8n, etc.)
```

1. Concede a la app el permiso de **acceso a notificaciones** en Android.
2. En la pestaña **FastAPI**, ingresa la URL de tu webhook y tu token secreto, y guarda.
3. Pulsa **Probar Conexión** para validar.
4. Cada pago recibido en Yape se envía automáticamente a tu endpoint.

## Payload enviado

```json
{
  "event": "yape_payment_received",
  "payment_id": 1,
  "sender": "NOMBRE DEL REMITENTE",
  "amount": 1.0,
  "currency": "PEN",
  "timestamp": 1792428823116,
  "datetime_iso": "2026-10-18T22:45:00Z",
  "raw_title": "Título de la notificación",
  "raw_message": "Yape! NOMBRE te envió un pago por S/ 10.00",
  "app_source": "com.bcp.innovacxion.yapeapp",
  "device_name": "modelo del dispositivo"
}
```

El botón **Probar Conexión** envía el mismo formato con `"event": "test_ping"`. Tu backend debe responder con un código 2xx para que la app lo considere exitoso.

## Limitaciones

- Depende del formato de las notificaciones de Yape, si Yape cambia el texto, la extracción de datos puede fallar.
- Requiere que la app siga activa en segundo plano y con el permiso de notificaciones concedido.

## Fuera de funcionalidad

- No registra notificaciones de otras aplicaciones de pago como Plin entre otros.
- No se agrega autentificación es un MVP.
