# Estructura de Archivos del Proyecto

Que hace cada archivo y para que sirve.

## Archivos Raiz

| Archivo | Para que sirve |
|---------|---------------|
| `docker-compose.yml` | Levanta las 6 bases de datos PostgreSQL, Zookeeper, Kafka y Kafka UI |
| `docker-compose-observability.yml` | Levanta Prometheus, Grafana y Zipkin para monitoreo (opcional) |
| `README.md` | Documentacion principal del proyecto |
| `README_FUNCIONAL.md` | Guia funcional: como funciona todo y como probar con Postman |
| `README_ARCHIVOS.md` | Este archivo: que hace cada archivo del proyecto |
| `Proyecto_Arquitectura_Microservicios_Pedidos_Comida.md` | Documento de arquitectura de software (requisitos del trabajo final) |
| `FoodOrdersSystem.postman_collection.json` | Coleccion de Postman con las peticiones de prueba |

## Estructura de cada Microservicio

Todos los microservicios siguen la misma estructura (Clean Architecture). Uso user-service como ejemplo, pero aplica igual para los demas.

```
user-service/
├── Dockerfile                          → Como construir la imagen Docker
├── pom.xml                             → Dependencias Maven del servicio
├── database/                           → Scripts SQL de migracion (Flyway)
│   ├── V1__CREATE_TABLES.sql           → Crea las tablas
│   ├── V2__ADD_INDEXES.sql             → Crea los indices
│   └── V3__INSERT_DATA.sql             → Inserta datos iniciales
├── k8s/                                → Manifiestos de Kubernetes
│   ├── 00-namespace.yaml               → Crea el namespace en K8s
│   ├── 01-configmap.yaml               → Configuracion no sensible (URLs, puertos)
│   ├── 02-secret.yaml                  → Datos sensibles (passwords, JWT secret)
│   ├── 03-deployment.yaml              → Define el pod y sus variables de entorno
│   └── 04-service.yaml                 → Expone el pod via NodePort
└── src/main/
    ├── resources/
    │   ├── application.yaml            → Config para ejecucion local
    │   └── application-kubernetes.yaml → Config para ejecucion en K8s
    └── java/com/tecsup/app/micro/user/
        ├── UserServiceApplication.java → Clase principal que arranca el servicio
        ├── domain/                     → Logica de negocio pura (sin frameworks)
        │   ├── model/                  → Objetos de dominio (User, etc.)
        │   ├── repository/             → Interfaces de repositorio
        │   └── exception/              → Excepciones de negocio
        ├── application/                → Casos de uso
        │   ├── usecase/                → Un caso de uso por clase (CreateUser, GetUser, etc.)
        │   └── service/                → Servicio que agrupa los casos de uso
        ├── presentation/               → Capa REST (lo que ve el cliente)
        │   ├── controller/             → Controladores REST (endpoints)
        │   ├── dto/                    → Objetos de peticion y respuesta (Request/Response)
        │   └── mapper/                 → Convierte entre DTOs y objetos de dominio (MapStruct)
        └── infrastructure/             → Implementaciones tecnicas
            ├── config/                 → Configuracion (Security, JWT, Kafka, Beans)
            ├── persistence/            → JPA: entidades, repositorios, mappers
            ├── client/                 → Clientes REST para llamar a otros servicios
            └── kafka/                  → Productores y consumidores de Kafka
```

## Archivos por Servicio - Que hace cada uno

### user-service (Usuarios y Autenticacion)

**Domain:**
| Archivo | Que hace |
|---------|---------|
| `User.java` | Modelo de dominio de usuario (id, name, email, password, role) |
| `UserRepository.java` | Interface de repositorio de usuarios |
| `UserNotFoundException.java` | Excepcion cuando no se encuentra un usuario |
| `DuplicateEmailException.java` | Excepcion cuando el email ya existe |
| `InvalidUserDataException.java` | Excepcion para datos invalidos |

**Application:**
| Archivo | Que hace |
|---------|---------|
| `UserApplicationService.java` | Agrupa todos los casos de uso de usuario |
| `CreateUserUseCase.java` | Crea un nuevo usuario con password encriptado |
| `GetAllUsersUseCase.java` | Obtiene todos los usuarios |
| `GetUserByIdUseCase.java` | Busca un usuario por su ID |
| `UpdateUserUseCase.java` | Actualiza los datos de un usuario |
| `DeleteUserUseCase.java` | Elimina un usuario |

