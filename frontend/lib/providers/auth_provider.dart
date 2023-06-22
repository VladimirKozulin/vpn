import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/keycloak_auth_service.dart';

/// Provider для управления аутентификацией через Keycloak
class AuthProvider extends ChangeNotifier {
  final KeycloakAuthService _keycloakAuth = KeycloakAuthService();
  final AuthService _authService = AuthService();
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  User? _currentUser;
  String? _accessToken;
  String? _refreshToken;
  String? _idToken;
  bool _isLoading = false;

  /// Текущий пользователь (null если не авторизован)
  User? get currentUser => _currentUser;

  /// Access токен от Keycloak
  String? get accessToken => _accessToken;

  /// Refresh токен от Keycloak
  String? get refreshToken => _refreshToken;

  /// ID токен от Keycloak
  String? get idToken => _idToken;

  /// Проверка авторизован ли пользователь
  bool get isAuthenticated => _currentUser != null && _accessToken != null;

  /// Проверка является ли пользователь гостем
  bool get isGuest => !isAuthenticated;

  /// Индикатор загрузки
  bool get isLoading => _isLoading;

  /// Инициализация - загрузка токенов из secure storage
  Future<void> initialize() async {
    _isLoading = true;
    notifyListeners();

    try {
      // Пытаемся загрузить токены из хранилища
      _accessToken = await _secureStorage.read(key: 'access_token');
      _refreshToken = await _secureStorage.read(key: 'refresh_token');
      _idToken = await _secureStorage.read(key: 'id_token');

      if (_accessToken != null && _refreshToken != null) {
        try {
          // Загружаем информацию о пользователе
          await _loadUserInfo();
          debugPrint('Пользователь автоматически вошел: ${_currentUser!.email}');
        } catch (e) {
          // Access токен истек - пробуем обновить
          debugPrint('Access токен истек, обновляем...');
          await _refreshAccessToken();
        }
      }
    } catch (e) {
      debugPrint('Ошибка загрузки токенов: $e');
      await _clearAuth();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Вход через Keycloak
  Future<void> login() async {
    _isLoading = true;
    notifyListeners();

    try {
      final result = await _keycloakAuth.login();

      _accessToken = result.accessToken;
      _refreshToken = result.refreshToken;
      _idToken = result.idToken;

      // Сохраняем токены
      await _secureStorage.write(key: 'access_token', value: _accessToken);
      await _secureStorage.write(key: 'refresh_token', value: _refreshToken);
      await _secureStorage.write(key: 'id_token', value: _idToken);

      // Загружаем информацию о пользователе с нашего backend
      await _loadUserInfo();

      debugPrint('Вход успешен: ${_currentUser!.email}');
    } catch (e) {
      debugPrint('Ошибка входа: $e');
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Регистрация через Keycloak
  Future<void> register() async {
    _isLoading = true;
    notifyListeners();

    try {
      final result = await _keycloakAuth.register();

      _accessToken = result.accessToken;
      _refreshToken = result.refreshToken;
      _idToken = result.idToken;

      // Сохраняем токены
      await _secureStorage.write(key: 'access_token', value: _accessToken);
      await _secureStorage.write(key: 'refresh_token', value: _refreshToken);
      await _secureStorage.write(key: 'id_token', value: _idToken);

      // Загружаем информацию о пользователе
      await _loadUserInfo();

      debugPrint('Регистрация успешна: ${_currentUser!.email}');
    } catch (e) {
      debugPrint('Ошибка регистрации: $e');
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Выход из системы
  Future<void> logout() async {
    try {
      // Отзываем токены в Keycloak
      await _keycloakAuth.logout(_idToken);
    } catch (e) {
      debugPrint('Ошибка при logout в Keycloak: $e');
    } finally {
      await _clearAuth();
      debugPrint('Пользователь вышел из системы');
      notifyListeners();
    }
  }

  /// Обновить access токен используя refresh токен
  Future<void> _refreshAccessToken() async {
    if (_refreshToken == null) {
      throw Exception('Refresh токен отсутствует');
    }

    try {
      final result = await _keycloakAuth.refreshToken(_refreshToken!);

      _accessToken = result.accessToken;
      if (result.refreshToken != null) {
        _refreshToken = result.refreshToken;
      }
      if (result.idToken != null) {
        _idToken = result.idToken;
      }

      // Сохраняем новые токены
      await _secureStorage.write(key: 'access_token', value: _accessToken);
      if (_refreshToken != null) {
        await _secureStorage.write(key: 'refresh_token', value: _refreshToken);
      }
      if (_idToken != null) {
        await _secureStorage.write(key: 'id_token', value: _idToken);
      }

      // Загружаем информацию о пользователе
      await _loadUserInfo();

      debugPrint('Access токен обновлен');
    } catch (e) {
      debugPrint('Ошибка обновления токена: $e');
      await _clearAuth();
      rethrow;
    }
  }

  /// Загрузить информацию о пользователе с backend
  Future<void> _loadUserInfo() async {
    if (_accessToken == null) {
      throw Exception('Access токен отсутствует');
    }

    _currentUser = await _authService.getCurrentUser(_accessToken!);
  }

  /// Очистить данные аутентификации
  Future<void> _clearAuth() async {
    _accessToken = null;
    _refreshToken = null;
    _idToken = null;
    _currentUser = null;
    await _secureStorage.delete(key: 'access_token');
    await _secureStorage.delete(key: 'refresh_token');
    await _secureStorage.delete(key: 'id_token');
  }

  /// Привязать гостевого клиента к аккаунту (для обратной совместимости)
  /// В новой архитектуре с Keycloak гостевой режим не поддерживается
  Future<void> claimGuestClient(String clientUuid) async {
    if (!isAuthenticated) {
      throw Exception('Необходима авторизация');
    }

    try {
      final response = await http.post(
        Uri.parse('http://192.168.0.9:8080/api/clients/claim'),
        headers: {
          'Authorization': 'Bearer $_accessToken',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({'clientUuid': clientUuid}),
      );

      if (response.statusCode != 200) {
        throw Exception('Не удалось привязать клиента');
      }

      debugPrint('Клиент $clientUuid привязан к аккаунту');
    } catch (e) {
      debugPrint('Ошибка привязки клиента: $e');
      rethrow;
    }
  }
}
