import 'dart:async';

import 'package:call_notification_sdk/call_notification_sdk.dart';
import 'package:call_notification_sdk/call_notification_sdk_method_channel.dart';
import 'package:call_notification_sdk/call_notification_sdk_platform_interface.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockCallNotificationSdkPlatform
    with MockPlatformInterfaceMixin
    implements CallNotificationSdkPlatform {
  bool initializeCalled = false;
  Map<String, dynamic>? lastConfig;
  Map<String, dynamic>? lastMessage;
  String? lastStatusRoomId;
  String? lastStatus;

  final StreamController<dynamic> eventController =
      StreamController.broadcast();

  @override
  Future<void> initialize(Map<String, dynamic> config) async {
    initializeCalled = true;
    lastConfig = config;
  }

  @override
  Stream<dynamic> events() {
    return eventController.stream;
  }

  @override
  Future<void> handleRemoteMessage(Map<String, dynamic> message) async {
    lastMessage = message;
  }

  @override
  Future<void> updateStatus({
    required String roomId,
    required String status,
  }) async {
    lastStatusRoomId = roomId;
    lastStatus = status;
  }

  @override
  Future<void> dispose() async {
    await eventController.close();
  }
}

void main() {
  final CallNotificationSdkPlatform initialPlatform =
      CallNotificationSdkPlatform.instance;

  tearDown(() async {
    await CallNotificationSDK.instance.dispose();
    CallNotificationSdkPlatform.instance = initialPlatform;
  });

  test('$MethodChannelCallNotificationSdk is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelCallNotificationSdk>());
  });

  test('initialize delegates to platform', () async {
    final mockPlatform = MockCallNotificationSdkPlatform();
    CallNotificationSdkPlatform.instance = mockPlatform;

    await CallNotificationSDK.instance.initialize(
      config: const CallNotificationConfig(
        androidChannelId: 'channel',
        androidChannelName: 'Calls',
        androidFullScreenIntentActivity: 'com.example.Activity',
      ),
    );

    expect(mockPlatform.initializeCalled, isTrue);
    expect(mockPlatform.lastConfig?['androidChannelId'], 'channel');
  });

  test('handleRemoteMessage forwards payload', () async {
    final mockPlatform = MockCallNotificationSdkPlatform();
    CallNotificationSdkPlatform.instance = mockPlatform;

    await CallNotificationSDK.instance.handleRemoteMessage(
      RemoteMessageFake({'roomId': '123'}),
    );

    expect(mockPlatform.lastMessage, equals({'roomId': '123'}));
  });

  test('updateStatus forwards status name', () async {
    final mockPlatform = MockCallNotificationSdkPlatform();
    CallNotificationSdkPlatform.instance = mockPlatform;

    await CallNotificationSDK.instance.updateStatus(
      roomId: 'abc',
      status: CallStatus.answered,
    );

    expect(mockPlatform.lastStatusRoomId, 'abc');
    expect(mockPlatform.lastStatus, CallStatus.answered.name);
  });

  test('registerCallbacks reacts to events', () async {
    final mockPlatform = MockCallNotificationSdkPlatform();
    CallNotificationSdkPlatform.instance = mockPlatform;

    await CallNotificationSDK.instance.initialize(
      config: const CallNotificationConfig(
        androidChannelId: 'channel',
        androidChannelName: 'Calls',
        androidFullScreenIntentActivity: 'com.example.Activity',
      ),
    );

    final accepted = <CallNotificationPayload>[];
    CallNotificationSDK.instance.registerCallbacks(
      onAccept: accepted.add,
      onDecline: (_) {},
    );

    mockPlatform.eventController.add(
      CallEvent(
        status: CallStatus.answered,
        payload: CallNotificationPayload(
          callId: 'c1',
          roomId: 'c1',
          callerId: 'caller',
          callerName: 'Caller',
          receiverId: 'receiver',
          type: 'call_audio',
        ),
      ).toJson(),
    );

    await Future<void>.delayed(const Duration(milliseconds: 10));

    expect(accepted, hasLength(1));
  });
}

class RemoteMessageFake extends Fake implements RemoteMessage {
  RemoteMessageFake(this._data);

  final Map<String, dynamic> _data;

  @override
  Map<String, dynamic> get data => _data;
}
