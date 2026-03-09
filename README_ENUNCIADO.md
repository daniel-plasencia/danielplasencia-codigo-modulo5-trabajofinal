# Como se Cumple el Enunciado del Ejercicio

Este documento mapea cada seccion del enunciado (`Proyecto_Arquitectura_Microservicios_Pedidos_Comida.md`) con su implementacion concreta en el proyecto.

---

## 1. Introduccion General

### 1.1 Proposito del documento

> Documentar la arquitectura de un sistema de pedidos de comida basado en microservicios.

**Como se cumple:** El proyecto implementa un sistema completo de pedidos de comida con 6 microservicios: user-service, product-service, order-service, payment-service, delivery-service y notification-service. La documentacion se encuentra en README.md, README_FUNCIONAL.md, README_ARCHIVOS.md y este archivo.

### 1.2 Alcance del sistema

> El sistema permite a los usuarios realizar pedidos desde multiples restaurantes, gestionar pagos y coordinar entregas.

**Como se cumple:**

| Funcionalidad | Servicio | Evidencia |
|--------------|----------|-----------|
| Registrar/autenticar usuarios | user-service | `POST /api/auth/login`, JWT con roles |
| Catalogo de productos | product-service | `GET /api/products`, `POST /api/products` |
| Realizar pedidos | order-service | `POST /api/orders` (valida producto via REST, obtiene precio) |
| Gestionar pagos | payment-service | Registra pago PENDING al recibir evento; aprobacion manual via REST |
| Coordinar entregas | delivery-service | Crea entrega al aprobarse pago; marcar entregado via REST |
| Notificar al usuario | notification-service | Consume los 3 topics de Kafka |

### 1.3 Audiencia y nivel tecnico esperado

> Desarrolladores backend, DevOps, QA y arquitectos de software.

**Como se cumple:** Documentacion tecnica en 4 READMEs separados, instrucciones de despliegue paso a paso, y guia de pruebas con Postman.

---

## 2. Vision Arquitectonica General

### 2.1 Estilo arquitectonico utilizado

> Arquitectura de Microservicios, con servicios independientes desplegados en contenedores Docker.

**Como se cumple:**
- 6 microservicios independientes, cada uno con su propio `pom.xml`, `Dockerfile`, base de datos y namespace de Kubernetes
- Cada servicio se compila, construye y despliega de forma independiente
- Comunicacion entre servicios via REST (sincrono) y Kafka (asincrono)

**Archivos clave:**
- `user-service/Dockerfile`, `product-service/Dockerfile`, etc.
- `docker-compose.yml` (infraestructura)
- `*/k8s/03-deployment.yaml` (despliegue K8s)

### 2.2 Decisiones arquitectonicas clave

> Separacion de funcionalidades en servicios independientes, comunicacion via REST y eventos Kafka, uso de base de datos por servicio.

**Como se cumple:**

| Decision | Implementacion |
|----------|---------------|
| Servicios independientes | 6 Spring Boot apps separadas, cada una con su propio proyecto Maven |
| Comunicacion REST | order-service → product-service (valida productos), product-service → user-service (valida usuarios) |
| Comunicacion Kafka | 3 topics: `orders.events`, `payments.events`, `deliveries.events` |
| BD por servicio | 6 PostgreSQL independientes: userdb, productdb, orderdb, paymentdb, deliverydb, notificationdb |

### 2.3 Diagramas de alto nivel

> Servicio de Usuarios, Pedidos, Catalogo, Entregas y Pagos.

**Como se cumple:** Los 6 servicios implementados corresponden a:
- Servicio de Usuarios → `user-service`
- Servicio de Catalogo → `product-service`
- Servicio de Pedidos → `order-service`
- Servicio de Pagos → `payment-service`
- Servicio de Entregas → `delivery-service`
- (Adicional) Servicio de Notificaciones → `notification-service`

---

## 3. Componentes del Sistema

### 3.1 Modulos principales y responsabilidades

> Cada servicio tiene responsabilidades bien definidas.

**Como se cumple:**

