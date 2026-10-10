# Handover — AppBaboonsSellers v1.7.0-staging (10/10/2026)

Estado: **staging funcionando end-to-end**. Login → sync → ruta → pedidos → visitas → cobros → refuerzos → tracking GPS → novedades → gastos, todo probado en celu físico contra `multinegociobaboons-staging.fly.dev`.

## Ambientes

| Ambiente | App | Backend | Base (Neon) |
|---|---|---|---|
| Staging | build **debug** (`versionName *-staging`) | `multinegociobaboons-staging.fly.dev` | branch propia (`ep-holy-frost-...`, en `fly.staging.toml`) |
| Producción | build **release** | `multinegociobaboons-fly.fly.dev` | prod |

- `BASE_URL` en `ApiClient.kt` está spliteada por buildType: debug → staging, release → prod.
- **Prod NO está deployado**: todo lo de esta fase vive solo en staging/main.
- Deploy: `fly deploy -c fly.staging.toml` desde `C:\Users\usuario\Documents\MultinegocioBaboons` (el usuario deploya, nunca el agente).

## Descarga del APK

- GitHub Releases: https://github.com/ElChesca/AppBaboonsSellers/releases
- Link fijo en el `README.md` del repo (apunta al último).
- Cada build nuevo: `gh release create vX.Y.Z-staging <apk>` — nombrar el asset `baboons-sellers-X.Y.Z-staging.apk`.

## Lo que se hizo

### App (repo AppBaboonsSellers, todo en `main`)

