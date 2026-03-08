# Sistema de Pedidos de Comida - Microservicios

Sistema completo de pedidos de comida basado en arquitectura de microservicios con Spring Boot, PostgreSQL, Kafka, Docker y Kubernetes.

## 📋 Tabla de Contenidos

1. [Descripción del Sistema](#descripción-del-sistema)
2. [Arquitectura](#arquitectura)
3. [Prerrequisitos](#prerrequisitos)
4. [Estructura del Proyecto](#estructura-del-proyecto)
5. [Despliegue con Docker Compose](#despliegue-con-docker-compose)
6. [Ejecución Local de Microservicios](#ejecución-local-de-microservicios)
7. [Despliegue en Kubernetes](#despliegue-en-kubernetes)
8. [Configuración de Bases de Datos](#configuración-de-bases-de-datos)
9. [Pruebas con Postman](#pruebas-con-postman)
10. [Observabilidad](#observabilidad)
11. [Troubleshooting](#troubleshooting)

## 🎯 Descripción del Sistema

Sistema de pedidos de comida con 6 microservicios:

- **User Service** (8081): Gestión de usuarios y autenticación JWT
- **Product Service** (8082): Catálogo de productos
- **Order Service** (8083): Gestión de pedidos
- **Payment Service** (8084): Procesamiento de pagos
- **Delivery Service** (8085): Gestión de entregas
- **Notification Service** (8086): Notificaciones a usuarios

## 🏗️ Arquitectura

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ User Service │     │Product Svc  │     │Order Service│
│   (8081)     │     │   (8082)    │     │   (8083)    │
└──────┬───────┘     └──────┬──────┘     └──────┬──────┘
       │                    │                    │
       └────────────────────┼────────────────────┘
                            │
                    ┌───────▼───────┐
                    │     Kafka     │
                    └───────┬───────┘
                            │
       ┌────────────────────┼────────────────────┐
       │                    │                    │
┌──────▼──────┐    ┌───────▼──────┐    ┌───────▼──────┐
│Payment Svc  │    │Delivery Svc  │    │Notification  │
│   (8084)    │    │   (8085)     │    │   Service    │
└─────────────┘    └──────────────┘    └──────────────┘
```

## 📦 Prerrequisitos

- Java 21
- Maven 3.8+
- Docker Desktop (con Kubernetes habilitado)
- kubectl configurado
- Postman (para pruebas)

**Nota:** Este proyecto está configurado para ejecutarse completamente en local usando Docker Desktop. No requiere AWS, EKS, ni servicios en la nube.

## 📁 Estructura del Proyecto

```
danielplasencia-codigo-modulo5-trabajofinal/
├── docker-compose.yml                    # BDs y Kafka
├── docker-compose-observability.yml      # Prometheus, Grafana, Zipkin
├── user-service/                         # Microservicio de usuarios
├── product-service/                      # Microservicio de productos
├── order-service/                        # Microservicio de pedidos
├── payment-service/                      # Microservicio de pagos
├── delivery-service/                     # Microservicio de entregas
├── notification-service/                # Microservicio de notificaciones
├── observability/                        # Configuración de observabilidad
│   ├── prometheus/
│   └── grafana/
├── FoodOrdersSystem.postman_collection.json  # Colección Postman
└── README.md
```

## 🐳 Despliegue con Docker Compose

### 1. Iniciar Infraestructura (BDs y Kafka)

```bash
# Desde el directorio raíz
docker-compose up -d

# Verificar que todos los contenedores estén corriendo
docker ps
```

Deberías ver:
- postgres-user (puerto 5432)
- postgres-product (puerto 5433)
- postgres-order (puerto 5434)
- postgres-payment (puerto 5435)
- postgres-delivery (puerto 5436)
- postgres-notification (puerto 5437)
- zookeeper (puerto 2181)
- kafka (puerto 9092)
- kafka-ui (puerto 8090)

### 2. Verificar Kafka UI

Abre en el navegador: http://localhost:8090

## 🚀 Ejecución Local de Microservicios

### 1. User Service

```bash
cd user-service
mvn clean package -DskipTests
mvn spring-boot:run
```

El servicio estará disponible en: http://localhost:8081

### 2. Product Service

```bash
cd product-service
mvn clean package -DskipTests
mvn spring-boot:run
```

El servicio estará disponible en: http://localhost:8082

### 3. Order Service

```bash
cd order-service
mvn clean package -DskipTests
mvn spring-boot:run
```

El servicio estará disponible en: http://localhost:8083

### 4. Payment Service

```bash
cd payment-service
mvn clean package -DskipTests
mvn spring-boot:run
```

El servicio estará disponible en: http://localhost:8084

### 5. Delivery Service

```bash
cd delivery-service
mvn clean package -DskipTests
mvn spring-boot:run
```

El servicio estará disponible en: http://localhost:8085

### 6. Notification Service

```bash
cd notification-service
mvn clean package -DskipTests
mvn spring-boot:run
```

El servicio estará disponible en: http://localhost:8086

## ☸️ Despliegue en Kubernetes (Docker Desktop)

**Importante:** Este proyecto está configurado para Kubernetes local usando Docker Desktop. No requiere AWS EKS ni servicios en la nube.

### 0. Habilitar Kubernetes en Docker Desktop

1. Abre Docker Desktop
2. Ve a Settings → Kubernetes
3. Marca "Enable Kubernetes"
4. Espera a que se inicie (verás un check verde)
5. Verifica: `kubectl get nodes`

### 1. Construir Imágenes Docker

Para cada servicio:

```bash
# User Service
cd user-service
docker build -t user-service:1.0 .

# Product Service
cd product-service
docker build -t product-service:1.0 .

# Order Service
cd order-service
docker build -t order-service:1.0 .

# Payment Service
cd payment-service
docker build -t payment-service:1.0 .

# Delivery Service
cd delivery-service
docker build -t delivery-service:1.0 .

# Notification Service
cd notification-service
docker build -t notification-service:1.0 .
```

### 2. Verificar que las imágenes estén disponibles

Con Docker Desktop, las imágenes Docker locales están disponibles automáticamente para Kubernetes. Solo asegúrate de que las imágenes estén construidas:

```bash
docker images | grep -E "user-service|product-service|order-service|payment-service|delivery-service|notification-service"
```

### 3. Desplegar en Kubernetes

Para cada servicio, desde su directorio `k8s/`:

```bash
# User Service
kubectl apply -f user-service/k8s/

# Product Service
kubectl apply -f product-service/k8s/

# Order Service
kubectl apply -f order-service/k8s/

# Payment Service
kubectl apply -f payment-service/k8s/

# Delivery Service
kubectl apply -f delivery-service/k8s/

# Notification Service
kubectl apply -f notification-service/k8s/
```

### 4. Verificar Despliegue

```bash
# Ver pods
kubectl get pods --all-namespaces

# Ver servicios
kubectl get svc --all-namespaces

# Ver logs de un servicio
kubectl logs -n user-service deployment/user-service
```

## 🗄️ Configuración de Bases de Datos

Las migraciones de Flyway se ejecutan automáticamente al iniciar cada servicio.

### Datos Iniciales

**User Service:**
- Email: `juan.perez@example.com`, Password: `admin123`, Role: `ADMIN`
- Email: `maria.garcia@example.com`, Password: `user123`, Role: `USER`

**Product Service:**
- Productos iniciales cargados automáticamente

## 📮 Pruebas con Postman

### 1. Importar Colección

Importa el archivo `FoodOrdersSystem.postman_collection.json` en Postman.

### 2. Flujo de Prueba

1. **Login** (User Service)
   - POST `http://localhost:8081/api/auth/login`
   - Body: `{"email": "juan.perez@example.com", "password": "admin123"}`
   - Copia el `token` de la respuesta

2. **Crear Producto** (Product Service)
   - POST `http://localhost:8082/api/products`
   - Header: `Authorization: Bearer <token>`
   - Body: `{"name": "Pizza", "price": 25.50, "description": "Pizza Margherita"}`

3. **Crear Pedido** (Order Service)
   - POST `http://localhost:8083/api/orders`
   - Header: `Authorization: Bearer <token>`
   - Body: `{"items": [{"productId": 1, "quantity": 2}]}`

4. **Verificar Pago** (Payment Service)
   - El pago se procesa automáticamente cuando se crea un pedido

5. **Verificar Entrega** (Delivery Service)
   - La entrega se crea automáticamente cuando el pago es exitoso

6. **Ver Notificaciones** (Notification Service)
   - GET `http://localhost:8086/api/notifications`
   - Verás notificaciones de todos los eventos

## 🎯 Guía Funcional - Flujo Completo de la Aplicación

Esta sección explica cómo funciona el sistema de pedidos de comida desde el punto de vista funcional y cómo probarlo paso a paso.

### 📋 Flujo General del Sistema

El sistema funciona con comunicación **síncrona (REST)** y **asíncrona (Kafka)**:

```
┌─────────────┐
│   Cliente   │
└──────┬──────┘
       │
       │ 1. Login (REST)
       ▼
┌─────────────┐
│User Service │ ──► Genera JWT Token
└──────┬──────┘
       │
       │ 2. Crear Producto (REST + JWT)
       ▼
┌─────────────┐
│Product Svc  │ ──► Guarda en productdb
└──────┬──────┘
       │
       │ 3. Crear Pedido (REST + JWT)
       ▼
┌─────────────┐     ┌──────────────┐
│Order Service│ ──►│ Product Svc  │ (Valida productos)
└──────┬──────┘     └──────────────┘
       │
       │ 4. Publica evento: OrderCreatedEvent
       ▼
   ┌───┴───┐
   │ Kafka │
   └───┬───┘
       │
       ├──► 5. Payment Service (consume evento)
       │    └─► Procesa pago (simulado: siempre aprueba)
       │    └─► Publica: PaymentProcessedEvent
       │
       ├──► 6. Delivery Service (consume PaymentProcessedEvent)
       │    └─► Crea entrega
       │    └─► Publica: DeliveryStartedEvent
       │
       └──► 7. Notification Service (consume todos los eventos)
            └─► Crea notificaciones para el usuario
```

### 🔄 Flujo Detallado Paso a Paso

#### **Paso 1: Autenticación (User Service)**

**Qué hace:**
- El usuario se autentica con email y contraseña
- El sistema valida las credenciales contra la base de datos
- Si son correctas, genera un token JWT que expira en 1 hora

**Cómo probarlo:**
```bash
POST http://localhost:8081/api/auth/login
Body:
{
  "email": "juan.perez@example.com",
  "password": "admin123"
}

Respuesta:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer"
}
```

**Importante:** Guarda el `token` para usarlo en las siguientes peticiones.

---

#### **Paso 2: Crear Producto (Product Service)**

**Qué hace:**
- Crea un nuevo producto en el catálogo
- Valida que el usuario esté autenticado (JWT)
- Guarda el producto en `productdb`

**Cómo probarlo:**
```bash
POST http://localhost:8082/api/products
Headers:
  Authorization: Bearer <token>

Body:
{
  "name": "Pizza Margherita",
  "price": 25.50,
  "description": "Deliciosa pizza italiana con tomate y mozzarella"
}

Respuesta:
{
  "id": 1,
  "name": "Pizza Margherita",
  "price": 25.50,
  "description": "Deliciosa pizza italiana con tomate y mozzarella",
  "createdAt": "2025-01-15T10:30:00"
}
```

**Nota:** Puedes crear varios productos. Anota los IDs para usarlos en el pedido.

---

#### **Paso 3: Crear Pedido (Order Service)**

**Qué hace:**
1. Valida que el usuario esté autenticado (extrae `userId` del JWT)
2. Valida cada producto llamando al **Product Service** (comunicación síncrona)
3. Calcula el total del pedido
4. Guarda el pedido en `orderdb` con estado `PENDING`
5. Publica evento `OrderCreatedEvent` en Kafka (comunicación asíncrona)

**Cómo probarlo:**
```bash
POST http://localhost:8083/api/orders
Headers:
  Authorization: Bearer <token>

Body:
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ]
}

Respuesta:
{
  "id": 1,
  "orderNumber": "ORD-2025-001",
  "userId": 1,
  "status": "PENDING",
  "totalAmount": 76.50,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "price": 25.50,
      "subtotal": 51.00
    },
    {
      "productId": 2,
      "quantity": 1,
      "price": 25.50,
      "subtotal": 25.50
    }
  ],
  "createdAt": "2025-01-15T10:35:00"
}
```

**Lo que sucede internamente:**
- Order Service llama a Product Service para validar cada producto
- Si un producto no existe, retorna error 404
- Calcula el total: `sum(quantity × price)` de cada item
- Guarda en BD y publica evento en Kafka

---

#### **Paso 4: Procesamiento de Pago (Payment Service) - Automático**

**Qué hace:**
- **Payment Service** está escuchando el topic `orders.events` en Kafka
- Cuando recibe un `OrderCreatedEvent`, automáticamente:
  1. Crea un registro de pago en `paymentdb`
  2. Simula el procesamiento (siempre aprueba el pago)
  3. Publica evento `PaymentProcessedEvent` en Kafka

**Cómo verificar:**
```bash
# Ver el pago creado
GET http://localhost:8084/api/payments/1

Respuesta:
{
  "id": 1,
  "orderId": 1,
  "amount": 76.50,
  "status": "APPROVED",
  "paymentMethod": "CREDIT_CARD",
  "processedAt": "2025-01-15T10:35:05"
}
```

**Nota:** Este paso es **automático**. No necesitas hacer nada, solo esperar unos segundos después de crear el pedido.

---

#### **Paso 5: Creación de Entrega (Delivery Service) - Automático**

**Qué hace:**
- **Delivery Service** está escuchando el topic `payments.events` en Kafka
- Cuando recibe un `PaymentProcessedEvent` con status `APPROVED`, automáticamente:
  1. Crea un registro de entrega en `deliverydb`
  2. Asigna estado `IN_TRANSIT`
  3. Publica evento `DeliveryStartedEvent` en Kafka

**Cómo verificar:**
```bash
# Ver la entrega creada
GET http://localhost:8085/api/deliveries/1

Respuesta:
{
  "id": 1,
  "orderId": 1,
  "status": "IN_TRANSIT",
  "address": "Calle Principal 123",
  "estimatedDeliveryTime": "2025-01-15T11:35:00",
  "createdAt": "2025-01-15T10:35:10"
}
```

**Nota:** Este paso también es **automático**. Se ejecuta después de que el pago sea aprobado.

---

#### **Paso 6: Notificaciones (Notification Service) - Automático**

**Qué hace:**
- **Notification Service** está escuchando **todos** los eventos de Kafka:
  - `orders.events` → Crea notificación "Pedido creado"
  - `payments.events` → Crea notificación "Pago procesado"
  - `deliveries.events` → Crea notificación "Entrega iniciada"

**Cómo verificar:**
```bash
# Ver todas las notificaciones
GET http://localhost:8086/api/notifications

Respuesta:
[
  {
    "id": 1,
    "userId": 1,
    "message": "Tu pedido ORD-2025-001 ha sido creado",
    "type": "ORDER_CREATED",
    "read": false,
    "createdAt": "2025-01-15T10:35:00"
  },
  {
    "id": 2,
    "userId": 1,
    "message": "Tu pago de $76.50 ha sido aprobado",
    "type": "PAYMENT_APPROVED",
    "read": false,
    "createdAt": "2025-01-15T10:35:05"
  },
  {
    "id": 3,
    "userId": 1,
    "message": "Tu pedido está en camino",
    "type": "DELIVERY_STARTED",
    "read": false,
    "createdAt": "2025-01-15T10:35:10"
  }
]
```

---

### 🧪 Flujo de Prueba Completo (End-to-End)

Sigue estos pasos en orden para probar todo el sistema:

#### **1. Preparación**
```bash
# Iniciar infraestructura
docker-compose up -d

# Verificar que todo esté corriendo
docker ps
```

#### **2. Iniciar Microservicios**
Inicia cada servicio en orden (en terminales separadas o en background):

```bash
# Terminal 1: User Service
cd user-service
mvn spring-boot:run

# Terminal 2: Product Service
cd product-service
mvn spring-boot:run

# Terminal 3: Order Service
cd order-service
mvn spring-boot:run

# Terminal 4: Payment Service
cd payment-service
mvn spring-boot:run

# Terminal 5: Delivery Service
cd delivery-service
mvn spring-boot:run

# Terminal 6: Notification Service
cd notification-service
mvn spring-boot:run
```

#### **3. Ejecutar Flujo Completo con Postman**

**3.1. Login**
- Request: `POST /api/auth/login`
- Body: `{"email": "juan.perez@example.com", "password": "admin123"}`
- Guarda el `token` de la respuesta

**3.2. Crear Producto**
- Request: `POST /api/products`
- Header: `Authorization: Bearer <token>`
- Body: `{"name": "Pizza", "price": 25.50, "description": "Pizza Margherita"}`
- Anota el `id` del producto creado

**3.3. Crear Pedido**
- Request: `POST /api/orders`
- Header: `Authorization: Bearer <token>`
- Body: `{"items": [{"productId": 1, "quantity": 2}]}`
- Anota el `id` del pedido creado

**3.4. Esperar 5-10 segundos** (para que se procesen los eventos de Kafka)

**3.5. Verificar Pago**
- Request: `GET /api/payments/{orderId}`
- Deberías ver el pago aprobado

**3.6. Verificar Entrega**
- Request: `GET /api/deliveries/{orderId}`
- Deberías ver la entrega creada

**3.7. Verificar Notificaciones**
- Request: `GET /api/notifications`
- Deberías ver 3 notificaciones (pedido, pago, entrega)

---

### 🔍 Verificación de Eventos en Kafka

Puedes verificar que los eventos se están publicando correctamente:

1. **Abrir Kafka UI**: http://localhost:8090
2. **Ver topics**: Deberías ver `orders.events`, `payments.events`, `deliveries.events`
3. **Ver mensajes**: Haz clic en cada topic para ver los mensajes publicados

---

### 📊 Diagrama de Secuencia Completo

```
Cliente          User Svc    Product Svc   Order Svc    Kafka    Payment Svc   Delivery Svc   Notification Svc
  │                 │             │            │          │          │              │                 │
  ├─Login──────────►│             │            │          │          │              │                 │
  │◄─Token──────────┤             │            │          │          │              │                 │
  │                 │             │            │          │          │              │                 │
  ├─Create Product─►│────────────►│            │          │          │              │                 │
  │◄─Product ID─────┤◄────────────┤            │          │          │              │                 │
  │                 │             │            │          │          │              │                 │
  ├─Create Order───►│────────────►│───────────►│          │          │              │                 │
  │                 │             │            │          │          │              │                 │
  │                 │             │            ├─Event───►│          │              │                 │
  │                 │             │            │          │          │              │                 │
  │                 │             │            │          ├─Event───►│              │                 │
  │                 │             │            │          │          ├─Event───────►│                 │
  │                 │             │            │          │          │              ├─Event───────────►│
  │                 │             │            │          │          │              │                 │
  │◄─Order ID──────┤             │◄───────────┤          │          │              │                 │
  │                 │             │            │          │          │              │                 │
  ├─Get Payment────►│             │            │          │          │              │                 │
  │◄─Payment───────┤             │            │          │          │              │                 │
  │                 │             │            │          │          │              │                 │
  ├─Get Delivery───►│             │            │          │          │              │                 │
  │◄─Delivery───────┤             │            │          │          │              │                 │
  │                 │             │            │          │          │              │                 │
  ├─Get Notifications────────────►│             │            │          │          │              │                 │
  │◄─Notifications────────────────┤             │            │          │          │              │                 │
```

---

### ✅ Checklist de Verificación

Después de ejecutar el flujo completo, verifica:

- [ ] Login exitoso y token recibido
- [ ] Producto creado correctamente
- [ ] Pedido creado con total calculado correctamente
- [ ] Pago procesado automáticamente (status: APPROVED)
- [ ] Entrega creada automáticamente (status: IN_TRANSIT)
- [ ] 3 notificaciones creadas (pedido, pago, entrega)
- [ ] Eventos visibles en Kafka UI

---

## 📊 Observabilidad

### 1. Iniciar Stack de Observabilidad

```bash
docker-compose -f docker-compose-observability.yml up -d
```

### 2. Acceder a Dashboards

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Zipkin**: http://localhost:9411

### 3. Métricas Disponibles

- Health checks de cada servicio
- Métricas de JVM
- Métricas de HTTP requests
- Métricas de Kafka

## 🔧 Troubleshooting

### Problema: Los servicios no se conectan a la BD

**Solución:**
- Verifica que Docker Compose esté corriendo: `docker ps`
- Verifica las URLs de conexión en `application.yaml`
- Revisa los logs: `docker logs postgres-user`

### Problema: Kafka no funciona

**Solución:**
- Verifica que Zookeeper esté corriendo: `docker ps | grep zookeeper`
- Espera 30 segundos después de iniciar Zookeeper antes de iniciar Kafka
- Revisa logs: `docker logs kafka`

### Problema: Error 401 en las peticiones

**Solución:**
- Asegúrate de hacer login primero y obtener el token JWT
- Incluye el header: `Authorization: Bearer <token>`
- Verifica que el token no haya expirado (1 hora por defecto)

### Problema: Los servicios no se comunican en Kubernetes

**Solución:**
- Verifica que los servicios estén en el mismo namespace o usa FQDN
- Formato: `http://service-name.namespace.svc.cluster.local:port`
- Revisa los logs: `kubectl logs -n <namespace> <pod-name>`


## 📝 URLs de Servicios

### Local (Docker Compose)
| Servicio | Puerto Local | Health Check |
|----------|--------------|--------------|
| User Service | 8081 | /actuator/health |
| Product Service | 8082 | /actuator/health |
| Order Service | 8083 | /actuator/health |
| Payment Service | 8084 | /actuator/health |
| Delivery Service | 8085 | /actuator/health |
| Notification Service | 8086 | /actuator/health |
| Kafka UI | 8090 | - |
| Prometheus | 9090 | - |
| Grafana | 3000 | - |
| Zipkin | 9411 | - |

### Kubernetes (NodePort)
| Servicio | Puerto NodePort | Health Check |
|----------|-----------------|--------------|
| User Service | http://localhost:30081 | /actuator/health |
| Product Service | http://localhost:30082 | /actuator/health |
| Order Service | http://localhost:30083 | /actuator/health |
| Payment Service | http://localhost:30084 | /actuator/health |
| Delivery Service | http://localhost:30085 | /actuator/health |
| Notification Service | http://localhost:30086 | /actuator/health |

**Nota:** Los servicios están expuestos mediante NodePort. Puedes acceder directamente usando `localhost` con el puerto NodePort correspondiente.

## 📚 Referencias

- [Documento de Arquitectura](./Proyecto_Arquitectura_Microservicios_Pedidos_Comida.md)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Docker Desktop Kubernetes](https://docs.docker.com/desktop/kubernetes/)

## ⚠️ Nota Importante

Este proyecto está configurado para ejecutarse **completamente en local** usando:
- Docker Compose para bases de datos y Kafka
- Docker Desktop Kubernetes para los microservicios
- No requiere AWS, EKS, ni ningún servicio en la nube

Todo está diseñado para ejecutarse en tu PC local con Docker Desktop.