| Servicio | Responsabilidad | Puerto | BD |
|----------|----------------|--------|-----|
| user-service | Registro, login, gestion de usuarios, generacion de JWT | 8081/30081 | userdb (5432) |
| product-service | CRUD de productos, validacion de usuarios via REST | 8082/30082 | productdb (5433) |
| order-service | Crear pedidos, validar productos via REST, publicar eventos | 8083/30083 | orderdb (5434) |
| payment-service | Registrar pagos PENDING y aprobar manualmente via REST | 8084/30084 | paymentdb (5435) |
| delivery-service | Crear entregas al aprobarse pago, marcar entregado via REST | 8085/30085 | deliverydb (5436) |
| notification-service | Registrar notificaciones de todos los eventos | 8086/30086 | notificationdb (5437) |

### 3.2 Interfaces y APIs expuestas

> APIs REST para clientes web, y APIs internas entre servicios.

**Como se cumple:**
- **APIs publicas (cliente):** Login, listar productos, crear pedido, ver notificaciones
- **APIs internas (entre servicios):** `GET /api/products/{id}` (order→product), `GET /api/users/{id}` (product→user)
- Documentacion de endpoints en `README_FUNCIONAL.md`

### 3.3 Comunicacion entre componentes

> Principalmente asincronica via Kafka, con REST para operaciones sincronicas necesarias.

**Como se cumple:**

**REST (sincrono):**
| Origen | Destino | Endpoint | Para que |
|--------|---------|----------|----------|
| order-service | product-service | `GET /api/products/{id}` | Validar producto y obtener precio |
| product-service | user-service | `GET /api/users/{id}` | Validar que el usuario existe |

**Kafka (asincrono):**
| Topic | Productor | Consumidor(es) |
|-------|-----------|----------------|
| `orders.events` | order-service | payment-service, notification-service |
| `payments.events` | payment-service | order-service, delivery-service, notification-service |
| `deliveries.events` | delivery-service | order-service, notification-service |

**Archivos clave:**
- `order-service/.../ProductClient.java` (REST a product-service con Resilience4j)
- `product-service/.../UserClient.java` (REST a user-service con Resilience4j)
- `order-service/.../OrderEventPublisher.java` (Kafka producer)
- `payment-service/.../OrderEventKafkaListener.java` (Kafka consumer)

### 3.4 Integracion con sistemas externos (OPCIONAL)

> SE PUEDE SOLO SIMULAR LA PASARELA DE PAGOS NO MAS.

**Como se cumple:** El payment-service simula una pasarela de pagos. Cuando recibe un `OrderCreatedEvent`, registra el pago con status `PENDING`. El cliente debe aprobar el pago manualmente via `POST /api/payments/{orderId}/pay`, lo cual cambia el status a `APPROVED` y dispara la cadena de eventos. No se integra con Stripe ni otro proveedor real.

**Archivos:** `payment-service/.../ProcessPaymentUseCase.java` (crea PENDING), `ApprovePaymentUseCase.java` (aprueba), `PaymentController.java` (REST)

---

## 4. Detalle del Estilo Arquitectonico

### 4.2 Arquitectura de Microservicios

> Cada servicio es autonomo, despliegue independiente, y mantiene su propia base de datos. Uso de Kubernetes para orquestacion.

**Como se cumple:**

| Requisito | Implementacion |
|-----------|---------------|
| Servicio autonomo | Cada servicio tiene su propio `pom.xml`, `Dockerfile` y `Application.java` |
| Despliegue independiente | Cada servicio tiene su propia carpeta `k8s/` con 5 manifiestos (namespace, configmap, secret, deployment, service) |
| BD propia | 6 PostgreSQL separadas definidas en `docker-compose.yml` (puertos 5432-5437) |
| Kubernetes | Docker Desktop K8s, 6 namespaces, 6 deployments, 6 NodePort services (30081-30086) |

**Patron Clean Architecture:** Cada servicio sigue la misma estructura de capas:
- `domain/` → Modelos y repositorios (sin frameworks)
- `application/` → Casos de uso (logica de negocio)
- `presentation/` → Controllers y DTOs (REST)
- `infrastructure/` → JPA, Kafka, JWT, REST clients

---

## 5. Seguridad

### 5.1 Autenticacion y autorizacion

> Implementacion con OAuth2 y JWT para sesiones de usuario. Validacion de permisos por roles.

**Como se cumple:**

