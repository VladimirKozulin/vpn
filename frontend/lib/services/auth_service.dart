import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/user.dart';

/// Сервис для работы с API пользователей
/// Авторизация теперь через Keycloak
class AuthService {
  static const String baseUrl = 'http://192.168.0.9:8080/api/users';

  /// Получить информацию о текущем пользователе
  /// Требует access токен от Keycloak
  Future<User> getCurrentUser(String accessToken) async {
    final response = await http.get(
      Uri.parse('$baseUrl/me'),
      headers: {
        'Authorization': 'Bearer $accessToken',
      },
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return User.fromJson(data);
    } else {
      throw Exception('Неверный токен');
    }
  }
}