| Feature | Detalle |
|---|---|
| Login + Sync | JWT del ERP → snapshot Room (clientes, productos, HR, pedidos). |
| Tracking GPS | `LocationTrackingService` foreground, cada 60s, desde abrir la app con sesión hasta logout. Cola offline en Room (`posiciones_pendientes`). El chofer ve su rastro en Mapa de Ruta; admin lo ve en la web (polyline naranja). |
| Pedidos | Crear pedido desde card de cliente (carrito) o detalle. Offline-first (Room → `sync/pedidos-offline`). |
| Marcar Visitado (ojito) | `PUT /api/hoja_ruta/{hr}/item/{itemId}` — marca local + avisa al server; revierte si falla. |
| Resultado de visita | Chips Entregado/No estaba/Parcial/Rechazado + obs → visitado + observación al ERP; queda en la card y limpia el form; vuelve al Home solo. |
| Consultar Stock + Pedir Refuerzo | Stock del depósito (snapshot) + stepper → `POST /hojas-ruta/{id}/refuerzos` → pestaña "Alertas de Refuerzo" + notificación. |
| COBRAR | Detalle cliente → monto + Efectivo/Transferencia → `POST /hojas-ruta/{id}/cobrar-cc` → `ventas_cobros_pendientes` estado **borrador** (admin confirma). |
| Resumen de Jornada | KPIs visitados/pedidos/$ + listas por sección, filtrado por ruta seleccionada. Cobros guardados en Room (`cobros_realizados`, DB v11). |
| Reportar Novedad | **Modal en el Home** (no pantalla aparte): tipo (camión roto / cliente cerrado / dirección incorrecta / demora / otro) + cliente opcional + obs → `POST /hojas-ruta/{id}/novedades` → notificación + tab "Novedades" web. |
| Registrar Gasto | Modal: monto + categorías (las 7 de la web del chofer) + obs → `POST /hojas-ruta/{id}/gastos` → `gastos_operativos` con `hoja_ruta_id`; aparece en Rendición de Gastos de la liquidación. |
| UX | Cards clickeables (tap → detalle, carrito → pedido), logo D-Co (launcher + drawer), `navigationBarsPadding` en footers, ruta mostrada como `numero_interno` (#190). |

### ERP (repo MultinegocioBaboons, PRs mergeados #82–#88)

| PR | Qué |
|---|---|
| #82 | Registra blueprint `sync_routes` (fix 404 snapshot) + claim `negocio_nombre` en JWT |
| #83 | `vendedor_id` en JWT para roles no-vendedor (chofer) — fallback por email en tabla `vendedores` |
| #84 | Tracking: tabla `vehiculo_posiciones`, `POST /mobile/tracking`, `GET /mobile/mi-rastro`, `GET /vehiculos/{id}/rastro`, polyline en mapa HR web |
| #85 | Snapshot incluye pedidos de la HR activa aunque tengan `vendedor_id NULL` |
| #86 | `numero_interno` en snapshot y `hojas-ruta-activas` |
| #87 | `cobrar-cc` acepta HR en estado `activa` (además de `en_ruta`) |
| #88 | Novedades: `POST /hojas-ruta/{id}/novedades`, `GET /negocios/{id}/hojas-ruta/novedades/activas`, `PUT /hojas-ruta/novedades/{id}/estado`, tab "Novedades" en `hoja_ruta.html`, evento `NOVEDAD_RUTA` en config notificaciones |

## Puntos clave de integración

- **Auth**: `@token_required` en todo. El JWT lleva `negocio_id`, `vendedor_id`, `rol`. Chofer (`rol='chofer'`) resuelve `vendedor_id` por email → tabla `vendedores`.
- **HR**: reparto normal queda en estado `activa` durante toda la ruta; `en_ruta` es autoventa. Endpoints del chofer aceptan ambas.
- **IDs**: la app guarda `hoja_ruta_clientes.item_id` (= `hoja_ruta_items.id`) — lo usa el PUT de visita. La UI muestra `numero_interno`, no el `id`.
- **Notificaciones**: `emitir_notificacion_evento(evento_codigo)` solo dispara si hay regla en `erp_notificaciones_config` para el negocio (se configura en Config de Notificaciones). Novedad tiene fallback a roles `admin/superadmin` si no hay regla.
- **SQL manuales correr en Neon** (ya entregados):
  - `tracking_chofer_migration.sql` → tabla `vehiculo_posiciones`
  - `novedades_ruta_migration.sql` → tabla `hoja_ruta_novedades`
- **Migraciones ERP**: siempre manuales (`python db_migrate.py`), nunca automáticas.
- **Room DB**: v11. Al subir version con cambios de schema, usuarios reinstalan o hacen clear-data (destructive migration está OK para este uso).

## Gotchas aprendidos (¡importantes!)

1. **`hiltViewModel()` = una instancia por NavBackStackEntry**. Estado compartido entre pantallas NO va en el VM: la ruta seleccionada vive en `companion object` (`_sharedSelectedHojaRutaId`) + `TokenStore` como respaldo. Por eso Novedad/Gasto son **modales dentro del Home** (mismo VM que muestra la ruta). Si hacés una pantalla nueva que necesite la ruta, leé `_sharedSelectedHojaRutaId`/`TokenStore.getHojaRutaId`, no un VM nuevo.
2. **Edge-to-edge**: `enableEdgeToEdge()` → todo footer necesita `Modifier.navigationBarsPadding()` o la botonera del sistema lo tapa.
3. **Versionado app**: bump en 3 lugares juntos — `versionCode`/`versionName` en `build.gradle.kts` + texto footer en `LoginScreen.kt` y `HomeScreen.kt`.
4. **Versionado ERP**: `APP_VERSION` sincronizada en `main.js`, `service-worker.js` e `index.html` (ahora 1.20.434).
5. **Sync rebuild preserva estado local**: `visitado`/`resultado_visita` se mantienen al re-sincronizar (no se pisan con el snapshot).
6. Pedidos cargados desde el módulo HR web salen con `vendedor_id NULL` — por eso el sync trae "todo lo de la HR activa", no solo lo del vendedor.

## Lo que falta

- **Refuerzo en dos vías**: que el chofer vea el estado de su pedido de refuerzo (pendiente/preparando/listo) — `GET /negocios/{id}/hojas-ruta/refuerzos/activos` ya existe; falta pantalla/item en la app.
- **Deploy a prod**: `fly deploy` en la máquina de producción + build release de la app. Todo lo anterior validado en staging.
- Opcional: estampar `vendedor_id` al crear pedidos desde el módulo HR (quedan NULL hoy — la app ya los tolera).
- Opcional: botón "Iniciar/Finalizar jornada" para el tracking en vez de automático al abrir.
- Handover viejo con detalle de arquitectura: `MVP_HANDOVER_v1.3.5.md` en el repo.