| Aspecto | Implementacion |
|---------|---------------|
| Autenticacion | JWT generado por user-service al hacer login (`POST /api/auth/login`) |
| Claims del JWT | `email` (subject), `roles` (ROLE_ADMIN, ROLE_USER), `userId` |
| Autorizacion por roles | Spring Security con `hasRole("ADMIN")`, `hasAnyRole("USER", "ADMIN")` |
| Propagacion de identidad | El `userId` se extrae del JWT en product-service y order-service |
| Secreto compartido | Mismo `JWT_SECRET` en user-service, product-service y order-service (via K8s Secret) |

**Roles implementados:**

| Rol | Permisos |
|-----|---------|
| ADMIN | Crear/editar/eliminar usuarios y productos, crear pedidos |
| USER | Crear pedidos |

**Archivos clave:**
- `user-service/.../JwtTokenProvider.java` → Genera JWT con userId
- `user-service/.../AuthController.java` → Endpoint de login
- `product-service/.../SecurityConfig.java` → Reglas de autorizacion
- `order-service/.../SecurityConfig.java` → Reglas de autorizacion
- `*/k8s/02-secret.yaml` → JWT_SECRET en base64

**Ejemplo de uso de identidad del JWT:**
- Al crear un producto: `createdBy` se asigna automaticamente con el `userId` del JWT
- Al crear un pedido: `userId` se extrae del JWT (no se pide en el body)

---

## 6. Escalabilidad y Rendimiento

### 6.1 Estrategias de escalabilidad

> Escalado horizontal automatico en Kubernetes por uso de CPU y cola de eventos.

**Como se cumple:**
- Cada servicio se despliega como Deployment en K8s con `replicas: 1`
- Se puede escalar con `kubectl scale deployment <servicio> --replicas=3 -n <namespace>`
- Kafka permite procesamiento asincrono y desacoplado (cola de eventos)
- Consumer groups en Kafka (`group-id` por servicio) permiten escalado de consumidores

### 6.2 Balanceo de carga

> Kubernetes Ingress Controller para balanceo de solicitudes REST.

**Como se cumple:**
- K8s Service (tipo NodePort) balancea trafico entre las replicas de cada pod
- La comunicacion interna usa DNS de K8s: `http://product-service.product-service.svc.cluster.local`
- NodePort expone cada servicio en puertos 30081-30086 para acceso externo

**Archivos:** `*/k8s/04-service.yaml`

### 6.3 Tolerancia a fallos y alta disponibilidad

> Replicacion de servicios criticos, reintento en servicios consumidores de eventos y circuit breakers.

**Como se cumple:**

| Mecanismo | Donde | Configuracion |
|-----------|-------|--------------|
| Circuit Breaker | product-service → user-service | Ventana: 10 llamadas, umbral: 50%, abierto: 10s |
| Circuit Breaker | order-service → product-service | Ventana: 10 llamadas, umbral: 50%, abierto: 10s |
| Retry | product-service → user-service | 3 intentos, 1s entre cada uno |
| Retry | order-service → product-service | 3 intentos, 1s entre cada uno |
| Fallback | UserClient, ProductClient | Retorna datos parciales o null cuando el servicio no responde |
| Kafka auto-offset-reset | Todos los consumers | `earliest` - no se pierden mensajes al reiniciar |
| Liveness/Readiness Probes | Todos los pods K8s | Spring Actuator health checks |

**Archivos clave:**
- `product-service/.../UserClient.java` → `@CircuitBreaker` + `@Retry` + fallback
- `order-service/.../ProductClient.java` → `@CircuitBreaker` + `@Retry` + fallback
- `*/application.yaml` → Configuracion de Resilience4j
- `*/k8s/03-deployment.yaml` → livenessProbe y readinessProbe

---

## 7. DevOps y Despliegue (OPCIONAL)

### 7.1 - 7.3 CI/CD, Infraestructura como codigo, Ambientes

**Como se cumple (parcialmente, es seccion opcional):**

| Aspecto | Implementacion |
|---------|---------------|
| Contenedorizacion | Dockerfile por servicio (Java 21 Alpine) |
| Orquestacion | Kubernetes manifiestos en `*/k8s/` |
| Infraestructura como codigo | Docker Compose para BDs + Kafka, K8s YAML para servicios |
| Ambientes | Perfil `default` (local) y perfil `kubernetes` (K8s) |
| Configuracion externalizada | ConfigMaps y Secrets de K8s, variables de entorno |
| Separacion de namespaces | Un namespace por servicio (user-service, product-service, etc.) |

