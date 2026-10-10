# MVP Handover Documentation - App Baboons Sellers (v1.3.5)

Este documento es para que el próximo agente de IA tome el control del proyecto sin repetir errores pasados.

## 🚀 Estado del Proyecto (24/03/2026)
- **Versión Actual**: `1.3.5` (Android App).
- **Backend**: Desplegado en Fly.io (`multinegociobaboons-fly`).
- **Logros**:
  - Nombre del negocio 100% dinámico (se actualiza en el TopAppBar al sincronizar).
  - Navigation Drawer integrado y funcional.
  - Sincronización de Hojas de Ruta y Clientes (mapping local en Room v5).
  - Backend preparado para capturar `hoja_ruta_id` en pedidos offline.

## 🛠️ Stack Tecnológico
- **Frontend**: Kotlin, Jetpack Compose, Room (v5), Retrofit, Dagger Hilt.
- **Backend**: Python (Flask + SQLAlchemy/Psychopg2), PostgreSQL en Fly.io.
- **Herramientas**: Fly.io CLI (`fly deploy`, `fly logs`).

## ⚠️ Bloqueos Actuales y "Gotchas"
- **"Mis Pedidos" (Historial)**: El servidor ya envía los últimos 100 pedidos (`pedidos_recientes`) en el JSON de `syncSnapshot`. Sin embargo, **no aparecen en la pantalla "Mis Pedidos"**.
  - **Causa probable**: Verificar si `SyncRepository.kt` está guardando bien los datos con `estado_sync="sincronizado"`.
  - **Ojo**: PostgreSQL devuelve fechas como Float (epoch), pero Room espera `Long`. El fix para el backend ya está hecho (`::BIGINT`) pero hay que asegurar que el mapeo en `ApiClient.kt` sea correcto.
- **Navegación**: El `hoja_ruta_id` se pasa entre pantallas mediante `SavedStateHandle` en los ViewModels.

## 📂 Archivos Críticos
- `app/src/main/java/com/baboons/sellers/data/repository/SyncRepository.kt`: Lógica de sincronización maestra.
- `app/src/main/java/com/baboons/sellers/ui/screens/HomeScreen.kt`: Pantalla principal y selector de rutas.
- `app/src/main/java/com/baboons/sellers/ui/screens/MisPedidosScreen.kt`: Filtrado de pedidos por ruta.
- `app/routes/sync_routes.py` (Backend): Ruta `/sync/snapshot` y `/sync/pedidos-offline`.

## 💻 Comandos Útiles
- **Desplegar Backend**: `fly deploy` en `C:\Users\usuario\Documents\MultinegocioBaboons`.
- **Ver Logs Backend**: `fly logs --app multinegociobaboons-fly`.
- **Sync DB**: Recordar que `versionName="1.3.5"` y `versionCode=3`.

---

**Nota para el siguiente agente**: El usuario está agotado por problemas de sincronización de datos y falta de comunicación visual. Sé preciso con los cambios de código y SIEMPRE verifica que los números de versión se actualicen para darle seguridad al usuario.
