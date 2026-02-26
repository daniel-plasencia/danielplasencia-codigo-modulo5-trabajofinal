#  Microservicio Product-Service - Observability

```
┌──────────────────────────────────────────────────────────────────┐
│                     Docker Desktop / K8s                         │
│                                                                  │
│  ┌───────────────┐      ┌───────────────┐                        │
│  │ user-service  │      │product-service│                        │
│  │   :8081       │      │   :8082       │                        │
│  │  /actuator/   │      │  /actuator/   │                        │
│  │  prometheus   │      │  prometheus   │                        │
│  └───────┬───────┘      └───────┬───────┘                        │
│          │  métricas            │  métricas                      │
│          ▼                      ▼                                │
│  ┌─────────────────────────────────┐     ┌────────────────┐      │
│  │         PROMETHEUS              │     │    ZIPKIN      │      │
│  │    (scrape cada 15s)            │     │  (recibe trazas│      │
│  │         :9090                   │     │   via HTTP)    │      │
│  └──────────────┬──────────────────┘     │    :9411       │      │
│                 │  datasource            └────────────────┘      │
│                 ▼                                                │
│  ┌─────────────────────────────────┐                             │
│  │          GRAFANA                │                             │
│  │   (dashboards + alertas)        │                             │
│  │          :3000                  │                             │
│  └─────────────────────────────────┘                             │
└──────────────────────────────────────────────────────────────────┘

```

## 1.- Estructura

```

proyecto/
├── docker-compose.yml                         
├── docker-compose-observability.yml                      (NUEVO)
├── observability/
│   ├── prometheus/
│   │   └── prometheus.yml                                 (NUEVO)
│   └── grafana/
│       └── provisioning/
│           ├── datasources/
│           │   └── datasources.yml                        (NUEVO)
│           └── dashboards/
│               ├── dashboards.yml                         (NUEVO)
│               └── json/                                  (vacío, para dashboards exportados)
├── user-service/
│   └── src/main/resources/application.yaml                (MODIFICADO)
│   └── src/main/resources/application-kubernetes.yaml     (MODIFICADO)
└── product-service/
    └── src/main/resources/application.yaml                (MODIFICADO)
    └── src/main/resources/application-kubernetes.yaml     (MODIFICADO)

```

## 2.- Modificar user-service/pom.xml y product-service/pom.xml


```xml
.
.
.
        <!-- ============================================ -->
        <!-- NUEVO - Módulo 5 Sesión 1: Observabilidad    -->
        <!-- ============================================ -->

        <!-- Micrometer → Prometheus (métricas) -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Micrometer Tracing → Brave (trazas distribuidas) -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>

        <!-- Reporter: enviar trazas a Zipkin via HTTP -->
        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-reporter-brave</artifactId>
        </dependency>

.
.
.


```

## 3.- Modificar application.yaml y application-kubernetes.yaml en user-service y product-service

- Agregar al final de los archivos 

```xml

# ============================================
# OBSERVABILIDAD - Módulo 5 Sesión 1
# ============================================
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus    # ← Agregar prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when-authorized
  # Métricas Prometheus
  prometheus:
    metrics:
      export:
        enabled: true
  # Trazas distribuidas
  tracing:
    sampling:
      probability: 1.0        # 1.0 = 100% de trazas (solo para desarrollo)
                                # En producción usar 0.1 (10%)
  # Tags comunes en todas las métricas
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true    # Habilitar histograma de latencia

# Zipkin endpoint
management.zipkin.tracing:
  endpoint: ${ZIPKIN_URL:http://localhost:9411/api/v2/spans}

# Logging con traceId (se inyecta automáticamente)
logging:
  pattern:
    level: "%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]"
  level:
    com.tecsup.app.micro.user: ${LOG_LEVEL:INFO}
    org.hibernate.SQL: ${SQL_LOG_LEVEL:WARN}

```

## 4.- Crear el  docker-compose con stack de observabilidad

