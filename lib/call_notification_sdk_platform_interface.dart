import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'call_notification_sdk_method_channel.dart';

abstract class CallNotificationSdkPlatform extends PlatformInterface {
  /// Constructs a CallNotificationSdkPlatform.
  CallNotificationSdkPlatform() : super(token: _token);

  static final Object _token = Object();

  static CallNotificationSdkPlatform _instance = MethodChannelCallNotificationSdk();

  /// The default instance of [CallNotificationSdkPlatform] to use.
  ///
  /// Defaults to [MethodChannelCallNotificationSdk].
  static CallNotificationSdkPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [CallNotificationSdkPlatform] when
  /// they register themselves.
  static set instance(CallNotificationSdkPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// Initialize native side with the provided configuration.
  Future<void> initialize(Map<String, dynamic> config) {
    throw UnimplementedError('initialize() has not been implemented.');
  }

  /// Stream of raw events emitted by the native layer.
  Stream<dynamic> events() {
    throw UnimplementedError('events() has not been implemented.');
  }

  /// Pass an FCM payload to the native layer.
  Future<void> handleRemoteMessage(Map<String, dynamic> message) {
    throw UnimplementedError('handleRemoteMessage() has not been implemented.');
  }

  /// Update call status from Dart side.
  Future<void> updateStatus({
    required String roomId,
    required String status,
  }) {
    throw UnimplementedError('updateStatus() has not been implemented.');
  }

  /// Optionally dispose native resources.
  Future<void> dispose() async {}
}
