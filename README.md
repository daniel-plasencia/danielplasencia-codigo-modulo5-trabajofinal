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
- Docker y Docker Compose
- Kubernetes (Minikube, Kind, o clúster real)
- kubectl configurado
- Postman (para pruebas)

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

## ☸️ Despliegue en Kubernetes

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

### 2. Cargar Imágenes en Kubernetes

Si usas Minikube:

```bash
minikube image load user-service:1.0
minikube image load product-service:1.0
minikube image load order-service:1.0
minikube image load payment-service:1.0
minikube image load delivery-service:1.0
minikube image load notification-service:1.0
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

| Servicio | Puerto Local | Puerto K8s | Health Check |
|----------|--------------|------------|--------------|
| User Service | 8081 | 30081 | /actuator/health |
| Product Service | 8082 | 30082 | /actuator/health |
| Order Service | 8083 | 30083 | /actuator/health |
| Payment Service | 8084 | 30084 | /actuator/health |
| Delivery Service | 8085 | 30085 | /actuator/health |
| Notification Service | 8086 | 30086 | /actuator/health |
| Kafka UI | 8090 | - | - |
| Prometheus | 9090 | - | - |
| Grafana | 3000 | - | - |
| Zipkin | 9411 | - | - |

## 📚 Referencias

- [Documento de Arquitectura](./Proyecto_Arquitectura_Microservicios_Pedidos_Comida.md)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
