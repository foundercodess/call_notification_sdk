import 'dart:async';

import 'package:call_notification_sdk/call_notification_sdk.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'firebase_options.dart';

const CallNotificationConfig _callNotificationConfig = CallNotificationConfig(
  androidChannelId: 'incoming_calls',
  androidChannelName: 'Incoming Calls',
  androidFullScreenIntentActivity:
      'com.example.call_notification_sdk_example.MainActivity',
);

Future<void> _initializeCallNotificationSdk() async {
  await CallNotificationSDK.instance.initialize(
    config: _callNotificationConfig,
  );
}

@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  if (Firebase.apps.isEmpty) {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    );
  }

  try {
    await _initializeCallNotificationSdk();
    await CallNotificationSDK.instance.handleRemoteMessage(message);
  } catch (error, stackTrace) {
    debugPrint('[Example] Background handler error: $error');
    debugPrint('$stackTrace');
  }
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);

  if (Firebase.apps.isEmpty) {
    await Firebase.initializeApp(
      options: DefaultFirebaseOptions.currentPlatform,
    );
  }

  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _status = 'Idle';
  String? _token;
  bool _sdkInitialized = false;
  bool _isInitializingSdk = false;
  late final StreamSubscription<CallEvent> _subscription;
  StreamSubscription<RemoteMessage>? _onMessageSubscription;
  StreamSubscription<RemoteMessage>? _onMessageOpenedAppSubscription;
  StreamSubscription<String>? _tokenRefreshSubscription;

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

    unawaited(_initializeSdk(updateStatus: false));
    unawaited(_configureFirebaseMessaging());
  }

  @override
  void dispose() {
    _onMessageSubscription?.cancel();
    _onMessageOpenedAppSubscription?.cancel();
    _tokenRefreshSubscription?.cancel();
    _subscription.cancel();
    CallNotificationSDK.instance.dispose();
    super.dispose();
  }

  Future<void> _configureFirebaseMessaging() async {
    final messaging = FirebaseMessaging.instance;

    final settings = await messaging.requestPermission();
    debugPrint('[Example] Notification permission status: '
        '${settings.authorizationStatus}');

    final token = await messaging.getToken();
    print(token);
    if (mounted) {
      setState(() {
        _token = token;
      });
    }

    _tokenRefreshSubscription =
        FirebaseMessaging.instance.onTokenRefresh.listen((newToken) {
      debugPrint('[Example] FCM token refreshed');
      if (!mounted) return;
      setState(() {
        _token = newToken;
      });
    });

    final initialMessage = await messaging.getInitialMessage();
    if (initialMessage != null) {
      await _handleIncomingMessage(
        initialMessage,
        origin: 'Terminated state',
      );
    }

    _onMessageSubscription = FirebaseMessaging.onMessage.listen((message) {
      unawaited(_handleIncomingMessage(
        message,
        origin: 'Foreground',
      ));
    });

    _onMessageOpenedAppSubscription =
        FirebaseMessaging.onMessageOpenedApp.listen((message) {
      unawaited(_handleIncomingMessage(
        message,
        origin: 'Notification tap',
      ));
    });
  }

  Future<void> _fetchLatestToken() async {
    try {
      final token = await FirebaseMessaging.instance.getToken();
      if (!mounted) return;
      print('token: $token');
      setState(() {
        _token = token;
        _status = 'FCM token refreshed';
      });
    } catch (error, stackTrace) {
      debugPrint('[Example] Error fetching token: $error');
      debugPrint('$stackTrace');
      if (!mounted) return;
      setState(() {
        _status = 'Failed to refresh FCM token';
      });
    }
  }

  Future<void> _copyTokenToClipboard() async {
    final token = _token;
    if (token == null || token.isEmpty) {
      setState(() {
        _status = 'No FCM token to copy';
      });
      return;
    }

    await Clipboard.setData(ClipboardData(text: token));
    if (!mounted) return;
    setState(() {
      _status = 'FCM token copied to clipboard';
    });
  }

  Future<void> _initializeSdk({bool updateStatus = true}) async {
    if (_isInitializingSdk) return;
    _isInitializingSdk = true;

    try {
      await _initializeCallNotificationSdk();
      if (!mounted) return;
      setState(() {
        _sdkInitialized = true;
        if (updateStatus) {
          _status = 'SDK initialized';
        }
      });
    } catch (error, stackTrace) {
      debugPrint('[Example] Failed to initialize SDK: $error');
      debugPrint('$stackTrace');
      if (!mounted) return;
      setState(() {
        _status = 'SDK initialization failed';
      });
    } finally {
      _isInitializingSdk = false;
    }
  }

  Future<void> _handleIncomingMessage(
    RemoteMessage message, {
    required String origin,
  }) async {
    try {
      if (!_sdkInitialized) {
        await _initializeSdk(updateStatus: false);
      }
      await CallNotificationSDK.instance.handleRemoteMessage(message);
      if (!mounted) return;
      setState(() {
        _status = '$origin notification handled';
      });
    } catch (error, stackTrace) {
      debugPrint('[Example] Error forwarding message: $error');
      debugPrint('$stackTrace');
      if (!mounted) return;
      setState(() {
        _status = 'Failed to handle $origin notification';
      });
    }
  }

  Future<void> _simulateIncomingCall() async {
    if (!_sdkInitialized) {
      await _initializeSdk(updateStatus: false);
    }

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
              const SizedBox(height: 12),
              Text(
                _token == null
                    ? 'FCM token: requesting...'
                    : 'FCM token: $_token',
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _fetchLatestToken,
                  child: const Text('Fetch Latest FCM Token'),
                ),
              ),
              const SizedBox(height: 12),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _copyTokenToClipboard,
                  child: const Text('Copy FCM Token'),
                ),
              ),
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
