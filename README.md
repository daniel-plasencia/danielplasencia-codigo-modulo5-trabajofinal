# Sistema de Pedidos de Comida - Microservicios

Sistema de pedidos de comida basado en arquitectura de microservicios con Spring Boot 3.4, PostgreSQL, Apache Kafka, Docker y Kubernetes.

## Descripcion del Sistema

6 microservicios independientes que se comunican via REST (sincrono) y Kafka (asincrono):

| Servicio | Puerto Local | NodePort K8s | Base de Datos | Responsabilidad |
|----------|-------------|-------------|---------------|-----------------|
| user-service | 8081 | 30081 | userdb (5432) | Usuarios, autenticacion, JWT |
| product-service | 8082 | 30082 | productdb (5433) | Catalogo de productos |
| order-service | 8083 | 30083 | orderdb (5434) | Pedidos, publica eventos Kafka |
| payment-service | 8084 | 30084 | paymentdb (5435) | Pagos (consume Kafka) |
| delivery-service | 8085 | 30085 | deliverydb (5436) | Entregas (consume Kafka) |
| notification-service | 8086 | 30086 | notificationdb (5437) | Notificaciones (consume Kafka) |

## Tecnologias

- Java 21, Spring Boot 3.4.2
- PostgreSQL 15 (una BD por servicio)
- Apache Kafka (comunicacion asincrona entre servicios)
- JWT con JJWT 0.12.6 (autenticacion)
- Resilience4j 2.2.0 (circuit breaker en product-service y order-service)
- Flyway (migraciones de BD)
- Docker + Kubernetes (Docker Desktop)
- MapStruct + Lombok

## Arquitectura

```
                        REST + JWT
  Cliente ──────────► user-service ──► JWT Token
     │
     ├─ REST + JWT ──► product-service ──► productdb
     │
     └─ REST + JWT ──► order-service ──► orderdb
                            │
                      Kafka: orders.events
                            │
              ┌─────────────┼─────────────────┐
              ▼             ▼                  ▼
        payment-service  notification-service
         paymentdb        notificationdb
              │
        Kafka: payments.events
              │
     ┌────────┼────────────┐
     ▼        ▼            ▼
order-service delivery-service notification-service
 (update)     deliverydb
                  │
            Kafka: deliveries.events
                  │
                  ▼
           notification-service
```

## Prerrequisitos

- Java 21
- Maven 3.8+
- Docker Desktop con Kubernetes habilitado
- kubectl

## Despliegue Rapido

### 1. Levantar infraestructura (BDs + Kafka)

```bash
docker compose up -d
```

Esperar ~15 segundos hasta que las BDs esten healthy:

```bash
docker ps
```

### 2. Compilar los 6 microservicios

```bash
cd user-service && mvn clean package -DskipTests && cd ..
cd product-service && mvn clean package -DskipTests && cd ..
cd order-service && mvn clean package -DskipTests && cd ..
cd payment-service && mvn clean package -DskipTests && cd ..
cd delivery-service && mvn clean package -DskipTests && cd ..
cd notification-service && mvn clean package -DskipTests && cd ..
```

### 3. Construir imagenes Docker

```bash
docker build -t user-service:1.0 ./user-service
docker build -t product-service:1.0 ./product-service
docker build -t order-service:1.0 ./order-service
docker build -t payment-service:1.0 ./payment-service
docker build -t delivery-service:1.0 ./delivery-service
docker build -t notification-service:1.0 ./notification-service
```

### 4. Desplegar en Kubernetes

```bash
kubectl apply -f user-service/k8s/
kubectl apply -f product-service/k8s/
kubectl apply -f order-service/k8s/
kubectl apply -f payment-service/k8s/
kubectl apply -f delivery-service/k8s/
kubectl apply -f notification-service/k8s/
```

### 5. Verificar

```bash
kubectl get pods --all-namespaces -l app
```

Los 6 pods deben estar en estado `Running 1/1`.

## Ejecucion Local (sin Kubernetes)

Si prefieres correr los servicios sin K8s, levanta la infraestructura con Docker Compose y ejecuta cada servicio con Maven:

```bash
docker compose up -d
cd user-service && mvn spring-boot:run
```

Repetir para cada servicio en terminales separadas.

## Usuarios de Prueba

| Email | Password | Rol |
|-------|----------|-----|
| juan.perez@example.com | admin123 | ADMIN |
| maria.garcia@example.com | user123 | USER |
| ana.torres@example.com | admin123 | ADMIN |

## Limpiar Todo

```bash
kubectl delete namespace user-service product-service order-service payment-service delivery-service notification-service
docker compose down -v
docker rmi user-service:1.0 product-service:1.0 order-service:1.0 payment-service:1.0 delivery-service:1.0 notification-service:1.0
```

## Documentacion Adicional

- [README_FUNCIONAL.md](./README_FUNCIONAL.md) - Como funciona el sistema, flujo de eventos, como probar con Postman
- [README_ARCHIVOS.md](./README_ARCHIVOS.md) - Que hace cada archivo del proyecto
- [Proyecto_Arquitectura_Microservicios_Pedidos_Comida.md](./Proyecto_Arquitectura_Microservicios_Pedidos_Comida.md) - Documento de arquitectura
