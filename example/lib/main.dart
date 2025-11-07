import 'dart:async';

import 'package:call_notification_sdk/call_notification_sdk.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _status = 'Idle';
  late final StreamSubscription<CallEvent> _subscription;

  @override
  void initState() {
    super.initState();

    CallNotificationSDK.instance.registerCallbacks(
      onAccept: (payload) => setState(() {
        _status = 'Accepted call ${payload.callId}';
      }),
      onDecline: (payload) => setState(() {
        _status = 'Declined call ${payload.callId}';
      }),
      onCallerCancelled: (payload) => setState(() {
        _status = 'Caller cancelled ${payload.callId}';
      }),
      onTimeout: (payload) => setState(() {
        _status = 'Missed call ${payload.callId}';
      }),
    );

    _subscription = CallNotificationSDK.instance.events.listen((event) {
      debugPrint('[Example] Received event: ${event.status}');
    });
  }

  @override
  void dispose() {
    _subscription.cancel();
    CallNotificationSDK.instance.dispose();
    super.dispose();
  }

  Future<void> _initializeSdk() async {
    await CallNotificationSDK.instance.initialize(
      config: const CallNotificationConfig(
        androidChannelId: 'incoming_calls',
        androidChannelName: 'Incoming Calls',
        androidFullScreenIntentActivity:
            'com.example.call_notification_sdk_example/com.example.call_notification_sdk_example.MainActivity',
      ),
    );

    if (!mounted) return;
    setState(() {
      _status = 'SDK initialized';
    });
  }

  Future<void> _simulateIncomingCall() async {
    final message = RemoteMessage.fromMap({
      'messageId': 'local-simulated',
      'sentTime': DateTime.now().millisecondsSinceEpoch,
      'data': <String, dynamic>{
        'type': 'call_audio',
        'roomId': 'demo-room-42',
        'callId': 'demo-call-42',
        'callerId': 'user_123',
        'callerName': 'Alice',
        'receiverId': 'user_456',
        'mediaType': 'audio',
        'metadata': '{"note":"local simulation"}',
      },
    });

    await CallNotificationSDK.instance.handleRemoteMessage(message);

    if (!mounted) return;
    setState(() {
      _status = 'Simulated incoming call dispatched';
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Call Notification SDK Example'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Status: $_status'),
              const SizedBox(height: 24),
              const Text('1. Configure Firebase Messaging in this app.'),
              const Text(
                '2. Forward RemoteMessage data payloads to '
                'CallNotificationSDK.instance.handleRemoteMessage.',
              ),
              const Text(
                '3. Listen to the callbacks or events stream to launch your call UI.',
              ),
              const Spacer(),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _initializeSdk,
                  child: const Text('Initialize Call Notification SDK'),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _simulateIncomingCall,
                  child: const Text('Simulate Incoming Call (local)'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
