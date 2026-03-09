# Guia Funcional - Sistema de Pedidos de Comida

Este documento explica como funciona el sistema, que eventos se disparan, como se comunican los servicios, que bases de datos se actualizan y como probar todo con Postman.

## Resumen del Flujo

El flujo combina acciones manuales del usuario con procesamiento automatico via Kafka:

```
1. Cliente hace login              → user-service genera JWT (incluye userId en el token)
2. Cliente crea un pedido          → order-service valida producto via REST, obtiene precio
3. order-service guarda            → orderdb (status: PENDING)
4. order-service publica           → Kafka topic: orders.events
5. payment-service consume         → crea pago en paymentdb (status: PENDING) ← NO aprueba aun
6. Cliente paga manualmente        → POST /api/payments/{orderId}/pay
7. payment-service aprueba         → paymentdb (status: APPROVED), publica Kafka payments.events
8. order-service consume           → actualiza orderdb (status: PAID)
9. delivery-service consume        → crea entrega en deliverydb (status: IN_TRANSIT)
10. delivery-service publica       → Kafka topic: deliveries.events
11. Repartidor marca entregado     → PUT /api/deliveries/{id}/status {"status":"DELIVERED"}
12. delivery-service publica       → Kafka topic: deliveries.events (DELIVERED)
13. order-service consume          → actualiza orderdb (status: DELIVERED)
14. notification-service consume   → 4 notificaciones: ORDER_CREATED, PAYMENT_APPROVED, DELIVERY_STARTED, DELIVERY_COMPLETED
```

**Ciclo de vida del pedido:** `PENDING → PAID → DELIVERED`

## Comunicacion entre Servicios

### Comunicacion Sincrona (REST)

| Origen | Destino | Que hace |
|--------|---------|----------|
| product-service | user-service | Valida que un usuario exista (via RestTemplate + circuit breaker) |
| order-service | product-service | Valida que un producto exista y obtiene el precio unitario (via RestTemplate + circuit breaker) |

### Comunicacion Asincrona (Kafka)

| Topic | Quien publica | Quien consume | Evento |
|-------|--------------|---------------|--------|
| orders.events | order-service | payment-service, notification-service | OrderCreatedEvent (orderId, userId, amount) |
| payments.events | payment-service | order-service, delivery-service, notification-service | PaymentProcessedEvent (orderId, status, paymentId) |
| deliveries.events | delivery-service | order-service, notification-service | DeliveryEvent (orderId, status, deliveryId) |

## Bases de Datos - Que se actualiza en cada paso

| Paso | Base de Datos | Tabla | Que se escribe |
|------|--------------|-------|----------------|
| Login | userdb | users | Solo lectura (valida credenciales) |
| Crear producto | productdb | products | Nuevo producto (createdBy tomado del JWT) |
| Crear pedido | orderdb | orders + order_items | Pedido con status PENDING (userId del JWT, precio de product-service) |
| Pago registrado (auto) | paymentdb | payments | Pago con status PENDING |
| Pago aprobado (manual) | paymentdb | payments | Actualiza status a APPROVED |
| Pedido pagado | orderdb | orders | Actualiza status a PAID |
| Entrega creada (auto) | deliverydb | deliveries | Entrega con status IN_TRANSIT |
| Entrega completada (manual) | deliverydb | deliveries | Actualiza status a DELIVERED |
| Pedido entregado | orderdb | orders | Actualiza status a DELIVERED |
| Notificaciones | notificationdb | notifications | 4 registros (ORDER_CREATED, PAYMENT_APPROVED, DELIVERY_STARTED, DELIVERY_COMPLETED) |

## Seguridad (JWT)

- **user-service** genera tokens JWT al hacer login. El token incluye: email (subject), roles y userId
- **product-service** y **order-service** validan tokens JWT en cada peticion
- **product-service** extrae el userId del JWT al crear productos (campo createdBy automatico)
- **order-service** extrae el userId del JWT al crear pedidos (no se pide en el body)
- **payment-service** y **delivery-service** validan tokens JWT en sus endpoints REST
- **notification-service** no tiene seguridad JWT (solo expone GET de notificaciones)
- El secreto JWT es compartido entre user-service, product-service, order-service, payment-service y delivery-service
- Los tokens expiran en 1 hora (Kubernetes) o 24 horas (local)

### Roles

