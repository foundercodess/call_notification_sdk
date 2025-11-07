import 'dart:async';

import 'package:firebase_messaging/firebase_messaging.dart';

import '../call_notification_sdk_platform_interface.dart';
import 'models/call_event.dart';
import 'models/call_notification_config.dart';
import 'models/call_notification_payload.dart';
import 'models/call_status.dart';

typedef CallNotificationCallback = void Function(
  CallNotificationPayload payload,
);

class CallNotificationSDK {
  CallNotificationSDK._();

  static final CallNotificationSDK instance = CallNotificationSDK._();

  CallNotificationSdkPlatform get _platform =>
      CallNotificationSdkPlatform.instance;

  final StreamController<CallEvent> _eventController =
      StreamController.broadcast();

  StreamSubscription? _nativeSubscription;

  CallNotificationCallback? _onAccept;
  CallNotificationCallback? _onDecline;
  CallNotificationCallback? _onTimeout;
  CallNotificationCallback? _onCallerCancelled;

  /// Initialize the SDK with platform-specific configuration.
  Future<void> initialize({required CallNotificationConfig config}) async {
    await _nativeSubscription?.cancel();
    _nativeSubscription =
        _platform.events().listen(_handleRawEvent, onError: (error) {
      _eventController.addError(error);
    });

    await _platform.initialize(config.toJson());
  }

  /// Register callbacks for call lifecycle updates.
  void registerCallbacks({
    required CallNotificationCallback onAccept,
    required CallNotificationCallback onDecline,
    CallNotificationCallback? onTimeout,
    CallNotificationCallback? onCallerCancelled,
  }) {
    _onAccept = onAccept;
    _onDecline = onDecline;
    _onTimeout = onTimeout;
    _onCallerCancelled = onCallerCancelled;
  }

  /// Forward Firebase [RemoteMessage]s to the native layer.
  Future<void> handleRemoteMessage(RemoteMessage message) async {
    await _platform.handleRemoteMessage(message.data);
  }

  /// Update call status manually from the host application.
  Future<void> updateStatus({
    required String roomId,
    required CallStatus status,
  }) async {
    await _platform.updateStatus(
      roomId: roomId,
      status: status.name,
    );
  }

  /// Stream of call events emitted by the native layer.
  Stream<CallEvent> get events => _eventController.stream;

  Future<void> dispose() async {
    await _nativeSubscription?.cancel();
    _nativeSubscription = null;
    await _platform.dispose();
  }

  void _handleRawEvent(dynamic event) {
    if (event is! Map) {
      return;
    }

    final callEvent = CallEvent.fromJson(
      Map<String, dynamic>.from(event),
    );

    _eventController.add(callEvent);

    final payload = callEvent.payload;

    switch (callEvent.status) {
      case CallStatus.ringing:
      case CallStatus.answering:
        break;
      case CallStatus.answered:
        _onAccept?.call(payload);
        break;
      case CallStatus.declined:
        _onDecline?.call(payload);
        break;
      case CallStatus.cancelled:
        _onCallerCancelled?.call(payload);
        break;
      case CallStatus.timeout:
      case CallStatus.missed:
        _onTimeout?.call(payload);
        break;
    }
  }
}

