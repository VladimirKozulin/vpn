# VPN Server - Архитектура и Логика Работы

## 🏗️ Архитектура Сервисов

```
┌─────────────────┐
│   Пользователь  │
│    (Браузер)    │
└────────┬────────┘
         │ HTTPS (SSL)
         │ :8080
         ▼
┌─────────────────────────────────────────┐
│      Spring Boot Application            │
│  ┌───────────────────────────────────┐  │
│  │   Spring Security + OAuth2        │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │   Controllers (HomeController)    │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │   Services (VpnClientService,     │  │
│  │   XrayService, QrCodeService)     │  │
│  └───────────────────────────────────┘  │
└──────┬──────────────────┬───────────────┘
       │                  │
       │ gRPC             │ JDBC
       │ :10085           │ :5432
       ▼                  ▼
┌─────────────┐    ┌──────────────┐
│    Xray     │    │  PostgreSQL  │
│   Process   │    │   (vpn_db)   │
│   :443      │    └──────────────┘
└─────────────┘
       │
       │ VLESS + Reality
       ▼
┌─────────────┐
│ VPN Клиент  │
│  (v2rayN)   │
└─────────────┘

┌──────────────────────────────────────────┐
│         Keycloak (Auth Server)           │
│  ┌────────────────────────────────────┐  │
│  │  Realm: vpn                        │  │
│  │  Client: vpn-client                │  │
│  │  Role: user                        │  │
│  └────────────────────────────────────┘  │
└──────┬───────────────────────────────────┘
       │ JDBC :5433
       ▼
┌──────────────┐
│  PostgreSQL  │
│(keycloak_db) │
└──────────────┘
```

## 🔄 Flow 1: Первый Вход Пользователя (С Keycloak)

```
1. Пользователь → https://ip:8080/
   ↓
2. Spring Security видит неавторизованного пользователя
   ↓
3. Редирект → http://localhost:8180/realms/vpn/protocol/openid-connect/auth
   ↓
4. Keycloak показывает форму регистрации/логина
   ↓
5. Пользователь регистрируется (email + пароль)
   ↓
6. Keycloak создаёт пользователя, возвращает authorization code
   ↓
7. Spring Boot обменивает code на JWT token (ID Token + Access Token)
   ↓
8. Spring Security извлекает из JWT:
   - keycloak_user_id (UUID)
   - email
   - preferred_username
   ↓
9. VpnClientService.getOrCreateClient():
   - Ищет vpn_clients по keycloak_user_id
   - НЕ НАЙДЕН → Создаёт новую запись:
     * keycloak_user_id = UUID из JWT
     * email = email из JWT
     * uuid = новый UUID для Xray
     * is_active = true
     * created_at = now()
   ↓
10. XrayGrpcClient.addUser(uuid) → Добавляет пользователя в Xray через gRPC
    ↓
11. ConfigService.generateVlessLink(uuid) → Генерирует VLESS ссылку
    ↓
12. QrCodeService.generateQrCode(vlessLink) → Генерирует QR код
    ↓
13. HomeController возвращает index.html с QR кодом
    ↓
14. Пользователь видит свой QR код
```

## 🔄 Flow 2: Повторный Вход (SSO)

```
1. Пользователь → https://ip:8080/
   ↓
2. Spring Security проверяет сессию
   ↓
3. Keycloak проверяет SSO сессию (cookie)
   ↓
4. Keycloak возвращает JWT token (без повторного логина)
   ↓
5. Spring Security извлекает keycloak_user_id из JWT
   ↓
6. VpnClientService.getOrCreateClient():
   - Ищет vpn_clients по keycloak_user_id
   - НАЙДЕН → Возвращает существующую запись
   ↓
7. Генерируется ТОТ ЖЕ QR код (тот же UUID)
   ↓
8. Пользователь видит свой QR код
```