| Rol | Que puede hacer |
|-----|-----------------|
| ADMIN | Todo: crear/editar/eliminar usuarios y productos, crear pedidos, pagar, gestionar entregas |
| USER | Crear pedidos, pagar, ver entregas |

## Resiliencia (Circuit Breaker + Retry)

- **product-service** tiene circuit breaker + retry en las llamadas a user-service (UserClient)
- **order-service** tiene circuit breaker + retry en las llamadas a product-service (ProductClient)
- Si el servicio destino no responde despues de 3 reintentos, se activa el fallback
- Configuracion: ventana de 10 llamadas, umbral de fallo 50%, circuito abierto por 10 segundos

## Observabilidad (Prometheus + Grafana + Zipkin)

Para levantar el stack de observabilidad:

```bash
docker compose -f docker-compose-observability.yml up -d
```

| Herramienta | URL | Credenciales | Que ves |
|-------------|-----|-------------|---------|
| Prometheus | http://localhost:9090 | - | Targets, queries, metricas de los 6 servicios |
| Grafana | http://localhost:3000 | admin / admin | Dashboards, graficas de metricas |
| Zipkin | http://localhost:9411 | - | Trazas distribuidas |
| Kafka UI | http://localhost:8090 | - | Topics, mensajes, consumers |

### Verificar en Prometheus

1. Abrir http://localhost:9090/targets → los 6 servicios deben estar en estado `UP`
2. En la barra de queries escribir `jvm_memory_used_bytes` y ejecutar → metricas de memoria de cada servicio
3. Probar `http_server_requests_seconds_count` → contadores de peticiones HTTP por servicio

### Verificar en Grafana

1. Abrir http://localhost:3000 (usuario: admin, password: admin)
2. Ir a Connections > Data Sources → Prometheus ya esta configurado automaticamente
3. Ir a Explore → seleccionar Prometheus → escribir `jvm_memory_used_bytes` → Run query

### Endpoints de Actuator (publicos en todos los servicios)

```
GET /actuator/health     → Estado del servicio (UP/DOWN)
GET /actuator/info       → Informacion del servicio
GET /actuator/prometheus → Metricas en formato Prometheus (usado por Prometheus para scrapear)
```

---

## Pruebas con Postman

### URLs segun el entorno

| Entorno | Base URL |
|---------|----------|
| Local (Maven) | http://localhost:{puerto} |
| Kubernetes | http://localhost:{nodeport} |

Puertos locales: 8081-8086. NodePorts: 30081-30086.

Los ejemplos usan los NodePorts de Kubernetes. Si corres local, cambia 30081 por 8081, etc.

---

### PASO 1: Login

Obtener un token JWT para autenticarse. El token ahora incluye el userId del usuario.

```
POST http://localhost:30081/api/auth/login
Content-Type: application/json

{
    "email": "juan.perez@example.com",
    "password": "admin123"
}
```

