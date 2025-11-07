class CallNotificationConfig {
  const CallNotificationConfig({
    required this.androidChannelId,
    required this.androidChannelName,
    required this.androidFullScreenIntentActivity,
    this.androidNotificationIcon,
    this.androidAcceptAction,
    this.androidDeclineAction,
    this.androidRingtone,
    this.ringTimeout,
    this.enableFirestoreStatusListener = false,
  });

  final String androidChannelId;
  final String androidChannelName;
  final String androidFullScreenIntentActivity;
  final String? androidNotificationIcon;
  final String? androidAcceptAction;
  final String? androidDeclineAction;
  final String? androidRingtone;
  final Duration? ringTimeout;
  final bool enableFirestoreStatusListener;

  Map<String, dynamic> toJson() {
    return {
      'androidChannelId': androidChannelId,
      'androidChannelName': androidChannelName,
      'androidFullScreenIntentActivity':
          androidFullScreenIntentActivity,
      'androidNotificationIcon': androidNotificationIcon,
      'androidAcceptAction': androidAcceptAction,
      'androidDeclineAction': androidDeclineAction,
      'androidRingtone': androidRingtone,
      'ringTimeoutMs': ringTimeout?.inMilliseconds,
      'enableFirestoreStatusListener': enableFirestoreStatusListener,
    }..removeWhere((_, value) => value == null);
  }
}

