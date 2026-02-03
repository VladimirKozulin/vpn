import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_vless/flutter_vless.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/api_service.dart';
import 'auth_provider.dart';

/// Provider для управления VPN подключением
class VpnProvider extends ChangeNotifier {
  late final FlutterVless _flutterVless;
  final ApiService _apiService = ApiService();
  final AuthProvider authProvider;

  bool _isConnected = false;
  bool _isConnecting = false;
  int _connectionDuration = 0;
  Timer? _timer;
  String? _clientId;
  String? _clientUuid;
  String? _vlessLink;

  bool get isConnected => _isConnected;
  bool get isConnecting => _isConnecting;
  int get connectionDuration => _connectionDuration;
  
  String get formattedDuration {
    final hours = _connectionDuration ~/ 3600;
    final minutes = (_connectionDuration % 3600) ~/ 60;
    final seconds = _connectionDuration % 60;
    return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
  }

  VpnProvider({required this.authProvider}) {
    _flutterVless = FlutterVless(onStatusChanged: (status) {
      debugPrint('VPN Status: $status');
    });
    _initialize();
  }

  Future<void> _initialize() async {
    await _flutterVless.initializeVless(
      providerBundleIdentifier: 'com.example.frontend.VPNProvider',
      groupIdentifier: 'group.com.example.frontend',
    );

    // Загружаем сохраненный clientId и UUID
    final prefs = await SharedPreferences.getInstance();
    _clientId = prefs.getString('client_id');
    _clientUuid = prefs.getString('client_uuid');
    
    debugPrint('Загружен clientId: $_clientId, UUID: $_clientUuid');
    notifyListeners();
  }

  /// Подключиться к VPN
  Future<void> connect() async {
    if (_isConnecting || _isConnected) return;

    _isConnecting = true;
    notifyListeners();

    try {
      debugPrint('=== Начало подключения VPN ===');
      debugPrint('Авторизован: ${authProvider.isAuthenticated}');
      debugPrint('Текущий clientId: $_clientId');
      
      // Если нет clientId - создаем нового клиента
      if (_clientId == null) {
        debugPrint('Создание нового клиента...');
        // Передаем access токен если пользователь авторизован
        final token = authProvider.isAuthenticated ? authProvider.accessToken : null;
        debugPrint('Токен: ${token != null ? "есть" : "нет"}');
        
        _clientId = await _apiService.createClient(token: token);
        
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('client_id', _clientId!);
        
        debugPrint('Создан новый клиент ID: $_clientId');
      }

      // Получаем VLESS ссылку
      debugPrint('Получение конфигурации для клиента $_clientId...');
      final token = authProvider.isAuthenticated ? authProvider.accessToken : null;
      
      try {
        _vlessLink = await _apiService.getClientConfig(_clientId!, token: token);
      } catch (e) {
        // Если клиент не найден (404) или доступ запрещён (403) - очищаем старый ID
        if (e.toString().contains('404') || e.toString().contains('403')) {
          debugPrint('Клиент $_clientId недоступен (${e.toString().contains('404') ? '404' : '403'}), создаём нового...');
          _clientId = null;
          _clientUuid = null;
          final prefs = await SharedPreferences.getInstance();
          await prefs.remove('client_id');
          await prefs.remove('client_uuid');
          
          // Создаём нового клиента
          _clientId = await _apiService.createClient(token: token);
          await prefs.setString('client_id', _clientId!);
          debugPrint('Создан новый клиент ID: $_clientId для ${authProvider.isAuthenticated ? "авторизованного пользователя" : "гостя"}');
          
          // Получаем конфигурацию для нового клиента
          _vlessLink = await _apiService.getClientConfig(_clientId!, token: token);
        } else {
          rethrow;
        }
      }
      
      debugPrint('Получена VLESS ссылка: ${_vlessLink?.substring(0, 30)}...');

      // Парсим конфигурацию
      debugPrint('Парсинг конфигурации...');
      final parser = FlutterVless.parseFromURL(_vlessLink!);
      final config = parser.getFullConfiguration();
      debugPrint('Конфигурация получена');
      
      // Извлекаем UUID из VLESS ссылки (формат: vless://UUID@host:port...)
      // UUID находится между vless:// и @
      if (_vlessLink!.startsWith('vless://')) {
        final uuidEnd = _vlessLink!.indexOf('@');
        if (uuidEnd > 8) {
          _clientUuid = _vlessLink!.substring(8, uuidEnd);
          final prefs = await SharedPreferences.getInstance();
          await prefs.setString('client_uuid', _clientUuid!);
          debugPrint('Извлечен UUID клиента: $_clientUuid');
        }
      }

      // Запрашиваем разрешение VPN
      debugPrint('Запрос разрешения VPN...');
      final allowed = await _flutterVless.requestPermission();
      if (!allowed) {
        debugPrint('Разрешение VPN отклонено пользователем');
        _isConnecting = false;
        notifyListeners();
        return;
      }
      debugPrint('Разрешение VPN получено');

      // Запускаем VPN
      debugPrint('Запуск VPN...');
      await _flutterVless.startVless(
        remark: 'VPN Connection',
        config: config,
      );

      _isConnected = true;
      _startTimer();
      
      debugPrint('=== VPN подключен успешно ===');
    } catch (e, stackTrace) {
      debugPrint('=== ОШИБКА подключения VPN ===');
      debugPrint('Ошибка: $e');
      debugPrint('StackTrace: $stackTrace');
      rethrow;
    } finally {
      _isConnecting = false;
      notifyListeners();
    }
  }

  /// Отключиться от VPN
  Future<void> disconnect() async {
    if (!_isConnected) return;

    try {
      await _flutterVless.stopVless();
      _isConnected = false;
      _stopTimer();
      _connectionDuration = 0;
      
      debugPrint('VPN отключен');
      notifyListeners();
    } catch (e) {
      debugPrint('Ошибка отключения: $e');
    }
  }

  /// Привязать текущего гостевого клиента к аккаунту
  Future<void> claimCurrentClient() async {
    if (_clientUuid == null) {
      throw Exception('Нет активного клиента для привязки');
    }

    await authProvider.claimGuestClient(_clientUuid!);
    debugPrint('Текущий клиент привязан к аккаунту');
  }

  void _startTimer() {
    _timer?.cancel();
    _connectionDuration = 0;
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      _connectionDuration++;
      notifyListeners();
    });
  }

  void _stopTimer() {
    _timer?.cancel();
    _timer = null;
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}