**Respuesta:**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJST0xFX0FETUlOIl0sInVzZXJJZCI6MS...",
    "type": "Bearer",
    "email": "juan.perez@example.com",
    "roles": ["ROLE_ADMIN"]
}
```

El payload del JWT contiene: `{"roles":["ROLE_ADMIN"],"userId":1,"sub":"juan.perez@example.com",...}`

Copia el valor de `token`. Lo usaras en todas las peticiones siguientes como header:
```
Authorization: Bearer <token>
```

---

### PASO 2: Ver productos existentes (no requiere token)

```
GET http://localhost:30082/api/products
```

**Respuesta:** Lista de 5 productos precargados (Laptop Dell, Mouse Logitech, etc.)

---

### PASO 3: Crear un producto (requiere token ADMIN)

Ya NO se envia `createdBy` en el body. Se extrae automaticamente del JWT (userId del usuario autenticado).

```
POST http://localhost:30082/api/products
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Pizza Margherita",
    "description": "Pizza italiana con tomate y mozzarella",
    "price": 25.50,
    "stock": 100,
    "category": "PIZZA"
}
```

**Respuesta:**
```json
{
    "id": 8,
    "name": "Pizza Margherita",
    "description": "Pizza italiana con tomate y mozzarella",
    "price": 25.50,
    "stock": 100,
    "category": "PIZZA",
    "createdBy": 1,
    "available": true
}
```

Nota: `createdBy: 1` se obtuvo del claim `userId` del JWT, no del body.

---

### PASO 4: Crear un pedido (requiere token USER o ADMIN)

Este es el paso que dispara toda la cadena de eventos Kafka.

Ya NO se envia `price` en los items. Solo se envia `productId` y `quantity`. El order-service:
1. Extrae el `userId` del JWT (no se pide en el body)
2. Llama al product-service via REST para validar que el producto existe
3. Obtiene el precio unitario del product-service
4. Verifica que haya stock suficiente
5. Calcula el total y guarda el pedido

```
POST http://localhost:30083/api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
    "items": [
        {
            "productId": 8,
            "quantity": 2
        }
    ]
}
```

**Respuesta:**
```json
{
    "id": 3,
    "userId": 1,
    "status": "PENDING",
    "totalAmount": 51.00,
    "items": [
        { "id": 7, "productId": 8, "quantity": 2, "price": 25.50 }
    ],
    "createdAt": "2026-03-09T03:09:45",
    "updatedAt": "2026-03-09T03:09:45"
}
```

Nota: `userId: 1` se obtuvo del JWT. `price: 25.50` se obtuvo del product-service. `totalAmount: 51.00` = 25.50 x 2.

**Lo que pasa internamente (en ~5 segundos):**
1. order-service llama a product-service para validar producto y obtener precio
2. Se guarda el pedido en orderdb con status PENDING
3. Se publica OrderCreatedEvent en Kafka
4. payment-service consume el evento y crea un pago con status **PENDING** (NO lo aprueba automaticamente)
5. notification-service consume el evento y crea notificacion ORDER_CREATED

**El pedido queda en status PENDING esperando que el cliente pague.**

---

### PASO 5: Ver el pago pendiente (requiere token)

```
GET http://localhost:30084/api/payments/order/{orderId}
Authorization: Bearer <token>
```

**Respuesta:**
```json
{
    "id": 1,
    "orderId": 1,
    "amount": 51.00,
    "status": "PENDING"
}
```

---

### PASO 6: Pagar el pedido manualmente (requiere token)

Este es el paso clave. El pago NO se aprueba automaticamente.

```
POST http://localhost:30084/api/payments/{orderId}/pay
Authorization: Bearer <token>
```

**Respuesta:**
```json
{
    "id": 1,
    "orderId": 1,
    "amount": 51.00,
    "status": "APPROVED"
}
```

**Lo que pasa internamente (en ~5 segundos):**
1. payment-service cambia el pago a APPROVED y publica PaymentProcessedEvent en Kafka
2. order-service consume el evento y cambia el pedido a PAID
3. delivery-service consume el evento y crea la entrega con status IN_TRANSIT
4. notification-service consume el evento y crea notificacion PAYMENT_APPROVED y DELIVERY_STARTED

---

### PASO 7: Verificar que el pedido cambio a PAID (requiere token)

```
GET http://localhost:30083/api/orders/{orderId}
Authorization: Bearer <token>
```

**Respuesta:** El campo `status` debe ser `"PAID"`.

---

### PASO 8: Ver la entrega creada (requiere token)

```
GET http://localhost:30085/api/deliveries/order/{orderId}
Authorization: Bearer <token>
```

**Respuesta:**
```json
{
    "id": 1,
    "orderId": 1,
    "address": "Direccion de entrega",
    "status": "IN_TRANSIT"
}
```

---

### PASO 9: Marcar la entrega como DELIVERED (requiere token)

Cuando el repartidor entrega el pedido:

```
PUT http://localhost:30085/api/deliveries/{deliveryId}/status
Authorization: Bearer <token>
Content-Type: application/json

