import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:vault_gallery/core_api/desktop_system_appearance.dart';

void main() {
  test('windows accent changes are emitted without restart', () async {
    final colors = [Colors.red, Colors.red, Colors.blue];
    var i = 0;
    final out = await watchWindowsAccentColor(
      read: () async => colors[i < 2 ? i++ : 2],
      interval: Duration.zero,
      isWindows: true,
    ).take(2).toList();
    expect(out, [Colors.red, Colors.blue]);
  });
}
