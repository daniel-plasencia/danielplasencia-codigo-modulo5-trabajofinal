# Guia Funcional - Sistema de Pedidos de Comida

Este documento explica como funciona el sistema, que eventos se disparan, como se comunican los servicios, que bases de datos se actualizan y como probar todo con Postman.

## Resumen del Flujo

Cuando un cliente crea un pedido, ocurre lo siguiente de forma automatica:

```
1. Cliente hace login          → user-service genera JWT
2. Cliente crea un pedido      → order-service guarda en orderdb (status: PENDING)
3. order-service publica       → Kafka topic: orders.events
4. payment-service consume     → guarda pago en paymentdb (status: APPROVED)
5. payment-service publica     → Kafka topic: payments.events
6. order-service consume       → actualiza order en orderdb (status: PAID)
7. delivery-service consume    → guarda entrega en deliverydb (status: IN_TRANSIT)
8. delivery-service publica    → Kafka topic: deliveries.events
9. notification-service consume los 3 topics → guarda 3 notificaciones en notificationdb
```

## Comunicacion entre Servicios

### Comunicacion Sincrona (REST)

| Origen | Destino | Que hace |
|--------|---------|----------|
| product-service | user-service | Valida que un usuario exista (via RestTemplate) |

### Comunicacion Asincrona (Kafka)

| Topic | Quien publica | Quien consume | Evento |
|-------|--------------|---------------|--------|
| orders.events | order-service | payment-service, notification-service | OrderCreatedEvent (orderId, userId, amount) |
| payments.events | payment-service | order-service, delivery-service, notification-service | PaymentProcessedEvent (orderId, status, paymentId) |
| deliveries.events | delivery-service | notification-service | DeliveryStartedEvent (orderId, status, deliveryId) |

## Bases de Datos - Que se actualiza en cada paso

| Paso | Base de Datos | Tabla | Que se escribe |
|------|--------------|-------|----------------|
| Login | userdb | users | Solo lectura (valida credenciales) |
| Crear producto | productdb | products | Nuevo producto |
| Crear pedido | orderdb | orders + order_items | Pedido con status PENDING |
| Pago procesado | paymentdb | payments | Pago con status APPROVED |
| Pedido pagado | orderdb | orders | Actualiza status a PAID |
| Entrega creada | deliverydb | deliveries | Entrega con status IN_TRANSIT |
| Notificaciones | notificationdb | notifications | 3 registros (ORDER_CREATED, PAYMENT_APPROVED, DELIVERY_STARTED) |

## Seguridad (JWT)

- **user-service** genera tokens JWT al hacer login
- **product-service** y **order-service** validan tokens JWT en cada peticion
- **payment-service**, **delivery-service** y **notification-service** no tienen seguridad JWT (reciben eventos por Kafka)
- El secreto JWT es compartido entre user-service, product-service y order-service
- Los tokens expiran en 24 horas (local) o 1 hora (Kubernetes)

### Roles

| Rol | Que puede hacer |
|-----|-----------------|
| ADMIN | Todo: crear/editar/eliminar usuarios y productos, crear pedidos |
| USER | Crear pedidos |

## Resiliencia (Circuit Breaker)

- **product-service** tiene circuit breaker en las llamadas a user-service
- **order-service** tiene circuit breaker configurado para llamadas a user-service y product-service
- Si el servicio destino no responde, se activa el fallback despues de 3 reintentos

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