{
    "status": "DELIVERED"
}
```

**Respuesta:**
```json
{
    "id": 1,
    "orderId": 1,
    "address": "Direccion de entrega",
    "status": "DELIVERED"
}
```

**Lo que pasa internamente (en ~5 segundos):**
1. delivery-service publica DeliveryEvent (DELIVERED) en Kafka
2. order-service consume el evento y cambia el pedido a DELIVERED
3. notification-service consume el evento y crea notificacion DELIVERY_COMPLETED

---

### PASO 10: Verificar que el pedido cambio a DELIVERED (requiere token)

```
GET http://localhost:30083/api/orders/{orderId}
Authorization: Bearer <token>
```

**Respuesta:** El campo `status` debe ser `"DELIVERED"`.

---

### PASO 11: Ver todas las notificaciones (sin token, unico endpoint publico)

```
GET http://localhost:30086/api/notifications
```

**Respuesta:**
```json
[
    { "id": 1, "userId": 1, "message": "Tu pedido #1 ha sido creado", "type": "ORDER_CREATED" },
    { "id": 2, "userId": null, "message": "Pago aprobado para pedido #1", "type": "PAYMENT_APPROVED" },
    { "id": 3, "userId": null, "message": "Tu pedido #1 esta en camino", "type": "DELIVERY_STARTED" },
    { "id": 4, "userId": null, "message": "Tu pedido #1 ha sido entregado", "type": "DELIVERY_COMPLETED" }
]
```

---

### PASO 12: Verificar los eventos en Kafka UI

Abre http://localhost:8090 en el navegador. Ahi puedes ver:

- Topic `orders.events` - OrderCreatedEvent
- Topic `payments.events` - PaymentProcessedEvent (solo despues de pagar manualmente)
- Topic `deliveries.events` - DeliveryEvent (IN_TRANSIT al crearse, DELIVERED al marcar entregado)

---

### Todos los endpoints disponibles

**User Service (30081):**
```
GET  /api/users              → Lista usuarios (ADMIN)
GET  /api/users/{id}         → Usuario por ID (ADMIN)
POST /api/users              → Crear usuario (ADMIN)
PUT  /api/users/{id}         → Actualizar usuario (ADMIN)
DELETE /api/users/{id}       → Eliminar usuario (ADMIN)
GET  /api/users/health       → Health check (publico)
```

**Product Service (30082):**
```
GET  /api/products           → Lista productos (publico)
GET  /api/products/available → Productos con stock (publico)
GET  /api/products/{id}      → Producto por ID (publico, usado por order-service via REST)
POST /api/products           → Crear producto (ADMIN, createdBy del JWT)
PUT  /api/products/{id}      → Actualizar producto (ADMIN)
DELETE /api/products/{id}    → Eliminar producto (ADMIN)
GET  /api/products/health    → Health check (publico)
```

**Order Service (30083):**
```
POST /api/orders             → Crear pedido (USER o ADMIN, userId del JWT, valida productos via REST)
GET  /api/orders/{id}        → Ver pedido (USER o ADMIN)
GET  /api/orders/health      → Health check (publico)
```

**Payment Service (30084):**
```
GET  /api/payments           → Lista todos los pagos (USER o ADMIN)
GET  /api/payments/order/{orderId} → Ver pago por orderId (USER o ADMIN)
POST /api/payments/{orderId}/pay   → Aprobar pago manualmente (USER o ADMIN)
GET  /api/payments/health    → Health check (publico)
```

**Delivery Service (30085):**
```
GET  /api/deliveries                → Lista todas las entregas (USER o ADMIN)
GET  /api/deliveries/order/{orderId} → Ver entrega por orderId (USER o ADMIN)
PUT  /api/deliveries/{id}/status    → Actualizar status de entrega (USER o ADMIN)
GET  /api/deliveries/health         → Health check (publico)
```

**Notification Service (30086):**
```
GET  /api/notifications      → Lista todas las notificaciones (publico)
```

**Actuator (todos los servicios):**
```
GET  /actuator/health        → Estado del servicio
```

---

## Checklist de Verificacion

Despues de ejecutar el flujo completo:

- [ ] Login exitoso, token recibido (con userId en claims)
- [ ] Producto creado correctamente (createdBy tomado del JWT)
- [ ] Pedido creado con status PENDING, total calculado con precio de product-service, userId del JWT
- [ ] Pago registrado automaticamente con status PENDING
- [ ] Pago aprobado manualmente via POST /api/payments/{orderId}/pay
- [ ] Pedido actualizado a PAID (despues de pagar)
- [ ] Entrega creada automaticamente con status IN_TRANSIT (despues de pagar)
- [ ] Entrega marcada como DELIVERED via PUT /api/deliveries/{id}/status
- [ ] Pedido actualizado a DELIVERED (despues de entregar)
- [ ] 4 notificaciones creadas: ORDER_CREATED, PAYMENT_APPROVED, DELIVERY_STARTED, DELIVERY_COMPLETED
- [ ] Eventos visibles en Kafka UI (http://localhost:8090)

## Ver logs de un servicio en Kubernetes

```bash
kubectl logs -n order-service deployment/order-service --tail=30
kubectl logs -n payment-service deployment/payment-service --tail=30
kubectl logs -n delivery-service deployment/delivery-service --tail=30
kubectl logs -n notification-service deployment/notification-service --tail=30
```
