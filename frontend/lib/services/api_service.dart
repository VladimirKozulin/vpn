import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiService {
  static const String baseUrl = 'http://localhost:8080/api';

  Future<String> createClient() async {
    final response = await http.post(
      Uri.parse('$baseUrl/clients'),
      headers: {'Content-Type': 'application/json'},
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

  Future<String> getClientConfig(String clientId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/clients/$clientId/config'),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return data['link'];
    } else {
      throw Exception('Не удалось получить конфигурацию');
    }
  }

  Future<void> toggleClient(String clientId) async {
    await http.post(
      Uri.parse('$baseUrl/clients/$clientId/toggle'),
    );
  }
}
