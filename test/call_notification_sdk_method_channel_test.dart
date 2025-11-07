import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:call_notification_sdk/call_notification_sdk_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelCallNotificationSdk platform = MethodChannelCallNotificationSdk();
  const MethodChannel channel = MethodChannel('call_notification_sdk/methods');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return null;
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('initialize forwards to native layer', () async {
    bool called = false;

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        if (methodCall.method == 'initialize') {
          called = true;
        }
        return null;
      },
    );

    await platform.initialize({'androidChannelId': 'test'});
    expect(called, isTrue);
  });
}