---

## 8. Calidad y Mantenibilidad

### 8.1 Estrategias de pruebas

**Como se cumple:**
- Pruebas manuales documentadas con Postman en `README_FUNCIONAL.md`
- Flujo de prueba completo: login → crear producto → crear pedido → verificar pago → verificar notificaciones
- Checklist de verificacion incluido en README_FUNCIONAL.md
- Coleccion de Postman en `FoodOrdersSystem.postman_collection.json`

### 8.2 Observabilidad

> Uso de Prometheus para metricas, Grafana para dashboards.

**Como se cumple:**

| Herramienta | Implementacion |
|-------------|---------------|
| Spring Actuator | Todos los servicios exponen `/actuator/health`, `/actuator/info`, `/actuator/prometheus` |
| Prometheus | Configurado en `docker-compose-observability.yml` |
| Grafana | Configurado en `docker-compose-observability.yml` |
| Kafka UI | Accesible en `http://localhost:8090` para ver topics y mensajes |
| Logs K8s | `kubectl logs -n <namespace> deployment/<servicio>` |
| Health checks | Liveness y readiness probes en todos los deployments |

---

## 9. Anexos y Referencias (OPCIONAL)

### Migraciones de BD (Flyway)

Cada servicio tiene scripts SQL versionados que se ejecutan automaticamente al arrancar:
- `V1__CREATE_TABLES.sql` → Crea tablas
- `V2__ADD_INDEXES.sql` → Crea indices
- `V3__INSERT_DATA.sql` → Datos iniciales (usuarios, productos)

### Usuarios de prueba precargados

| Email | Password | Rol |
|-------|----------|-----|
| juan.perez@example.com | admin123 | ADMIN |
| maria.garcia@example.com | user123 | USER |
| ana.torres@example.com | admin123 | ADMIN |

### Tecnologias utilizadas

| Tecnologia | Version | Uso |
|-----------|---------|-----|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.4.2 | Framework de microservicios |
| PostgreSQL | 15 | Base de datos relacional |
| Apache Kafka | (via Docker) | Mensajeria asincrona |
| JJWT | 0.12.6 | Generacion y validacion de JWT |
| Resilience4j | 2.2.0 | Circuit breaker y retry |
| Flyway | (Spring Boot) | Migraciones de BD |
| MapStruct | 1.5.5 | Mapeo de DTOs |
| Lombok | (Spring Boot) | Reduccion de boilerplate |
| Docker | Desktop | Contenedorizacion |
| Kubernetes | Docker Desktop | Orquestacion |

---

## Resumen: Requisito → Implementacion

| # | Requisito del Enunciado | Implementado | Donde |
|---|------------------------|-------------|-------|
| 1 | Microservicios independientes | Si | 6 Spring Boot apps |
| 2 | Base de datos por servicio | Si | 6 PostgreSQL (docker-compose.yml) |
| 3 | Comunicacion REST sincrona | Si | order→product, product→user (RestTemplate) |
| 4 | Comunicacion Kafka asincrona | Si | 3 topics (orders, payments, deliveries) |
| 5 | Autenticacion JWT | Si | user-service genera, product/order validan |
| 6 | Autorizacion por roles | Si | ADMIN y USER con Spring Security |
| 7 | Circuit breaker | Si | Resilience4j en product-service y order-service |
| 8 | Retry | Si | 3 intentos en llamadas REST |
| 9 | Contenedores Docker | Si | Dockerfile por servicio |
| 10 | Orquestacion Kubernetes | Si | K8s manifiestos por servicio (namespace, configmap, secret, deployment, service) |
| 11 | Migraciones de BD | Si | Flyway (V1, V2, V3) |
| 12 | Health checks | Si | Spring Actuator + K8s probes |
| 13 | Observabilidad | Si | Actuator, Prometheus, Grafana, Kafka UI |
| 14 | Simulacion de pagos | Si | payment-service registra pago PENDING, aprobacion manual via REST |
| 15 | Clean Architecture | Si | domain, application, presentation, infrastructure |
