import 'dart:io';

import 'package:dbus/dbus.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final linuxPortalAccentColorProvider = FutureProvider<Color?>((ref) async {
  if (!Platform.isLinux) return null;
  return readLinuxPortalAccentColor();
});

Future<Color?> readLinuxPortalAccentColor() async {
  final client = DBusClient.session(introspectable: false);
  try {
    final response = await _readPortalSetting(client, 'ReadOne');
    return response == null ? null : decodePortalAccentColor(response);
  } on DBusUnknownMethodException {
    try {
      final response = await _readPortalSetting(client, 'Read');
      return response == null ? null : decodePortalAccentColor(response);
    } on DBusMethodResponseException {
      return null;
    } on SocketException {
      return null;
    }
  } on DBusMethodResponseException {
    return null;
  } on SocketException {
    return null;
  } finally {
    await client.close();
  }
}

Future<DBusValue?> _readPortalSetting(DBusClient client, String method) async {
  final response = await client.callMethod(
    destination: 'org.freedesktop.portal.Desktop',
    path: DBusObjectPath('/org/freedesktop/portal/desktop'),
    interface: 'org.freedesktop.portal.Settings',
    name: method,
    values: const [
      DBusString('org.freedesktop.appearance'),
      DBusString('accent-color'),
    ],
    replySignature: DBusSignature('v'),
  );
  return response.returnValues.firstOrNull;
}

@visibleForTesting
Color? decodePortalAccentColor(DBusValue value) {
  var unwrapped = value;
  while (unwrapped is DBusVariant) {
    unwrapped = unwrapped.value;
  }
  if (unwrapped.signature.value != '(ddd)') return null;
  final channels = unwrapped.asStruct();
  if (channels.length != 3) return null;
  final components = channels
      .map((channel) {
        if (channel.signature.value != 'd') return null;
        return channel.asDouble();
      })
      .toList(growable: false);
  if (components.any(
    (component) =>
        component == null ||
        !component.isFinite ||
        component < 0 ||
        component > 1,
  )) {
    return null;
  }
  return Color.fromARGB(
    255,
    (components[0]! * 255).round(),
    (components[1]! * 255).round(),
    (components[2]! * 255).round(),
  );
}
