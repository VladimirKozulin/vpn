import 'dart:convert';
import 'package:flutter_appauth/flutter_appauth.dart';
import 'package:flutter/foundation.dart';

/// Сервис для работы с Keycloak через OAuth2/OIDC
class KeycloakAuthService {
  static const String _keycloakUrl = 'http://192.168.0.9:8180';
  static const String _realm = 'vpn-realm';
  static const String _clientId = 'vpn-mobile';
  static const String _redirectUrl = 'com.example.vpn://login-callback';
  
  final FlutterAppAuth _appAuth = const FlutterAppAuth();
  
  /// Вход через Keycloak (Authorization Code Flow with PKCE)
  Future<TokenResponse> login() async {
    try {
      final AuthorizationTokenRequest request = AuthorizationTokenRequest(
        _clientId,
        _redirectUrl,
        issuer: '$_keycloakUrl/realms/$_realm',
        scopes: ['openid', 'profile', 'email', 'offline_access'],
        promptValues: ['login'],
      );
      
      final result = await _appAuth.authorizeAndExchangeCode(request);
      
      if (result == null) {
        throw Exception('Вход отменен');
      }
      
      return result;
    } catch (e) {
      debugPrint('Ошибка входа через Keycloak: $e');
      rethrow;
    }
  }
  
  /// Регистрация через Keycloak
  Future<TokenResponse> register() async {
    try {
      final AuthorizationTokenRequest request = AuthorizationTokenRequest(
        _clientId,
        _redirectUrl,
        issuer: '$_keycloakUrl/realms/$_realm',
        scopes: ['openid', 'profile', 'email', 'offline_access'],
        promptValues: ['login'],
        additionalParameters: {
          'kc_action': 'REGISTER',
        },
      );
      
      final result = await _appAuth.authorizeAndExchangeCode(request);
      
      if (result == null) {
        throw Exception('Регистрация отменена');
      }
      
      return result;
    } catch (e) {
      debugPrint('Ошибка регистрации через Keycloak: $e');
      rethrow;
    }
  }
  
  /// Обновить access токен
  Future<TokenResponse> refreshToken(String refreshToken) async {
    try {
      final result = await _appAuth.token(TokenRequest(
        _clientId,
        _redirectUrl,
        issuer: '$_keycloakUrl/realms/$_realm',
        refreshToken: refreshToken,
      ));
      
      if (result == null) {
        throw Exception('Не удалось обновить токен');
      }
      
      return result;
    } catch (e) {
      debugPrint('Ошибка обновления токена: $e');
      rethrow;
    }
  }
  
  /// Выход (отзыв токенов)
  Future<void> logout(String? idToken) async {
    try {
      if (idToken != null) {
        await _appAuth.endSession(EndSessionRequest(
          idTokenHint: idToken,
          postLogoutRedirectUrl: _redirectUrl,
          issuer: '$_keycloakUrl/realms/$_realm',
        ));
      }
    } catch (e) {
      debugPrint('Ошибка выхода: $e');
    }
  }
  
  /// Получить информацию о пользователе из токена
  Map<String, dynamic> parseJwt(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) {
        throw Exception('Невалидный JWT');
      }
      
      final payload = parts[1];
      final normalized = base64Url.normalize(payload);
      final decoded = utf8.decode(base64Url.decode(normalized));
      
      return jsonDecode(decoded);
    } catch (e) {
      debugPrint('Ошибка парсинга JWT: $e');
      return {};
    }
  }
}
