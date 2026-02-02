import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/vpn_provider.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[100],
      body: SafeArea(
        child: Center(
          child: Consumer<VpnProvider>(
            builder: (context, vpnProvider, child) {
              return Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Spacer(flex: 2),
                  
                  // Таймер
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 32,
                      vertical: 16,
                    ),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(16),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.05),
                          blurRadius: 10,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Text(
                      vpnProvider.formattedDuration,
                      style: const TextStyle(
                        fontSize: 48,
                        fontWeight: FontWeight.bold,
                        fontFamily: 'monospace',
                        color: Colors.black87,
                      ),
                    ),
                  ),
                  
                  const SizedBox(height: 16),
                  
                  // Статус
                  Text(
                    vpnProvider.isConnected
                        ? 'Подключено'
                        : vpnProvider.isConnecting
                            ? 'Подключение...'
                            : 'Отключено',
                    style: TextStyle(
                      fontSize: 18,
                      color: vpnProvider.isConnected
                          ? Colors.green[700]
                          : Colors.grey[600],
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  
                  const Spacer(flex: 1),
                  
                  // Главная кнопка
                  GestureDetector(
                    onTap: vpnProvider.isConnecting
                        ? null
                        : () {
                            if (vpnProvider.isConnected) {
                              vpnProvider.disconnect();
                            } else {
                              vpnProvider.connect();
                            }
                          },
                    child: Container(
                      width: 200,
                      height: 200,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        gradient: LinearGradient(
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                          colors: vpnProvider.isConnected
                              ? [Colors.red[400]!, Colors.red[600]!]
                              : vpnProvider.isConnecting
                                  ? [Colors.orange[400]!, Colors.orange[600]!]
                                  : [Colors.blue[400]!, Colors.blue[600]!],
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: (vpnProvider.isConnected
                                    ? Colors.red[400]
                                    : Colors.blue[400])!
                                .withOpacity(0.4),
                            blurRadius: 20,
                            offset: const Offset(0, 8),
                          ),
                        ],
                      ),
                      child: Center(
                        child: vpnProvider.isConnecting
                            ? const CircularProgressIndicator(
                                color: Colors.white,
                                strokeWidth: 3,
                              )
                            : Icon(
                                vpnProvider.isConnected
                                    ? Icons.power_settings_new
                                    : Icons.power_settings_new_outlined,
                                size: 80,
                                color: Colors.white,
                              ),
                      ),
                    ),
                  ),
                  
                  const SizedBox(height: 24),
                  
                  // Текст кнопки
                  Text(
                    vpnProvider.isConnected
                        ? 'Отключиться'
                        : vpnProvider.isConnecting
                            ? 'Подключение...'
                            : 'Подключиться',
                    style: const TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.w600,
                      color: Colors.black87,
                    ),
                  ),
                  
                  const Spacer(flex: 2),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}
