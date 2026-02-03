import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/api_service.dart';

/// Provider для управления аутентификацией пользователя
/// Обновлено для работы с access и refresh токенами
class AuthProvider extends ChangeNotifier {
  final AuthService _authService = AuthService();
  final ApiService _apiService = ApiService();
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  User? _currentUser;
  String? _accessToken;
  String? _refreshToken;
  bool _isLoading = false;

  /// Текущий пользователь (null если не авторизован)
  User? get currentUser => _currentUser;

  /// Access токен (короткоживущий, 15 минут)
  String? get accessToken => _accessToken;

  /// Refresh токен (долгоживущий, 30 дней)
  String? get refreshToken => _refreshToken;

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

      if (_accessToken != null && _refreshToken != null) {
        try {
          // Проверяем access токен и загружаем информацию о пользователе
          _currentUser = await _authService.getCurrentUser(_accessToken!);
          debugPrint('Пользователь автоматически вошел: ${_currentUser!.email}');
        } catch (e) {
          // Access токен истек - пробуем обновить
          debugPrint('Access токен истек, обновляем...');
          await _refreshAccessToken();
        }
      }
    } catch (e) {
      debugPrint('Ошибка загрузки токенов: $e');
      // Токены невалидны - очищаем
      await _clearAuth();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Регистрация нового пользователя
  Future<void> register(String email, String password, String name) async {
    _isLoading = true;
    notifyListeners();

    try {
      // Регистрируем пользователя
      final response = await _authService.register(email, password, name);

      // Сохраняем токены и данные пользователя
      _accessToken = response['accessToken'];
      _refreshToken = response['refreshToken'];
      _currentUser = User.fromJson(response);

      // Сохраняем токены в secure storage
      await _secureStorage.write(key: 'access_token', value: _accessToken);
      await _secureStorage.write(key: 'refresh_token', value: _refreshToken);

      debugPrint('Регистрация успешна: ${_currentUser!.email}');
    } catch (e) {
      debugPrint('Ошибка регистрации: $e');
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Вход в систему
  Future<void> login(String email, String password) async {
    _isLoading = true;
    notifyListeners();

    try {
      // Входим в систему
      final response = await _authService.login(email, password);

      // Сохраняем токены и данные пользователя
      _accessToken = response['accessToken'];
      _refreshToken = response['refreshToken'];
      _currentUser = User.fromJson(response);

      // Сохраняем токены в secure storage
      await _secureStorage.write(key: 'access_token', value: _accessToken);
      await _secureStorage.write(key: 'refresh_token', value: _refreshToken);

      debugPrint('Вход успешен: ${_currentUser!.email}');
    } catch (e) {
      debugPrint('Ошибка входа: $e');
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Выход из системы
  Future<void> logout() async {
    try {
      // Отзываем refresh токен на сервере
      if (_accessToken != null && _refreshToken != null) {
        await _authService.logout(_accessToken!, _refreshToken!);
      }
    } catch (e) {
      debugPrint('Ошибка при logout на сервере: $e');
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
      final response = await _authService.refreshToken(_refreshToken!);

      // Обновляем access токен
      _accessToken = response['accessToken'];
      _currentUser = User.fromJson(response);

      // Сохраняем новый access токен
      await _secureStorage.write(key: 'access_token', value: _accessToken);

      debugPrint('Access токен обновлен');
    } catch (e) {
      debugPrint('Ошибка обновления токена: $e');
      // Refresh токен невалиден - очищаем все
      await _clearAuth();
      rethrow;
    }
  }

  /// Привязать гостевого клиента к аккаунту
  Future<void> claimGuestClient(String clientUuid) async {
    if (!isAuthenticated) {
      throw Exception('Необходима авторизация');
    }

    try {
      await _apiService.claimClient(clientUuid, _accessToken!);
      debugPrint('Гостевой клиент привязан к аккаунту');
    } catch (e) {
      debugPrint('Ошибка привязки клиента: $e');
      rethrow;
    }
  }

  /// Очистить данные аутентификации
  Future<void> _clearAuth() async {
    _accessToken = null;
    _refreshToken = null;
    _currentUser = null;
    await _secureStorage.delete(key: 'access_token');
    await _secureStorage.delete(key: 'refresh_token');
  }
}
