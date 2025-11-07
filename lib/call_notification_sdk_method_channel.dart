import 'package:flutter/services.dart';

import 'call_notification_sdk_platform_interface.dart';

class MethodChannelCallNotificationSdk extends CallNotificationSdkPlatform {
  static const MethodChannel _methodChannel =
      MethodChannel('call_notification_sdk/methods');
  static const EventChannel _eventChannel =
      EventChannel('call_notification_sdk/events');

  @override
  Future<void> initialize(Map<String, dynamic> config) async {
    await _methodChannel.invokeMethod<void>('initialize', config);
  }

  @override
  Stream<dynamic> events() {
    return _eventChannel.receiveBroadcastStream();
  }

  @override
  Future<void> handleRemoteMessage(Map<String, dynamic> message) async {
    await _methodChannel.invokeMethod<void>('handleRemoteMessage', message);
  }

  @override
  Future<void> updateStatus({required String roomId, required String status}) async {
    await _methodChannel.invokeMethod<void>('updateStatus', {
      'roomId': roomId,
      'status': status,
    });
  }
}