```

## 🔐 SSL/TLS

- **Spring Boot**: HTTPS на порту 8080 с сертификатом `src/main/resources/ssl/keystore.p12`
- **Xray**: Reality протокол (TLS fingerprint маскировка под Chrome)
- **Keycloak**: HTTP в dev режиме (в продакшене нужен HTTPS)

## ⏱️ TTL Логика (5 минут)

**Pending Clients (в памяти):**
```java
PendingClient {
    uuid: String
    deviceInfo: String
    createdAt: LocalDateTime
    expiresAt: LocalDateTime  // createdAt + 5 минут
}
```

**Процесс:**
1. Создаётся PendingClient при генерации QR кода
2. Планируется задача через 5 минут
3. Через 5 минут:
   - Проверяется статистика подключения через gRPC
   - Если трафик > 0 → сохраняется в БД как VpnClient
   - Если трафик = 0 → удаляется из Xray

## 📊 База Данных

**vpn_clients:**
```sql
id                  BIGSERIAL PRIMARY KEY
keycloak_user_id    UUID UNIQUE NOT NULL
email               VARCHAR(255) UNIQUE NOT NULL
uuid                VARCHAR(36) UNIQUE NOT NULL  -- UUID для Xray
device_info         VARCHAR(255)
is_active           BOOLEAN DEFAULT true
created_at          TIMESTAMP NOT NULL
first_connected_at  TIMESTAMP
last_connected_at   TIMESTAMP
```

**Индексы:**
- idx_keycloak_user_id
- idx_email
- idx_uuid

## 🔗 Связи Между Сервисами

**Spring Boot ↔ Xray:**
- Протокол: gRPC
- Порт: 10085
- Операции: addUser(), removeUser(), getStats()

**Spring Boot ↔ PostgreSQL (vpn_db):**
- Протокол: JDBC
- Порт: 5432
- ORM: Hibernate (JPA)

**Spring Boot ↔ Keycloak:**
- Протокол: OAuth2 / OpenID Connect
- Порты: 8180 (внешний), 8080 (внутри Docker)
- Endpoints:
  - authorization: http://localhost:8180/realms/vpn/protocol/openid-connect/auth
  - token: http://keycloak:8080/realms/vpn/protocol/openid-connect/token
  - jwks: http://keycloak:8080/realms/vpn/protocol/openid-connect/certs

**Keycloak ↔ PostgreSQL (keycloak_db):**
- Протокол: JDBC
- Порт: 5433
- База: keycloak_db

**Xray ↔ VPN Клиент:**
- Протокол: VLESS + Reality (TLS)
- Порт: 443
- Маскировка: www.microsoft.com

## 🚀 Запуск

```bash
```

## 🔑 Keycloak Настройки

- **Realm**: vpn
- **Client ID**: vpn-client
- **Client Secret**: (в .env)
- **Role**: user
- **Valid Redirect URIs**: https://localhost:8080/*


## 🛠️ Полезные Команды

### Docker Контейнеры

```bash
# Посмотреть запущенные контейнеры
docker ps

# Посмотреть все контейнеры (включая остановленные)
docker ps -a

# Посмотреть логи контейнера
docker logs vpn-server
docker logs keycloak
docker logs vpn-postgres
docker logs keycloak-postgres

# Посмотреть логи в реальном времени
docker logs -f vpn-server

# Зайти внутрь контейнера
docker exec -it vpn-server bash
docker exec -it keycloak bash
docker exec -it vpn-postgres bash

# Перезапустить контейнер
docker-compose restart vpn-server
docker-compose restart keycloak

# Остановить все контейнеры
docker-compose down

# Удалить контейнеры и volumes
docker-compose down -v
```

### База Данных VPN (PostgreSQL)

```bash
# Зайти в PostgreSQL контейнер
docker exec -it vpn-postgres psql -U postgres -d vpn_db

# Или через docker-compose
docker-compose exec postgres psql -U postgres -d vpn_db
```

**SQL команды внутри PostgreSQL:**

```sql
-- Посмотреть все таблицы
\dt

-- Описание таблицы vpn_clients
\d vpn_clients

-- Посмотреть всех клиентов
SELECT * FROM vpn_clients;

-- Посмотреть активных клиентов
SELECT id, email, uuid, is_active, created_at FROM vpn_clients WHERE is_active = true;

-- Посмотреть клиента по email
SELECT * FROM vpn_clients WHERE email = 'user@example.com';

-- Посмотреть клиента по Keycloak ID
SELECT * FROM vpn_clients WHERE keycloak_user_id = 'uuid-here';

-- Количество клиентов
SELECT COUNT(*) FROM vpn_clients;

-- Последние 10 зарегистрированных
SELECT email, created_at FROM vpn_clients ORDER BY created_at DESC LIMIT 10;

-- Выйти из psql
\q
```

### База Данных Keycloak (PostgreSQL)

```bash
# Зайти в Keycloak PostgreSQL
docker exec -it keycloak-postgres psql -U keycloak -d keycloak_db