- ./docker-compose-observability.yml

```

# ============================================
# Stack de observabilidad para desarrollo
# Módulo 5 - Sesión 1
#
# Uso: docker compose -f docker-compose.yml -f docker-compose-observability.yml up -d
# ============================================

services:

  # ============================================
  # PROMETHEUS - Recolector de métricas
  # ============================================
  prometheus:
    image: prom/prometheus:v2.51.0
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./observability/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"
    restart: unless-stopped

  # ============================================
  # GRAFANA - Dashboards y visualización
  # ============================================
  grafana:
    image: grafana/grafana:10.4.0
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - ./observability/grafana/provisioning:/etc/grafana/provisioning:ro
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus
    restart: unless-stopped

  # ============================================
  # ZIPKIN - Trazas distribuidas
  # ============================================
  zipkin:
    image: openzipkin/zipkin:3.4
    container_name: zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem              # En memoria (desarrollo)
    restart: unless-stopped

volumes:
  grafana-data:
    driver: local

```


## 5.- Configuración de Prometheus

- Archivo : observability/prometheus/prometheus.yml

```
# ============================================
# Prometheus Configuration
# Módulo 5 - Sesión 1
# ============================================

global:
  scrape_interval: 15s          # Cada 15 segundos consulta las métricas
  evaluation_interval: 15s      # Cada 15 segundos evalúa reglas

# ============================================
# SCRAPE CONFIGS - Endpoints a monitorear
# ============================================
scrape_configs:

  # Prometheus se monitorea a sí mismo
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # user-service (corriendo en el host, no en Docker)
  - job_name: 'user-service'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['host.docker.internal:8081']
        labels:
          application: 'user-service'

  # product-service (corriendo en el host, no en Docker)
  - job_name: 'product-service'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    static_configs:
      - targets: ['host.docker.internal:8082']
        labels:
          application: 'product-service'

```
## 6.- Auto-provisioning de Grafana

- Archivo : observability/grafana/provisioning/datasources/datasources.yml

```
# ============================================
# Grafana: Datasource automático (Prometheus)
# ============================================
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
```

- Archivo : observability/grafana/provisioning/dashboards/dashboards.yml

```
# ============================================
# Grafana: Provisioning de dashboards
# ============================================
apiVersion: 1

providers:
  - name: 'default'
    orgId: 1
    folder: 'Microservices'
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /etc/grafana/provisioning/dashboards/json
      foldersFromFilesStructure: false
```

## 7.- SecurityConfig 

- En user-service SecurityConfig.java 
Se tiene :
```
java.requestMatchers("/actuator/health/**").permitAll()
```
Cambiar a:
```
java.requestMatchers("/actuator/**").permitAll()    // Permitir todos los actuator
                                                 // En producción: restringir por IP
```

- En product-service SecurityConfig.java 
Se tiene :
```
java.requestMatchers("/actuator/health/**").permitAll()
```
Cambiar a:
```
java.requestMatchers("/actuator/**").permitAll()    // Permitir todos los actuator
                                                 // En producción: restringir por IP
```

## 8.- Verificar

```
# 1. Levantar PostgreSQL (si no está corriendo)
docker compose up -d

# 2. Levantar stack de observabilidad
docker compose -f docker-compose-observability.yml up -d

# 3. Verificar contenedores
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
# Resultado esperado:
# prometheus     Up    0.0.0.0:9090->9090/tcp
# grafana        Up    0.0.0.0:3000->3000/tcp
# zipkin         Up    0.0.0.0:9411->9411/tcp

# 4. Levantar microservicios (desde IDE o terminal)
cd user-service 
mvn spring-boot:run &

cd product-service 
mvn spring-boot:run &

# 5. Verificar métricas
curl http://localhost:8081/actuator/prometheus | head -20
# Debe mostrar métricas en formato Prometheus:
# # HELP jvm_memory_used_bytes The amount of used memory
# # TYPE jvm_memory_used_bytes gauge
# jvm_memory_used_bytes{area="heap",...} 1.2345678E8

# 6. Verificar Prometheus targets
# Abrir: http://localhost:9090/targets
# Ambos servicios deben estar en estado "UP"

# 7. Verificar Zipkin
# Abrir: http://localhost:9411
# Debe mostrar la UI de Zipkin (vacía por ahora)

# 8. Verificar Grafana
# Abrir: http://localhost:3000
# Login: admin / admin
# Datasource "Prometheus" debe estar pre-configurado
```

