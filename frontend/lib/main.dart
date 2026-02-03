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
        ChangeNotifierProxyProvider<AuthProvider, VpnProvider>(
          create: (context) => VpnProvider(
            authProvider: context.read<AuthProvider>(),
          ),
          update: (context, authProvider, vpnProvider) =>
              vpnProvider ?? VpnProvider(authProvider: authProvider),
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
