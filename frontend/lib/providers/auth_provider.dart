import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/api_service.dart';

/// Provider для управления аутентификацией пользователя
class AuthProvider extends ChangeNotifier {
  final AuthService _authService = AuthService();
  final ApiService _apiService = ApiService();
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();

  User? _currentUser;
  String? _jwtToken;
  bool _isLoading = false;

  /// Текущий пользователь (null если не авторизован)
  User? get currentUser => _currentUser;

  /// JWT токен
  String? get jwtToken => _jwtToken;

  /// Проверка авторизован ли пользователь
  bool get isAuthenticated => _currentUser != null && _jwtToken != null;

  /// Проверка является ли пользователь гостем
  bool get isGuest => !isAuthenticated;

  /// Индикатор загрузки
  bool get isLoading => _isLoading;

  /// Инициализация - загрузка токена из secure storage
  Future<void> initialize() async {
    _isLoading = true;
    notifyListeners();

    try {
      // Пытаемся загрузить токен из хранилища
      _jwtToken = await _secureStorage.read(key: 'jwt_token');

      if (_jwtToken != null) {
        // Проверяем токен и загружаем информацию о пользователе
        _currentUser = await _authService.getCurrentUser(_jwtToken!);
        debugPrint('Пользователь автоматически вошел: ${_currentUser!.email}');
      }
    } catch (e) {
      debugPrint('Ошибка загрузки токена: $e');
      // Токен невалиден - очищаем
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

      // Сохраняем токен и данные пользователя
      _jwtToken = response['token'];
      _currentUser = User.fromJson(response);

      // Сохраняем токен в secure storage
      await _secureStorage.write(key: 'jwt_token', value: _jwtToken);

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

      // Сохраняем токен и данные пользователя
      _jwtToken = response['token'];
      _currentUser = User.fromJson(response);

      // Сохраняем токен в secure storage
      await _secureStorage.write(key: 'jwt_token', value: _jwtToken);

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
    await _clearAuth();
    debugPrint('Пользователь вышел из системы');
    notifyListeners();
  }

  /// Привязать гостевого клиента к аккаунту
  Future<void> claimGuestClient(String clientUuid) async {
    if (!isAuthenticated) {
      throw Exception('Необходима авторизация');
    }

    try {
      await _apiService.claimClient(clientUuid, _jwtToken!);
      debugPrint('Гостевой клиент привязан к аккаунту');
    } catch (e) {
      debugPrint('Ошибка привязки клиента: $e');
      rethrow;
    }
  }

  /// Очистить данные аутентификации
  Future<void> _clearAuth() async {
    _jwtToken = null;
    _currentUser = null;
    await _secureStorage.delete(key: 'jwt_token');
  }
}
