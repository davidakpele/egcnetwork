# 📍 Geofencing Service

A production-ready geofencing platform built with **Spring Boot** and **Java 21**. Tracks moving users in real time, fires enter/exit/dwell events when they cross virtual geographic boundaries, and delivers alerts through REST, WebSocket, gRPC, and Kafka simultaneously.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [WebSocket](#websocket)
- [gRPC](#grpc)
- [Kafka Topics](#kafka-topics)
- [Redis Caching Strategy](#redis-caching-strategy)
- [Security](#security)
- [Testing the Endpoints](#testing-the-endpoints)
- [Common Errors & Fixes](#common-errors--fixes)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client / Mobile App                       │
└──────┬──────────────┬──────────────┬────────────────────────────┘
       │ REST (8080)  │ WebSocket    │ gRPC (9090)
       ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                        │
│                                                                   │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │ REST        │  │ WebSocket    │  │ gRPC Services        │   │
│  │ Controllers │  │ Handler      │  │ Location / Geofence  │   │
│  └──────┬──────┘  └──────┬───────┘  │ Alert               │   │
│         │                │           └──────────┬───────────┘   │
│         └────────────────┴──────────────────────┘               │
│                           │                                       │
│              ┌────────────▼────────────┐                         │
│              │     Service Layer        │                         │
│              │  AuthService            │                         │
│              │  LocationService ──────►│──► Geofence Detection  │
│              │  GeofenceService        │    (PostGIS ST_Contains)│
│              │  AlertService           │                         │
│              │  CacheService           │                         │
│              └────────┬───────────────┘                         │
│                        │                                          │
│         ┌──────────────┼──────────────┐                         │
│         ▼              ▼              ▼                          │
│    PostgreSQL       Redis          Kafka                         │
│    + PostGIS      (Cache +       (Event Bus)                    │
│    (Persistence)   Pub/Sub)                                      │
└─────────────────────────────────────────────────────────────────┘
```

### Request Flow (Location Update)

```
Client sends location
        │
        ▼
LocationController.updateLocation()
        │
        ▼
LocationService.processLocationUpdate()
        ├── Save to PostgreSQL (location_updates table)
        ├── Cache latest location in Redis (TTL 30s)
        ├── Push to location history ring buffer in Redis
        ├── Update TrackingSession stats
        ├── Query PostGIS: which geofences contain this point?
        │       ├── ENTER detected → save GeofenceEvent → create Alert → publish Kafka
        │       ├── EXIT  detected → save GeofenceEvent → create Alert → publish Kafka
        │       └── DWELL detected → save GeofenceEvent → create Alert → publish Kafka
        ├── Publish KafkaLocationEvent → geofencing.location.updates
        ├── Push to user's WebSocket session
        └── Check battery / speed thresholds → create system Alerts
```

---

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Framework | Spring Boot | 3.2.0 |
| Language | Java | 21 |
| Database | PostgreSQL + PostGIS | 16 + 3.4 |
| Cache / State | Redis (Lettuce) | 7 |
| Message Bus | Apache Kafka | 7.5 |
| Real-time Push | WebSocket (SockJS) | — |
| RPC | gRPC + Protocol Buffers | 1.60.0 / 3.25.1 |
| Auth | JWT (JJWT 0.12.3) | 0.12.3 |
| ORM | Spring Data JPA + Hibernate Spatial | — |
| Migrations | Flyway | — |
| Build | Maven | 3.x |
| Containerisation | Docker + Docker Compose | — |

---

## Project Structure

```
geofencing-service/
├── docker-compose.yml                        # PostgreSQL, Redis, Kafka, Zookeeper, Kafka UI
├── pom.xml                                   # Maven dependencies
└── src/main/
    ├── proto/
    │   └── geofencing.proto                  # gRPC service definitions
    ├── resources/
    │   ├── application.yml                   # All configuration
    │   └── db/migration/
    │       └── V1__initial_schema.sql        # Flyway migration (PostGIS schema)
    └── java/com/geofence/
        ├── GeofencingServiceApplication.java # Entry point
        ├── config/
        │   ├── SecurityConfig.java           # Spring Security, CORS, JWT filter
        │   ├── RedisConfig.java              # RedisTemplate with JSON serialisation
        │   ├── KafkaConfig.java              # Topic declarations
        │   ├── KafkaProducerConfig.java      # Producer factory, idempotent producer
        │   ├── KafkaConsumerConfig.java      # Consumer factory, listener factory
        │   ├── WebSocketConfig.java          # WebSocket + SockJS endpoints
        │   └── GrpcServerConfig.java         # gRPC server on port 9090
        ├── controller/
        │   ├── AuthController.java           # Register, login, refresh, logout
        │   ├── UserController.java           # Profile CRUD
        │   ├── LocationController.java       # Location updates, history, tracking
        │   ├── GeofenceController.java       # Geofence CRUD + spatial queries
        │   └── AlertController.java          # Alert management
        ├── grpc/
        │   ├── LocationGrpcService.java      # Unary + bidirectional streaming
        │   ├── GeofenceGrpcService.java      # Geofence CRUD over gRPC
        │   └── AlertGrpcService.java         # Alert streaming over gRPC
        ├── kafka/
        │   ├── KafkaProducerService.java     # Publishes to 3 topics
        │   └── KafkaConsumerService.java     # Consumes and routes to WebSocket
        ├── websocket/
        │   └── LocationWebSocketHandler.java # Multi-device, JWT-authenticated WS
        ├── service/
        │   ├── AuthService.java              # Registration, login, token management
        │   ├── UserService.java              # User CRUD
        │   ├── DeviceService.java            # Device session management
        │   ├── LocationService.java          # Core engine: geofence detection pipeline
        │   ├── GeofenceService.java          # Geofence CRUD + spatial helpers
        │   ├── AlertService.java             # Alert creation, read, acknowledge
        │   ├── CacheService.java             # All Redis operations
        │   └── ScheduledTaskService.java     # Nightly token cleanup
        ├── repository/
        │   ├── UserRepository.java
        │   ├── RefreshTokenRepository.java
        │   ├── GeofenceRepository.java       # Native PostGIS spatial queries
        │   ├── LocationUpdateRepository.java
        │   ├── GeofenceEventRepository.java
        │   ├── AlertRepository.java
        │   ├── TrackingSessionRepository.java
        │   └── DeviceSessionRepository.java
        ├── model/
        │   ├── entity/                       # JPA entities (8 tables)
        │   ├── dto/                          # Request/response DTOs (14 classes)
        │   └── enums/                        # 7 enums
        ├── security/
        │   ├── JwtTokenProvider.java         # Sign, parse, validate JWT
        │   ├── JwtAuthenticationFilter.java  # Per-request JWT extraction
        │   ├── UserDetailsImpl.java
        │   └── UserDetailsServiceImpl.java
        ├── exception/
        │   ├── GeofencingException.java      # Domain exception with HTTP status
        │   └── GlobalExceptionHandler.java   # @RestControllerAdvice
        └── util/
            ├── GeometryUtils.java            # JTS geometry helpers, Haversine distance
            └── SecurityUtils.java            # Get current user from SecurityContext
```

---

## Database Schema

Eight tables managed by Flyway migration `V1__initial_schema.sql`. Requires the **PostGIS** extension.

```
users
├── id (UUID PK)
├── username, email, password
├── first_name, last_name, phone
├── role (USER | ADMIN | OPERATOR)
├── status (ACTIVE | INACTIVE | SUSPENDED | PENDING_VERIFICATION)
├── email_verified, email_verification_token
├── password_reset_token, password_reset_expires
└── last_login, created_at, updated_at

refresh_tokens
├── id, user_id (FK → users)
├── token, expires_at, revoked
└── device_info, ip_address

geofences
├── id, name, description, owner_id (FK → users)
├── geofence_type (POLYGON | CIRCLE | RECTANGLE)
├── geometry  GEOMETRY(GEOMETRY, 4326)   ← PostGIS, spatially indexed
├── center_point GEOMETRY(POINT, 4326)
├── radius_meters, color
├── is_active, is_global
├── enter_alert, exit_alert, dwell_alert, dwell_time_seconds
└── metadata (JSONB)

location_updates
├── id, user_id (FK → users)
├── location  GEOMETRY(POINT, 4326)      ← spatially indexed
├── latitude, longitude, altitude
├── accuracy, speed, heading
├── battery_level, device_id, provider
└── timestamp  ← indexed DESC

geofence_events
├── id, user_id, geofence_id
├── event_type (ENTER | EXIT | DWELL)
├── location GEOMETRY(POINT, 4326)
├── latitude, longitude
├── dwell_duration_seconds
└── occurred_at  ← indexed DESC

alerts
├── id, user_id, geofence_id, geofence_event_id
├── alert_type (GEOFENCE_ENTER | GEOFENCE_EXIT | GEOFENCE_DWELL |
│              SPEED_ALERT | BATTERY_LOW | TRACKING_STARTED | TRACKING_STOPPED)
├── severity (INFO | WARNING | CRITICAL)
├── title, message
├── is_read, is_acknowledged, acknowledged_at
└── metadata (JSONB)

tracking_sessions
├── id, user_id, device_id
├── started_at, ended_at
├── total_distance_meters, update_count
└── status (ACTIVE | ENDED | PAUSED)

device_sessions
├── id, user_id, device_id
├── device_name, device_type, os_version, app_version
├── push_token, is_active, last_seen
└── UNIQUE(user_id, device_id)

geofence_user_access
├── geofence_id, user_id
├── access_level (READ | WRITE | ADMIN)
└── granted_by, granted_at
```

**Key PostGIS queries used:**

```sql
-- Point-in-polygon (enter/exit detection)
SELECT * FROM geofences
WHERE is_active = true
AND ST_Contains(geometry, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326));

-- Radius search (nearby geofences)
SELECT * FROM geofences
WHERE ST_DWithin(geometry::geography,
                 ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                 :radiusMeters);

-- Distance to boundary
SELECT ST_Distance(geometry::geography,
                   ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)
FROM geofences WHERE id = :id;
```

---

## Features

### User Management
- Register with username, email, password, name, phone
- JWT access token (24h) + refresh token (7d)
- Multi-device login — each device gets its own device session
- Refresh token rotation — old token revoked on each refresh
- Password change with immediate token revocation
- Soft delete (status → INACTIVE)
- Role-based access: `USER`, `ADMIN`, `OPERATOR`

### Geofence Management
- Three shape types: **CIRCLE** (center + radius), **POLYGON** (arbitrary coordinates), **RECTANGLE**
- Full CRUD with ownership enforcement
- Activate / deactivate without deleting
- Per-geofence alert configuration (enter, exit, dwell)
- Configurable dwell time threshold (default 300 seconds)
- Custom colour per geofence
- Arbitrary JSON metadata field
- Spatial query: which geofences contain a given point?
- Spatial query: nearest N geofences to a given point?

### Location Tracking
- Single location update via REST or gRPC
- Bidirectional streaming via gRPC (`StreamLocation`)
- Tracking sessions with cumulative distance and update count
- Full location history with time-range filtering and pagination
- Latest location served from Redis cache (30s TTL), falls back to DB

### Geofence Event Detection (Core Engine)
Every location update runs the detection pipeline:

1. Query PostGIS for all active geofences containing the new point
2. Compare against cached previous state (Redis)
3. Fire **ENTER** event for geofences newly entered
4. Fire **EXIT** event for geofences just left (includes dwell duration)
5. Fire **DWELL** event when user stays inside longer than `dwell_time_seconds`
6. Persist `GeofenceEvent` to PostgreSQL
7. Create `Alert` record
8. Publish `KafkaGeofenceEvent` to Kafka
9. Push event to user's WebSocket connection(s)

### Alerts
- Auto-created for: ENTER, EXIT, DWELL, SPEED (>200 km/h), BATTERY LOW (<15%), TRACKING START/STOP
- Severity levels: INFO, WARNING, CRITICAL
- Mark individual alert as read / acknowledged
- Mark all alerts as read in one call
- Unread count endpoint for badge display

### Real-time Delivery
- **WebSocket** — JWT-authenticated, multi-device, SockJS fallback
- **gRPC streaming** — bidirectional location stream, server-push alert stream
- **Kafka** — decoupled event fan-out to any downstream consumers

---

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Java | 21+ | Runtime |
| Maven | 3.8+ | Build |
| Docker | 24+ | Infrastructure |
| Docker Compose | 2.x | Orchestration |
| grpcurl | any | gRPC testing (optional) |

---

## Getting Started

### 1. Clone and enter the project

```bash
git clone <your-repo-url>
cd geofencing-service
```

### 2. Start all infrastructure

```bash
docker-compose up -d
```

This starts:
- **PostgreSQL 16 + PostGIS** on port `5432`
- **Redis 7** on port `6379`
- **Zookeeper** on port `2181`
- **Kafka** on port `9092`
- **Kafka UI** on port `8090` → http://localhost:8090

Wait ~15 seconds for Kafka to fully initialise.

### 3. Build and run the application

```bash
# First run: generates protobuf Java classes
mvn clean generate-sources

# Build and run
mvn spring-boot:run
```

The application starts on:
- REST API → `http://localhost:8080`
- gRPC → `localhost:9090`
- WebSocket → `ws://localhost:8080/ws/location-native`
- Actuator → `http://localhost:8080/actuator/health`

### 4. Verify it is running

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

---

## Configuration

All configuration is in `src/main/resources/application.yml`. Every sensitive value can be overridden with an environment variable:

| Property | Env Variable | Default | Description |
|---|---|---|---|
| `spring.datasource.url` | — | `jdbc:postgresql://localhost:5432/geofencing_db` | PostgreSQL URL |
| `spring.datasource.username` | `DB_USERNAME` | `geofence_user` | DB user |
| `spring.datasource.password` | `DB_PASSWORD` | `geofence_pass` | DB password |
| `spring.data.redis.host` | `REDIS_HOST` | `localhost` | Redis host |
| `spring.data.redis.port` | `REDIS_PORT` | `6379` | Redis port |
| `spring.data.redis.password` | `REDIS_PASSWORD` | _(empty)_ | Redis auth |
| `spring.kafka.bootstrap-servers` | `KAFKA_SERVERS` | `localhost:9092` | Kafka brokers |
| `jwt.secret` | `JWT_SECRET` | _(long default)_ | HMAC-SHA512 key |
| `jwt.expiration` | — | `86400000` | Access token TTL ms (24h) |
| `jwt.refresh-expiration` | — | `604800000` | Refresh token TTL ms (7d) |
| `grpc.server.port` | — | `9090` | gRPC server port |
| `geofencing.location.history-ttl-hours` | — | `24` | Redis location history TTL |
| `geofencing.location.max-history-per-user` | — | `1000` | Ring buffer max entries |
| `geofencing.cache.location-ttl-seconds` | — | `30` | Latest location cache TTL |
| `geofencing.cache.geofence-ttl-seconds` | — | `600` | Geofence object cache TTL |

**Production override example:**

```bash
export DB_PASSWORD=supersecret
export REDIS_PASSWORD=redispass
export JWT_SECRET=my-256-bit-production-secret-key-here-must-be-long
export KAFKA_SERVERS=kafka1:9092,kafka2:9092
mvn spring-boot:run
```

---

## API Reference

All REST responses follow this envelope:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { },
  "error": null,
  "timestamp": "2025-01-15T10:30:00"
}
```

Protected endpoints require the header:
```
Authorization: Bearer <accessToken>
```

---

### Authentication  `POST /api/v1/auth`

#### Register
```
POST /api/v1/auth/register
```
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "securepass123",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+2348012345678"
}
```
Response: `AuthResponse` with `accessToken` and `refreshToken`

#### Login
```
POST /api/v1/auth/login
```
```json
{
  "usernameOrEmail": "johndoe",
  "password": "securepass123",
  "deviceId": "iphone-uuid-001",
  "deviceName": "John's iPhone",
  "deviceType": "iOS"
}
```

#### Refresh Token
```
POST /api/v1/auth/refresh?refreshToken=<token>
```

#### Logout
```
POST /api/v1/auth/logout                       🔒 requires auth
```

#### Change Password
```
PUT /api/v1/auth/change-password               🔒 requires auth
```
```json
{
  "currentPassword": "securepass123",
  "newPassword": "evenmoresecure456"
}
```

---

### Users  `GET|PUT|DELETE /api/v1/users`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/users/me` | 🔒 | Get own profile |
| PUT | `/api/v1/users/me` | 🔒 | Update name / phone |
| DELETE | `/api/v1/users/me` | 🔒 | Soft-delete own account |
| GET | `/api/v1/users/{userId}` | 🔒 ADMIN | Get any user |

Update request body:
```json
{
  "firstName": "Jonathan",
  "lastName": "Doe",
  "phone": "+2348099999999"
}
```

---

### Location  `/api/v1/locations`

#### Update Location
```
POST /api/v1/locations                         🔒 requires auth
```
```json
{
  "latitude": 6.5244,
  "longitude": 3.3792,
  "altitude": 50.0,
  "accuracy": 10.0,
  "speed": 1.4,
  "heading": 90.0,
  "batteryLevel": 75,
  "deviceId": "iphone-uuid-001",
  "provider": "GPS",
  "timestamp": "2025-01-15T10:30:00"
}
```
This call triggers the full geofence detection pipeline.

#### Get Current Location
```
GET /api/v1/locations/current                  🔒 requires auth
```
Returns the latest cached or persisted location.

#### Location History
```
GET /api/v1/locations/history                  🔒 requires auth
    ?page=0&size=50
    &from=2025-01-01T00:00:00
    &to=2025-01-15T23:59:59
```

#### Geofence Events
```
GET /api/v1/locations/events?page=0&size=20    🔒 requires auth
```
Returns all ENTER / EXIT / DWELL events for the current user.

#### Tracking Session
```
POST /api/v1/locations/tracking/start?deviceId=iphone-uuid-001   🔒
POST /api/v1/locations/tracking/stop                              🔒
```

#### Admin Endpoints
```
GET /api/v1/locations/{userId}/current         🔒 ADMIN only
GET /api/v1/locations/{userId}/history         🔒 ADMIN only
```

---

### Geofences  `/api/v1/geofences`

#### Create — Circle
```
POST /api/v1/geofences                         🔒 requires auth
```
```json
{
  "name": "Head Office",
  "description": "Main company premises",
  "geofenceType": "CIRCLE",
  "centerLat": 6.5244,
  "centerLng": 3.3792,
  "radiusMeters": 200,
  "enterAlert": true,
  "exitAlert": true,
  "dwellAlert": true,
  "dwellTimeSeconds": 300,
  "color": "#FF5733",
  "metadata": {
    "category": "work",
    "building": "Block A"
  }
}
```

#### Create — Polygon
```json
{
  "name": "Restricted Zone",
  "geofenceType": "POLYGON",
  "coordinates": [
    { "latitude": 6.5200, "longitude": 3.3700 },
    { "latitude": 6.5300, "longitude": 3.3700 },
    { "latitude": 6.5300, "longitude": 3.3900 },
    { "latitude": 6.5200, "longitude": 3.3900 }
  ],
  "enterAlert": true,
  "exitAlert": true
}
```

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/geofences` | Create geofence |
| GET | `/api/v1/geofences` | List own geofences (paginated) |
| GET | `/api/v1/geofences?activeOnly=true` | List only active geofences |
| GET | `/api/v1/geofences/{id}` | Get single geofence |
| PUT | `/api/v1/geofences/{id}` | Update geofence |
| DELETE | `/api/v1/geofences/{id}` | Delete geofence |
| PATCH | `/api/v1/geofences/{id}/activate` | Activate geofence |
| PATCH | `/api/v1/geofences/{id}/deactivate` | Deactivate geofence |
| GET | `/api/v1/geofences/contains?lat=6.52&lng=3.38` | Which geofences contain this point? |
| GET | `/api/v1/geofences/nearby?lat=6.52&lng=3.38&limit=10` | Nearest N geofences |

---

### Alerts  `/api/v1/alerts`

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/alerts?page=0&size=20` | All alerts (paginated) |
| GET | `/api/v1/alerts?unreadOnly=true` | Unread alerts only |
| GET | `/api/v1/alerts/unread-count` | `{ "unreadCount": 5 }` |
| PATCH | `/api/v1/alerts/{id}/read` | Mark one alert as read |
| PATCH | `/api/v1/alerts/{id}/acknowledge` | Acknowledge one alert |
| POST | `/api/v1/alerts/mark-all-read` | Mark all as read |

---

## WebSocket

### Connection

Connect with a valid JWT token:

```
ws://localhost:8080/ws/location-native?token=<accessToken>
```

SockJS fallback (for browsers without native WS):
```
http://localhost:8080/ws/location
```

### Messages you send (client → server)

```json
// Keep-alive ping
{ "type": "ping" }

// Subscribe to a specific geofence's events
{ "type": "subscribe_geofence", "geofenceId": "uuid-here" }
```

### Messages you receive (server → client)

```json
// Connection confirmation
{ "type": "connected", "userId": "uuid", "message": "WebSocket connected" }

// Pong
{ "type": "pong", "timestamp": 1705315800000 }

// Real-time location update confirmation
{
  "type": "location_update",
  "data": {
    "id": "uuid", "userId": "uuid",
    "latitude": 6.5244, "longitude": 3.3792,
    "speed": 1.4, "heading": 90.0,
    "timestamp": "2025-01-15T10:30:00",
    "activeGeofences": [
      { "geofenceId": "uuid", "name": "Head Office", "inside": true, "distanceToBoundaryMeters": 0 }
    ]
  }
}

// Geofence transition event
{
  "type": "geofence_event",
  "data": {
    "id": "uuid", "userId": "uuid",
    "geofenceId": "uuid", "geofenceName": "Head Office",
    "eventType": "ENTER",
    "latitude": 6.5244, "longitude": 3.3792,
    "occurredAt": "2025-01-15T10:30:00"
  }
}

// Alert pushed in real time
{
  "type": "alert",
  "data": {
    "id": "uuid", "alertType": "GEOFENCE_ENTER",
    "severity": "INFO",
    "title": "Entered Head Office",
    "message": "You entered the geofenced area: Head Office",
    "isRead": false
  }
}
```

### Browser test (paste in console)

```javascript
const token = 'YOUR_JWT_TOKEN_HERE';
const ws = new WebSocket(`ws://localhost:8080/ws/location-native?token=${token}`);

ws.onopen    = ()    => { console.log('✅ Connected'); ws.send(JSON.stringify({ type: 'ping' })); };
ws.onmessage = (e)   => console.log('📨 Message:', JSON.parse(e.data));
ws.onerror   = (e)   => console.error('❌ Error:', e);
ws.onclose   = (e)   => console.log('🔌 Closed:', e.code, e.reason);
```

---

## gRPC

The gRPC server runs on port **9090** with server reflection enabled.

### Proto services defined in `geofencing.proto`

```protobuf
service LocationService {
  rpc UpdateLocation   (LocationRequest)        returns (LocationResponse);
  rpc StreamLocation   (stream LocationRequest) returns (stream LocationEvent);  // bidirectional
  rpc GetCurrentLocation (UserRequest)          returns (LocationResponse);
  rpc GetLocationHistory (LocationHistoryRequest) returns (LocationHistoryResponse);
  rpc StartTracking    (TrackingRequest)        returns (TrackingResponse);
  rpc StopTracking     (TrackingRequest)        returns (TrackingResponse);
}

service GeofenceService {
  rpc CreateGeofence       (CreateGeofenceRequest) returns (GeofenceResponse);
  rpc UpdateGeofence       (UpdateGeofenceRequest) returns (GeofenceResponse);
  rpc DeleteGeofence       (DeleteGeofenceRequest) returns (DeleteResponse);
  rpc GetGeofence          (GetGeofenceRequest)    returns (GeofenceResponse);
  rpc ListGeofences        (ListGeofencesRequest)  returns (ListGeofencesResponse);
  rpc CheckPointInGeofences (PointCheckRequest)    returns (PointCheckResponse);
  rpc WatchGeofenceEvents  (WatchRequest)          returns (stream GeofenceEvent);
}

service AlertService {
  rpc GetAlerts       (GetAlertsRequest)    returns (GetAlertsResponse);
  rpc MarkAlertRead   (AlertActionRequest)  returns (AlertResponse);
  rpc AcknowledgeAlert (AlertActionRequest) returns (AlertResponse);
  rpc StreamAlerts    (WatchRequest)        returns (stream AlertProto);
}
```

### Testing with grpcurl

```bash
# Install grpcurl: https://github.com/fullstorydev/grpcurl/releases
# macOS: brew install grpcurl

# List all available services
grpcurl -plaintext localhost:9090 list

# Describe the LocationService
grpcurl -plaintext localhost:9090 describe geofencing.LocationService

# Update a location
grpcurl -plaintext -d '{
  "user_id": "USER-UUID-HERE",
  "latitude": 6.5244,
  "longitude": 3.3792,
  "altitude": 50.0,
  "accuracy": 10.0,
  "speed": 1.4,
  "battery_level": 80,
  "device_id": "device-001",
  "provider": "GPS"
}' localhost:9090 geofencing.LocationService/UpdateLocation

# Create a geofence
grpcurl -plaintext -d '{
  "owner_id": "USER-UUID-HERE",
  "name": "My Zone",
  "geofence_type": "CIRCLE",
  "center_lat": 6.5244,
  "center_lng": 3.3792,
  "radius_meters": 300.0,
  "enter_alert": true,
  "exit_alert": true
}' localhost:9090 geofencing.GeofenceService/CreateGeofence

# Start tracking session
grpcurl -plaintext -d '{
  "user_id": "USER-UUID-HERE",
  "device_id": "device-001"
}' localhost:9090 geofencing.LocationService/StartTracking

# Get current location
grpcurl -plaintext -d '{ "user_id": "USER-UUID-HERE" }' \
  localhost:9090 geofencing.LocationService/GetCurrentLocation
```

---

## Kafka Topics

Three topics are auto-created on startup:

| Topic | Partitions | Key | Payload class | Consumer |
|---|---|---|---|---|
| `geofencing.location.updates` | 6 | `userId` | `KafkaLocationEvent` | `KafkaConsumerService` → admin WebSocket |
| `geofencing.geofence.events` | 3 | `userId` | `KafkaGeofenceEvent` | `KafkaConsumerService` → user WebSocket |
| `geofencing.alerts` | 3 | `userId` | `AlertDto` | `KafkaConsumerService` → user WebSocket |

### KafkaLocationEvent payload
```json
{
  "eventId": "uuid",
  "userId": "uuid",
  "latitude": 6.5244,
  "longitude": 3.3792,
  "altitude": 50.0,
  "speed": 1.4,
  "heading": 90.0,
  "batteryLevel": 75,
  "deviceId": "device-001",
  "timestamp": "2025-01-15T10:30:00",
  "sessionId": "uuid"
}
```

### KafkaGeofenceEvent payload
```json
{
  "eventId": "uuid",
  "userId": "uuid",
  "geofenceId": "uuid",
  "geofenceName": "Head Office",
  "eventType": "ENTER",
  "latitude": 6.5244,
  "longitude": 3.3792,
  "dwellDurationSeconds": null,
  "occurredAt": "2025-01-15T10:30:00"
}
```

**Kafka UI** is available at `http://localhost:8090` to browse messages live.

---

## Redis Caching Strategy

| Key Pattern | Type | TTL | Content |
|---|---|---|---|
| `user:location:{userId}` | String | 30s | Latest `LocationDto` (JSON) |
| `location:history:{userId}` | List | 24h | Ring buffer of last 1000 locations |
| `geofence:{geofenceId}` | String | 600s | Cached `GeofenceDto` |
| `geofence:state:{userId}:{geofenceId}` | String | 24h | `true`/`false` — is user inside? |
| `user:geofences:{userId}` | Set | 24h | Set of geofence IDs user is currently inside |
| `dwell:{userId}:{geofenceId}` | String | 24h | Epoch ms when user entered (dwell timer) |
| `dwell:fired:{userId}:{geofenceId}` | String | dwell_time_seconds | Flag to prevent duplicate dwell alerts |
| `rate:{action}:{userId}` | String | window | Rate limit counter |

The geofence state cache is critical — it enables O(1) detection of ENTER/EXIT transitions without querying the event history table.

---

## Security

### Authentication flow

```
1. Client sends credentials → POST /api/v1/auth/login
2. Server validates, returns accessToken (24h) + refreshToken (7d)
3. Client sends: Authorization: Bearer <accessToken> on every request
4. JwtAuthenticationFilter extracts and validates the token per-request
5. When accessToken expires → POST /api/v1/auth/refresh (old refresh token revoked, new pair issued)
6. On logout → all refresh tokens for the user are revoked
7. On password change → all refresh tokens revoked (forces re-login on all devices)
```

### JWT structure

```
Header: { "alg": "HS512" }
Payload: {
  "sub": "user-uuid",
  "username": "johndoe",
  "role": "USER",
  "type": "access",
  "iat": 1705315800,
  "exp": 1705402200
}
```

### Role permissions

| Role | Permissions |
|---|---|
| `USER` | Own profile, own geofences, own location, own alerts |
| `ADMIN` | All USER permissions + any user's data + admin endpoints |
| `OPERATOR` | USER permissions + WebSocket admin broadcast feed |

### WebSocket auth

Token is passed as a query parameter on connect:
```
ws://localhost:8080/ws/location-native?token=<accessToken>
```
Or as a header: `Authorization: Bearer <token>`

---

## Testing the Endpoints

### Step-by-step Postman flow

**Step 1 — Register**
```
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Step 2 — Login, save the token**
```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "testuser",
  "password": "password123",
  "deviceId": "postman-device-01"
}
```
Copy `data.accessToken` from the response. In Postman set it as a collection variable `{{token}}` and use `Authorization: Bearer {{token}}` on all subsequent requests.

**Step 3 — Create a geofence around your location**
```
POST http://localhost:8080/api/v1/geofences
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "name": "Test Zone",
  "geofenceType": "CIRCLE",
  "centerLat": 6.5244,
  "centerLng": 3.3792,
  "radiusMeters": 500,
  "enterAlert": true,
  "exitAlert": true,
  "dwellAlert": true,
  "dwellTimeSeconds": 60
}
```
Save the returned `id` as `{{geofenceId}}`.

**Step 4 — Start tracking**
```
POST http://localhost:8080/api/v1/locations/tracking/start?deviceId=postman-device-01
Authorization: Bearer {{token}}
```

**Step 5 — Send location INSIDE the geofence → triggers ENTER**
```
POST http://localhost:8080/api/v1/locations
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "latitude": 6.5244,
  "longitude": 3.3792,
  "batteryLevel": 80,
  "deviceId": "postman-device-01",
  "provider": "GPS"
}
```

**Step 6 — Send location OUTSIDE the geofence → triggers EXIT**
```
POST http://localhost:8080/api/v1/locations
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "latitude": 6.6000,
  "longitude": 3.5000,
  "batteryLevel": 78,
  "deviceId": "postman-device-01",
  "provider": "GPS"
}
```

**Step 7 — Check what was generated**
```
GET http://localhost:8080/api/v1/alerts
GET http://localhost:8080/api/v1/locations/events
GET http://localhost:8080/api/v1/locations/history
```

### cURL quick test

```bash
# Register and store token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"testuser","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")

