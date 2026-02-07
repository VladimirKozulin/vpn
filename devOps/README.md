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
