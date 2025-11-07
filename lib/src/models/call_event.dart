import 'call_notification_payload.dart';
import 'call_status.dart';

class CallEvent {
  const CallEvent({
    required this.status,
    required this.payload,
  });

  final CallStatus status;
  final CallNotificationPayload payload;

  factory CallEvent.fromJson(Map<String, dynamic> json) {
    return CallEvent(
      status: CallStatus.fromName(json['status'] as String),
      payload: CallNotificationPayload.fromJson(
        Map<String, dynamic>.from(json['payload'] as Map),
      ),
    );
  }

  Map<String, dynamic> toJson() => {
        'status': status.name,
        'payload': payload.toJson(),
      };
}

