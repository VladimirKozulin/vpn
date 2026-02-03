import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'providers/auth_provider.dart';
import 'providers/vpn_provider.dart';
import 'screens/home_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        // AuthProvider должен быть первым
        ChangeNotifierProvider(create: (_) => AuthProvider()..initialize()),
        // VpnProvider зависит от AuthProvider
        // ProxyProvider автоматически пересоздаст VpnProvider когда AuthProvider изменится
        ChangeNotifierProxyProvider<AuthProvider, VpnProvider>(
          create: (context) => VpnProvider(
            authProvider: Provider.of<AuthProvider>(context, listen: false),
          ),
          update: (context, authProvider, previousVpnProvider) {
            // Возвращаем существующий экземпляр, он уже имеет ссылку на authProvider
            return previousVpnProvider!;
          },
        ),
      ],
      child: MaterialApp(
        title: 'VPN Client',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
          useMaterial3: true,
        ),
        home: const HomeScreen(),
      ),
    );
  }
}