Obtener un token JWT para autenticarse.

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
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcy...",
    "type": "Bearer",
    "email": "juan.perez@example.com",
    "roles": ["ROLE_ADMIN"]
}
```

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

```
POST http://localhost:30082/api/products
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Pizza Margherita",
    "description": "Pizza italiana con tomate y mozzarella",
    "price": 25.50,
    "stock": 100,
    "category": "Food",
    "createdBy": 1
}
```

**Respuesta:**
```json
{
    "id": 6,
    "name": "Pizza Margherita",
    "description": "Pizza italiana con tomate y mozzarella",
    "price": 25.50,
    "stock": 100,
    "category": "Food"
}
```

---

### PASO 4: Crear un pedido (requiere token USER o ADMIN)

Este es el paso que dispara toda la cadena de eventos Kafka.

```
POST http://localhost:30083/api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
    "items": [
        {
            "productId": 1,
            "quantity": 2,
            "price": 1299.99
        },
        {
            "productId": 2,
            "quantity": 1,
            "price": 99.99
        }
    ]
}
```

**Respuesta:**
```json
{
    "id": 1,
    "userId": 1,
    "status": "PENDING",
    "totalAmount": 2699.97,
    "items": [
        { "id": 1, "productId": 1, "quantity": 2, "price": 1299.99 },
        { "id": 2, "productId": 2, "quantity": 1, "price": 99.99 }
    ],
    "createdAt": "2026-03-09T00:34:07",
    "updatedAt": "2026-03-09T00:34:07"
}
```

**Lo que pasa internamente (en ~5 segundos):**
1. Se guarda el pedido en orderdb con status PENDING
2. Se publica OrderCreatedEvent en Kafka
3. payment-service consume el evento, crea el pago, publica PaymentProcessedEvent
4. order-service consume PaymentProcessedEvent y cambia el status a PAID
5. delivery-service consume PaymentProcessedEvent, crea la entrega, publica DeliveryStartedEvent
6. notification-service consume los 3 eventos y crea 3 notificaciones

---

### PASO 5: Verificar que el pedido cambio a PAID (requiere token)

Espera 5 segundos despues de crear el pedido y consulta:

```
GET http://localhost:30083/api/orders/1
Authorization: Bearer <token>
```

**Respuesta:** El campo `status` debe ser `"PAID"` (ya no PENDING).

---

### PASO 6: Ver las notificaciones generadas (sin token)

```
GET http://localhost:30086/api/notifications
```

**Respuesta:**
```json
[
    {
        "id": 1,
        "userId": 1,
        "message": "Tu pedido #1 ha sido creado",
        "type": "ORDER_CREATED"
    },
    {
        "id": 2,
        "userId": null,
        "message": "Pago aprobado para pedido #1",
        "type": "PAYMENT_APPROVED"
    },
    {
        "id": 3,
        "userId": null,
        "message": "Tu pedido #1 esta en camino",
        "type": "DELIVERY_STARTED"
    }
]
```

---

### PASO 7: Verificar los eventos en Kafka UI

Abre http://localhost:8090 en el navegador. Ahi puedes ver:

- Topic `orders.events` - contiene el OrderCreatedEvent
- Topic `payments.events` - contiene el PaymentProcessedEvent
- Topic `deliveries.events` - contiene el DeliveryStartedEvent

---

### Otros endpoints disponibles

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
GET  /api/products/{id}      → Producto por ID (autenticado)
POST /api/products           → Crear producto (ADMIN)
PUT  /api/products/{id}      → Actualizar producto (ADMIN)
DELETE /api/products/{id}    → Eliminar producto (ADMIN)
GET  /api/products/health    → Health check (publico)
```

**Order Service (30083):**
```
POST /api/orders             → Crear pedido (USER o ADMIN)
GET  /api/orders/{id}        → Ver pedido (USER o ADMIN)
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

- [ ] Login exitoso, token recibido
- [ ] Producto creado correctamente
- [ ] Pedido creado con status PENDING y total calculado
- [ ] Pago procesado automaticamente (ver logs de payment-service)
- [ ] Pedido actualizado a PAID
- [ ] Entrega creada automaticamente (ver logs de delivery-service)
- [ ] 3 notificaciones creadas en notification-service
- [ ] Eventos visibles en Kafka UI (http://localhost:8090)

## Ver logs de un servicio en Kubernetes

```bash
kubectl logs -n order-service deployment/order-service --tail=30
kubectl logs -n payment-service deployment/payment-service --tail=30
kubectl logs -n delivery-service deployment/delivery-service --tail=30
kubectl logs -n notification-service deployment/notification-service --tail=30
```
