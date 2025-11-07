import 'dart:convert';

class CallNotificationPayload {
  const CallNotificationPayload({
    required this.callId,
    required this.roomId,
    required this.callerId,
    required this.callerName,
    required this.receiverId,
    required this.type,
    this.avatarUrl,
    this.mediaType,
    this.metadata = const {},
  });

  final String callId;
  final String roomId;
  final String callerId;
  final String callerName;
  final String receiverId;
  final String type;
  final String? avatarUrl;
  final String? mediaType;
  final Map<String, dynamic> metadata;

  factory CallNotificationPayload.fromJson(Map<String, dynamic> json) {
    return CallNotificationPayload(
      callId: json['callId'] as String? ?? json['roomId'] as String,
      roomId: json['roomId'] as String,
      callerId: json['callerId'] as String,
      callerName: json['callerName'] as String,
      receiverId: json['receiverId'] as String,
      type: json['type'] as String,
      avatarUrl: json['avatarUrl'] as String?,
      mediaType: json['mediaType'] as String?,
      metadata: Map<String, dynamic>.from(
        json['metadata'] is Map
            ? json['metadata'] as Map
            : json['metadata'] is String
                ? _tryDecode(json['metadata'] as String)
                : const {},
      ),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'callId': callId,
      'roomId': roomId,
      'callerId': callerId,
      'callerName': callerName,
      'receiverId': receiverId,
      'type': type,
      'avatarUrl': avatarUrl,
      'mediaType': mediaType,
      'metadata': metadata,
    }..removeWhere((_, value) => value == null);
  }

  static Map<String, dynamic> _tryDecode(String value) {
    try {
      return Map<String, dynamic>.from(
        value.isEmpty ? const {} : (jsonDecode(value) as Map<String, dynamic>),
      );
    } catch (_) {
      return const {};
    }
  }
}