# Или через docker-compose
docker-compose exec keycloak-postgres psql -U keycloak -d keycloak_db
```

**SQL команды для Keycloak:**

```sql
-- Посмотреть все таблицы
\dt

-- Посмотреть всех пользователей
SELECT id, username, email, created_timestamp FROM user_entity;

-- Посмотреть пользователей realm 'vpn'
SELECT u.id, u.username, u.email, u.created_timestamp 
FROM user_entity u 
JOIN realm r ON u.realm_id = r.id 
WHERE r.name = 'vpn';

-- Посмотреть клиентов (applications)
SELECT id, client_id, name, enabled FROM client WHERE realm_id = (SELECT id FROM realm WHERE name = 'vpn');

-- Посмотреть роли realm
SELECT id, name, description FROM keycloak_role WHERE realm_id = (SELECT id FROM realm WHERE name = 'vpn');

-- Выйти
\q
```

### Xray

```bash
# Посмотреть конфигурацию Xray
cat xray-config.json

# Посмотреть логи Xray (через Spring Boot)
docker logs vpn-server | grep Xray

# Проверить работает ли Xray на порту 443
netstat -an | findstr :443

# Или через PowerShell
Test-NetConnection -ComputerName localhost -Port 443
```

### Keycloak

```bash
# Зайти в Keycloak CLI
docker exec -it keycloak /opt/keycloak/bin/kcadm.sh

# Экспортировать realm конфигурацию
docker exec -it keycloak /opt/keycloak/bin/kc.sh export --dir /tmp --realm vpn

# Посмотреть экспортированный файл
docker exec -it keycloak cat /tmp/vpn-realm.json
```

### Проверка Сервисов

```bash
# Проверить доступность Spring Boot
curl -k https://localhost:8080/

# Проверить доступность Keycloak
curl http://localhost:8180/

# Проверить Keycloak realm
curl http://localhost:8180/realms/vpn/.well-known/openid-configuration

# Проверить PostgreSQL VPN
docker exec vpn-postgres pg_isready -U postgres

# Проверить PostgreSQL Keycloak
docker exec keycloak-postgres pg_isready -U keycloak
```

### Очистка и Сброс

```bash
# Удалить все контейнеры и volumes (ОСТОРОЖНО - удалит все данные!)
docker-compose down -v

# Удалить только Keycloak данные
docker volume rm vpn_keycloak-postgres-data

# Удалить только VPN данные
docker volume rm vpn_postgres-data

# Пересоздать всё с нуля
docker-compose down -v
docker-compose up -d

# Пересоздать только Keycloak с автоимпортом realm
docker-compose down keycloak keycloak-postgres
docker volume rm vpn_keycloak-postgres-data
docker-compose up keycloak-postgres keycloak -d
```

### Бэкап и Восстановление

```bash
# Бэкап VPN базы данных
docker exec vpn-postgres pg_dump -U postgres vpn_db > backup_vpn_$(date +%Y%m%d).sql

# Восстановление VPN базы
cat backup_vpn_20260207.sql | docker exec -i vpn-postgres psql -U postgres -d vpn_db

# Бэкап Keycloak базы
docker exec keycloak-postgres pg_dump -U keycloak keycloak_db > backup_keycloak_$(date +%Y%m%d).sql

# Восстановление Keycloak базы
cat backup_keycloak_20260207.sql | docker exec -i keycloak-postgres psql -U keycloak -d keycloak_db
```

### Мониторинг

```bash
# Посмотреть использование ресурсов контейнерами
docker stats

# Посмотреть использование дискового пространства
docker system df

# Посмотреть сетевые подключения
docker network ls
docker network inspect vpn_vpn-network

# Посмотреть volumes
docker volume ls
docker volume inspect vpn_postgres-data
```


Необходимо тебе найти логику истечения 5 минутного ttl и определения подключения пользователя к конкретной vless ссылки. 

Сейчас у нас проверка только через 5 минут, но тогда если пользователь подключился сразу в базе данных пусто, нужно ждать 5 минут перед тем как данные появятся в базе. А при подключении пользователя к серверу у нас тут же появляются логи подключения пользователя к серверу. 

Следовательно мы можем в таком случае отменять 5 минутнуе ожидание и сразу же фиксировать его в базе данных. 

