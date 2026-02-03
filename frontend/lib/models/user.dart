/// Модель пользователя
class User {
  final int id;
  final String email;
  final String name;
  final String role;

  User({
    required this.id,
    required this.email,
    required this.name,
    required this.role,
  });

  /// Создать User из JSON
  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] ?? json['userId'],
      email: json['email'],
      name: json['name'],
      role: json['role'],
    );
  }

  /// Конвертировать User в JSON
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'name': name,
      'role': role,
    };
  }

  /// Проверка является ли пользователь гостем
  bool get isGuest => role == 'GUEST';

  /// Проверка является ли пользователь администратором
  bool get isAdmin => role == 'ADMIN';
}