## 9.- Dashboards y trazas distribuidas

- Generar tráfico para ver métricas
```
# Login para obtener JWT (si tienes seguridad activa)
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan.perez@example.com","password":"admin123"}' \
  | jq -r '.token')

# Generar tráfico a user-service
for i in $(seq 1 50); do
  curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/users > /dev/null
  echo "Request $i a user-service"
done

# Generar tráfico a product-service (endpoints públicos)
for i in $(seq 1 50); do
  curl -s http://localhost:8082/api/products > /dev/null
  echo "Request $i a product-service"
done

``` 

- Explorar Prometheus

Abrir http://localhost:9090 y ejecutar estas queries:

```
# Query 1: Total de requests por servicio
promqlhttp_server_requests_seconds_count

# Query 2: Tasa de requests por segundo
promqlrate(http_server_requests_seconds_count[1m])

# Query 3: Latencia p95 de cada endpoint
promqlhistogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri, application))

# Query 4: Conexiones de base de datos activas
promqlhikaricp_connections_active

# Query 5: Memoria JVM usada (%)
promqljvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# Query 6: Errores 5xx
promqlsum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application)

```

- Configurar Grafana
```
Paso 1: Importar dashboard Spring Boot

Abrir http://localhost:3000 (admin / admin)
Menú lateral → Dashboards → New → Import
En "Import via grafana.com" escribir: 19004 → Click Load
Seleccionar datasource: Prometheus
Click Import

Este dashboard muestra: request rate, latencia (p50/p95/p99), errores, y estado HTTP.
Paso 2: Importar dashboard JVM

Dashboards → New → Import
Dashboard ID: 4701 → Load
Datasource: Prometheus → Import

Muestra: heap memory, GC pauses, threads, classloading.
Paso 3: Importar dashboard HikariCP

Dashboards → New → Import
Dashboard ID: 6083 → Load
Datasource: Prometheus → Import

Muestra: conexiones activas, idle, pending, timeouts del pool.
Paso 4: Crear panel personalizado

Dashboards → New → New Dashboard → Add visualization
Datasource: Prometheus
Query:

promqlrate(http_server_requests_seconds_count{application="product-service"}[1m])

Title: "Product Service - Requests/seg"
Apply → Save dashboard como "Microservices Overview"
```

- Explorar Zipkin — Trazas distribuidas
```
Paso 1: Ver trazas

Abrir http://localhost:9411
Click Run Query (botón azul)
Debes ver trazas de las peticiones anteriores
Buscar una traza que tenga 2 spans (product-service → user-service)
Click en ella para ver el timeline
```

Paso 2: Analizar una traza cross-service
Buscar una traza de GET /api/products/1:

Paso 3: Verificar traceId en logs
```
INFO [product-service,6f3b2c8d4e1a0f9b,6f3b2c8d4e1a0f9b] REST request to create order
INFO [product-service,6f3b2c8d4e1a0f9b,a1b2c3d4e5f67890] Calling User Service
INFO [user-service,6f3b2c8d4e1a0f9b,f9e8d7c6b5a49382]    GET /api/users/1
                    ^^^^^^^^^^^^^^^^
                    Mismo traceId en ambos servicios
El traceId 6f3b2c8d4e1a0f9b aparece en ambos servicios — puedes buscar este ID en Zipkin para ver la traza completa.
```
