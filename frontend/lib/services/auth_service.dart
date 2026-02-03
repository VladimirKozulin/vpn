import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/user.dart';

/// Сервис для работы с аутентификацией
class AuthService {
  static const String baseUrl = 'http://192.168.0.9:8080/api/auth';

  /// Регистрация нового пользователя
  /// Возвращает JWT токен и информацию о пользователе
  Future<Map<String, dynamic>> register(
      String email, String password, String name) async {
    final response = await http.post(
      Uri.parse('$baseUrl/register'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'email': email,
        'password': password,
        'name': name,
      }),
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      final error = jsonDecode(response.body);
      throw Exception(error['error'] ?? 'Ошибка регистрации');
    }
  }

  /// Вход в систему
  /// Возвращает JWT токен и информацию о пользователе
  Future<Map<String, dynamic>> login(String email, String password) async {
    final response = await http.post(
      Uri.parse('$baseUrl/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({
        'email': email,
        'password': password,
      }),
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      final error = jsonDecode(response.body);
      throw Exception(error['error'] ?? 'Неверный email или пароль');
    }
  }

  /// Получить информацию о текущем пользователе
  Future<User> getCurrentUser(String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/me'),
      headers: {
        'Authorization': 'Bearer $token',
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
