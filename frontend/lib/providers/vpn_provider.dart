import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_v2ray_plus/flutter_v2ray_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/api_service.dart';

class VpnProvider extends ChangeNotifier {
  final FlutterV2ray _flutterV2ray = FlutterV2ray();
  final ApiService _apiService = ApiService();
  
  bool _isConnected = false;
  bool _isConnecting = false;
  int _connectionDuration = 0;
  Timer? _timer;
  String? _clientId;
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

  VpnProvider() {
    _initialize();
  }

  Future<void> _initialize() async {
    await _flutterV2ray.initializeVless(
      providerBundleIdentifier: 'com.example.frontend.VPNProvider',
      groupIdentifier: 'group.com.example.frontend',
    );
    
    final prefs = await SharedPreferences.getInstance();
    _clientId = prefs.getString('client_id');
    notifyListeners();
  }

  Future<void> connect() async {
    if (_isConnecting || _isConnected) return;

    _isConnecting = true;
    notifyListeners();

    try {
      if (_clientId == null) {
        _clientId = await _apiService.createClient();
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('client_id', _clientId!);
      }

      _vlessLink = await _apiService.getClientConfig(_clientId!);
      
      final parser = FlutterV2ray.parseFromURL(_vlessLink!);
      final config = parser.getFullConfiguration();

      final allowed = await _flutterV2ray.requestPermission();
      if (!allowed) {
        _isConnecting = false;
        notifyListeners();
        return;
      }

      await _flutterV2ray.startVless(
        remark: 'VPN Connection',
        config: config,
      );

      _isConnected = true;
      _startTimer();
    } catch (e) {
      debugPrint('Ошибка подключения: $e');
    } finally {
      _isConnecting = false;
      notifyListeners();
    }
  }

  Future<void> disconnect() async {
    if (!_isConnected) return;

    try {
      await _flutterV2ray.stopVless();
      _isConnected = false;
      _stopTimer();
      _connectionDuration = 0;
      notifyListeners();
    } catch (e) {
      debugPrint('Ошибка отключения: $e');
    }
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