**Presentation:**
| Archivo | Que hace |
|---------|---------|
| `UserController.java` | Endpoints REST de usuarios (/api/users) |
| `AuthController.java` | Endpoint de login (/api/auth/login). Genera JWT con userId en los claims |
| `GlobalExceptionHandler.java` | Maneja errores y devuelve respuestas JSON |
| `CreateUserRequest.java` | DTO para crear usuario |
| `UpdateUserRequest.java` | DTO para actualizar usuario |
| `UserResponse.java` | DTO de respuesta de usuario |
| `LoginRequest.java` | DTO para login (email + password) |
| `LoginResponse.java` | DTO de respuesta de login (token + roles) |
| `UserDtoMapper.java` | Convierte entre DTOs y modelo de dominio |

**Infrastructure:**
| Archivo | Que hace |
|---------|---------|
| `SecurityConfig.java` | Configura Spring Security: que endpoints son publicos, cuales necesitan token |
| `JwtTokenProvider.java` | Genera JWT con email, roles y userId. Valida tokens JWT |
| `JwtAuthenticationFilter.java` | Filtro que intercepta peticiones y valida el JWT |
| `CustomUserDetailsService.java` | Carga usuarios de la BD para Spring Security |
| `BeanConfig.java` | Registra beans como RestTemplate y PasswordEncoder |
| `UserEntity.java` | Entidad JPA mapeada a la tabla users |
| `JpaUserRepository.java` | Interface JPA que habla con PostgreSQL |
| `UserRepositoryImpl.java` | Implementa el repositorio del dominio usando JPA |

---

### product-service (Catalogo de Productos)

Misma estructura que user-service, mas:

| Archivo | Que hace |
|---------|---------|
| `ProductController.java` | Endpoints REST. POST extrae userId del JWT para createdBy (ya no se pide en el body) |
| `JwtTokenProvider.java` | Valida tokens JWT y extrae userId del claim (para asignar createdBy) |
| `UserClient.java` | Llama al user-service via REST para validar usuarios (circuit breaker + retry) |
| `UserDto.java` / `UserDtoMapper.java` | DTO y mapper para la respuesta del user-service |
| `ProductPersistenceMapper.java` | Convierte entre entidad JPA y dominio |

---

### order-service (Pedidos)

Misma estructura base, mas:

| Archivo | Que hace |
|---------|---------|
| `Order.java` / `OrderItem.java` | Modelos de dominio del pedido y sus items |
| `OrderEntity.java` / `OrderItemEntity.java` | Entidades JPA |
| `CreateOrderUseCase.java` | Crea el pedido, calcula total y publica evento Kafka |
| `UpdateOrderStatusUseCase.java` | Actualiza el status del pedido (PENDING → PAID) |
| `GetOrderByIdUseCase.java` | Busca un pedido por ID |
| `ProductClient.java` | Llama al product-service via REST para validar productos y obtener precios (circuit breaker + retry) |
| `ProductDto.java` | DTO para la respuesta del product-service (id, name, price, stock, available) |
| `BeanConfig.java` | Registra el bean RestTemplate para llamadas REST a product-service |
| `OrderEventPublisher.java` | Publica OrderCreatedEvent en Kafka topic orders.events |
| `PaymentEventKafkaListener.java` | Consume PaymentProcessedEvent de Kafka y actualiza la orden a PAID |
| `DeliveryEventKafkaListener.java` | Consume DeliveryEvent de Kafka; si status es DELIVERED, actualiza la orden a DELIVERED |
| `DeliveryEventDto.java` | DTO del evento de entrega que se consume (orderId, status, deliveryId) |
| `OrderCreatedEvent.java` | DTO del evento que se publica (orderId, userId, amount) |
| `PaymentEventDto.java` | DTO del evento de pago que se consume (orderId, status, paymentId) |
| `KafkaConfig.java` | Define los topics de Kafka (orders.events, deliveries.events) y los crea automaticamente |
| `SecurityConfig.java` | Seguridad JWT para los endpoints de pedidos |
| `JwtTokenProvider.java` | Valida tokens JWT y extrae userId (mismo secret que user-service) |
| `JwtAuthenticationFilter.java` | Filtro JWT |

---

### payment-service (Pagos)

| Archivo | Que hace |
|---------|---------|
| `Payment.java` | Modelo de dominio del pago |
| `PaymentRepository.java` | Interface de repositorio (incluye findAll y findByOrderId) |
| `PaymentEntity.java` | Entidad JPA mapeada a la tabla payments |
| `PaymentRepositoryImpl.java` | Implementa el repositorio del dominio usando JPA |
| `ProcessPaymentUseCase.java` | Crea el pago con status PENDING (no lo aprueba automaticamente) |
| `ApprovePaymentUseCase.java` | Aprueba un pago manualmente: cambia status a APPROVED y publica evento Kafka |
| `PaymentController.java` | Endpoints REST: GET /api/payments, GET /api/payments/order/{orderId}, POST /api/payments/{orderId}/pay |
| `OrderEventKafkaListener.java` | Consume OrderCreatedEvent de Kafka y registra pago PENDING |
| `PaymentEventPublisher.java` | Publica PaymentProcessedEvent en Kafka topic payments.events |
| `OrderEventDto.java` | DTO del evento que consume (orderId, userId, amount) |
| `PaymentProcessedEvent.java` | DTO del evento que publica (orderId, status, paymentId) |
| `KafkaConfig.java` | Define los topics orders.events y payments.events |

