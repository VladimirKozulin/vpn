import 'dart:convert';
import 'package:http/http.dart' as http;

/// Сервис для работы с VPN клиентами через REST API
class ApiService {
  static const String baseUrl = 'http://192.168.0.9:8080/api';

  /// Создать нового VPN клиента
  /// Если token передан - создает клиента привязанного к пользователю
  /// Если token = null - создает гостевого клиента
  Future<String> createClient({String? token}) async {
    final headers = {'Content-Type': 'application/json'};
    
    // Добавляем токен если есть
    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }

    final response = await http.post(
      Uri.parse('$baseUrl/clients'),
      headers: headers,
      body: jsonEncode({
        'deviceInfo': 'Flutter Mobile App',
        'ipAddress': '0.0.0.0',
        'country': 'Unknown',
        'isActive': true,
        'trafficLimitGb': 100,
        'trafficUsedGb': 0.0,
      }),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return data['id'].toString();
    } else {
      throw Exception('Не удалось создать клиента');
    }
  }

  /// Получить VLESS конфигурацию для клиента
  Future<String> getClientConfig(String clientId, {String? token}) async {
    final headers = <String, String>{};
    
    // Добавляем токен если есть
    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }

    final response = await http.get(
      Uri.parse('$baseUrl/clients/$clientId/config'),
      headers: headers.isNotEmpty ? headers : null,
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return data['link'];
    } else {
      throw Exception('Не удалось получить конфигурацию');
    }
  }

  /// Получить все клиенты текущего пользователя
  Future<List<dynamic>> getMyClients(String token) async {
    final response = await http.get(
      Uri.parse('$baseUrl/clients/my'),
      headers: {
        'Authorization': 'Bearer $token',
      },
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Не удалось получить список клиентов');
    }
  }

  /// Привязать гостевого клиента к аккаунту
  Future<void> claimClient(String clientUuid, String token) async {
    final response = await http.post(
      Uri.parse('$baseUrl/clients/claim'),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
      body: jsonEncode({
        'clientUuid': clientUuid,
      }),
    );

    if (response.statusCode != 200) {
      final error = jsonDecode(response.body);
      throw Exception(error['error'] ?? 'Не удалось привязать клиента');
    }
  }

  /// Переключить активность клиента
  Future<void> toggleClient(String clientId, String token) async {
    await http.post(
      Uri.parse('$baseUrl/clients/$clientId/toggle'),
      headers: {
        'Authorization': 'Bearer $token',
      },
    );
  }
}