echo "Token acquired: ${TOKEN:0:20}..."

# Update location
curl -s -X POST http://localhost:8080/api/v1/locations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"latitude":6.5244,"longitude":3.3792,"batteryLevel":80,"deviceId":"curl-device"}' \
  | python3 -m json.tool

# Check alerts
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/alerts | python3 -m json.tool
```

---

## Common Errors & Fixes

| Error | Cause | Fix |
|---|---|---|
| `GeofenceServiceGrpc cannot be resolved` | Proto classes not generated | Run `mvn clean generate-sources` first |
| `cannot find symbol: method subject(String)` | JJWT version mismatch | Ensure `jjwt.version=0.12.3` in `pom.xml` |
| `cannot find symbol: method verifyWith` | Same JJWT mismatch | Same fix as above |
| `getCoordinatesList() undefined` | Parameter type is your DTO, not gRPC class | Change param type to `com.geofence.grpc.CreateGeofenceRequest` |
| `PostGIS function ST_Contains not found` | PostGIS extension missing | Use `postgis/postgis` Docker image, not plain `postgres` |
| `Flyway migration failed` | DB not ready yet | Wait for `pg_isready` healthcheck to pass before starting app |
| `Connection refused: localhost:9092` | Kafka not running | Run `docker-compose up -d` and wait 15s |
| WebSocket returns 403 | Token not passed | Add `?token=<jwt>` to the WebSocket URL |
| `HibernateException: No Dialect mapping for JDBC type: 1111` | Geometry column type not mapped | Ensure `hibernate-spatial` dependency is present in `pom.xml` |