---

### delivery-service (Entregas)

| Archivo | Que hace |
|---------|---------|
| `Delivery.java` | Modelo de dominio de la entrega |
| `DeliveryRepository.java` | Interface de repositorio (incluye findAll y findByOrderId) |
| `DeliveryEntity.java` | Entidad JPA mapeada a la tabla deliveries |
| `DeliveryRepositoryImpl.java` | Implementa el repositorio del dominio usando JPA |
| `CreateDeliveryUseCase.java` | Crea la entrega con status IN_TRANSIT y publica evento |
| `UpdateDeliveryStatusUseCase.java` | Actualiza status de una entrega (ej: a DELIVERED) y publica evento Kafka |
| `DeliveryController.java` | Endpoints REST: GET /api/deliveries, GET /api/deliveries/order/{orderId}, PUT /api/deliveries/{id}/status |
| `PaymentEventKafkaListener.java` | Consume PaymentProcessedEvent; si el pago fue APPROVED, crea la entrega |
| `DeliveryEventPublisher.java` | Publica DeliveryEvent en Kafka topic deliveries.events (IN_TRANSIT o DELIVERED) |
| `PaymentEventDto.java` | DTO del evento que consume |
| `DeliveryStartedEvent.java` | DTO del evento que publica (orderId, status, deliveryId) |
| `KafkaConfig.java` | Define los topics payments.events y deliveries.events |

---

### notification-service (Notificaciones)

| Archivo | Que hace |
|---------|---------|
| `Notification.java` | Modelo de dominio de la notificacion |
| `NotificationEntity.java` | Entidad JPA mapeada a la tabla notifications |
| `CreateNotificationUseCase.java` | Guarda una notificacion en la BD |
| `NotificationKafkaListeners.java` | Consume los 3 topics de Kafka y crea notificaciones (diferencia DELIVERY_STARTED y DELIVERY_COMPLETED) |
| `NotificationController.java` | Endpoint GET /api/notifications para ver todas las notificaciones |
| `OrderEventDto.java` | DTO del evento de ordenes |
| `PaymentEventDto.java` | DTO del evento de pagos |
| `DeliveryEventDto.java` | DTO del evento de entregas |
| `KafkaConfig.java` | Define los 3 topics que consume |

---

## Archivos de Kubernetes (k8s/)

Cada servicio tiene 5 manifiestos en su carpeta `k8s/`:

| Archivo | Que hace |
|---------|---------|
| `00-namespace.yaml` | Crea un namespace aislado para el servicio (ej: user-service) |
| `01-configmap.yaml` | Guarda configuracion no sensible: URL de BD, URL de otros servicios, URL de Kafka |
| `02-secret.yaml` | Guarda datos sensibles en base64: usuario y password de BD, JWT secret |
| `03-deployment.yaml` | Define como correr el pod: imagen Docker, variables de entorno, health checks |
| `04-service.yaml` | Expone el pod fuera del cluster via NodePort (30081-30086) |

## Archivos de Docker

| Archivo | Que hace |
|---------|---------|
| `Dockerfile` (cada servicio) | Copia el JAR y lo ejecuta con Java 21 Alpine |
| `docker-compose.yml` | Levanta 6 PostgreSQL + Zookeeper + Kafka + Kafka UI |

## Archivos de Base de Datos (database/)

Cada servicio tiene scripts SQL en su carpeta `database/` o `src/main/resources/db/migration/`:

| Archivo | Que hace |
|---------|---------|
| `V1__CREATE_TABLES.sql` | Crea las tablas y triggers de updated_at |
| `V2__ADD_INDEXES.sql` | Crea indices para mejorar rendimiento de consultas |
| `V3__INSERT_DATA.sql` | Inserta datos iniciales (usuarios, productos) |
| `V4__CREATE_ROLES_TABLE.sql` | (solo user-service) Crea la tabla de roles |

Flyway ejecuta estos scripts automaticamente al arrancar cada servicio. El prefijo V1, V2, etc. indica el orden de ejecucion.

## Archivos de Configuracion (application.yaml)

| Archivo | Cuando se usa |
|---------|--------------|
| `application.yaml` | Cuando corres el servicio local con Maven |
| `application-kubernetes.yaml` | Cuando corres en K8s (perfil activado con SPRING_PROFILES_ACTIVE=kubernetes) |

La diferencia principal es que en K8s las URLs de BD y Kafka apuntan a `host.docker.internal` (la maquina host desde dentro del pod), mientras que en local apuntan a `localhost`.
