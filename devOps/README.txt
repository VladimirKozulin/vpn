═══════════════════════════════════════════════════════════════
  СТРУКТУРА DEVOPS
═══════════════════════════════════════════════════════════════

📁 devOps/
├── 📁 data/              # Сервисы для хранения данных
│   └── docker-compose.yml   # PostgreSQL для VPN данных
│
├── 📁 auth/              # Сервисы аутентификации
│   └── docker-compose.yml   # Keycloak + PostgreSQL для Keycloak
│
└── 📁 server/            # Сервисы приложения
    ├── Dockerfile           # Образ VPN сервера
    └── docker-compose.yml   # VPN сервер конфигурация

═══════════════════════════════════════════════════════════════
  ЗАПУСК СЕРВИСОВ
═══════════════════════════════════════════════════════════════

Из корня проекта:

1. Запустить всё:
   docker-compose up -d

2. Запустить только БД VPN:
   docker-compose up postgres -d

3. Запустить только Keycloak (с его БД):
   docker-compose up keycloak-postgres keycloak -d

4. Запустить только сервер:
   docker-compose up vpn-server -d

5. Остановить всё:
   docker-compose down

6. Пересобрать образы:
   docker-compose build
