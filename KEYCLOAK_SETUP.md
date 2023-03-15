# Настройка Keycloak для VPN сервера

## 1. Запуск Keycloak

```bash
docker-compose up -d keycloak postgres
```

Keycloak будет доступен по адресу: http://localhost:8180

## 2. Первоначальная настройка

1. Откройте http://localhost:8180
2. Войдите в Admin Console:
   - Username: `admin`
   - Password: `admin`

## 3. Создание Realm

1. Наведите на "master" в левом верхнем углу
2. Нажмите "Create Realm"
3. Realm name: `vpn-realm`
4. Нажмите "Create"

## 4. Создание ролей

1. В меню слева выберите "Realm roles"
2. Нажмите "Create role"
3. Создайте роль `user`:
   - Role name: `user`
   - Description: `Regular VPN user`
4. Создайте роль `admin`:
   - Role name: `admin`
   - Description: `VPN administrator`

## 5. Создание клиента для мобильного приложения

1. В меню слева выберите "Clients"
2. Нажмите "Create client"
3. Заполните:
   - Client type: `OpenID Connect`
   - Client ID: `vpn-mobile`
4. Нажмите "Next"
5. Настройки:
   - Client authentication: `OFF` (public client)
   - Authorization: `OFF`
   - Authentication flow: включите все галочки
6. Нажмите "Next"
7. Valid redirect URIs:
   - `com.example.vpn://login-callback`
   - `http://localhost:*`
8. Web origins: `*`
9. Нажмите "Save"

## 6. Создание клиента для backend (опционально)

1. Нажмите "Create client"
2. Заполните:
   - Client ID: `vpn-backend`
3. Нажмите "Next"
4. Настройки:
   - Client authentication: `ON` (confidential)
5. Нажмите "Save"
6. Перейдите на вкладку "Credentials"
7. Скопируйте "Client secret" (понадобится для backend)

## 7. Создание тестового пользователя

1. В меню слева выберите "Users"
2. Нажмите "Add user"
3. Заполните:
   - Username: `test@example.com`
   - Email: `test@example.com`
   - Email verified: `ON`
   - First name: `Test`
   - Last name: `User`
4. Нажмите "Create"
5. Перейдите на вкладку "Credentials"
6. Нажмите "Set password"
7. Установите пароль: `test123`
8. Temporary: `OFF`
9. Нажмите "Save"
10. Перейдите на вкладку "Role mapping"
11. Нажмите "Assign role"
12. Выберите `user` и нажмите "Assign"

## 8. Настройка регистрации

1. В меню слева выберите "Realm settings"
2. Перейдите на вкладку "Login"
3. Включите:
   - User registration: `ON`
   - Forgot password: `ON`
   - Remember me: `ON`
4. Нажмите "Save"

## 9. Проверка настройки

### Через HTTP файл:

Откройте `http/keycloak-setup.http` и выполните запрос:

```http
POST http://localhost:8180/realms/vpn-realm/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password
&client_id=vpn-mobile
&username=test@example.com
&password=test123
```

Вы должны получить access_token, refresh_token и id_token.

### Через мобильное приложение:

1. Запустите Flutter приложение
2. Нажмите "Войти"
3. Откроется браузер с формой входа Keycloak
4. Введите `test@example.com` / `test123`
5. Вы будете перенаправлены обратно в приложение

## 10. Production настройки

Для production окружения:

1. Измените пароль admin
2. Настройте HTTPS
3. Настройте email сервер для восстановления пароля
4. Настройте 2FA (опционально)
5. Настройте password policies
6. Настройте session timeouts

### Пример production конфигурации:

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:23.0
  environment:
    - KC_DB=postgres
    - KC_HOSTNAME=vpn.yourdomain.com
    - KC_HTTPS_CERTIFICATE_FILE=/opt/keycloak/conf/cert.pem
    - KC_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/conf/key.pem
  command: start --optimized
```

## Troubleshooting

### Keycloak не запускается

Проверьте логи:
```bash
docker logs keycloak
```

### Ошибка подключения к PostgreSQL

Убедитесь что PostgreSQL запущен:
```bash
docker ps | grep postgres
```

### Токен не валидируется на backend

Проверьте что issuer-uri правильный в application.yml:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://keycloak:8180/realms/vpn-realm
```

## Полезные ссылки

- Keycloak Documentation: https://www.keycloak.org/documentation
- OAuth2/OIDC: https://oauth.net/2/
- Spring Security OAuth2: https://spring.io/guides/tutorials/spring-boot-oauth2/
