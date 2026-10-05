import 'dart:async';
import 'dart:io';

import 'package:dbus/dbus.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final linuxPortalAccentColorProvider = StreamProvider<Color?>(
  (ref) => watchLinuxPortalAccentColor(),
);

Stream<Color?> watchLinuxPortalAccentColor() {
  if (!Platform.isLinux) return Stream.value(null);
  return Stream.multi((controller) {
    final client = DBusClient.session(introspectable: false);
    final signals = DBusSignalStream(
      client,
      sender: 'org.freedesktop.portal.Desktop',
      path: DBusObjectPath('/org/freedesktop/portal/desktop'),
      interface: 'org.freedesktop.portal.Settings',
      name: 'SettingChanged',
    );
    final accents = watchPortalAccentColorChanges(
      signals,
      () => _readPortalAccentColor(client),
    );
    final subscription = accents.listen(
      controller.add,
      onError: controller.addError,
      onDone: controller.close,
    );
    controller.onCancel = () async {
      await subscription.cancel();
      await client.close();
    };
  }, isBroadcast: false);
}

Future<Color?> readLinuxPortalAccentColor() async {
  final client = DBusClient.session(introspectable: false);
  try {
    return await _readPortalAccentColor(client);
  } finally {
    await client.close();
  }
}

Future<Color?> _readPortalAccentColor(DBusClient client) async {
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
  }
}

@visibleForTesting
Stream<Color?> watchPortalAccentColorChanges(
  Stream<DBusSignal> signals,
  Future<Color?> Function() readColor,
) => Stream.multi((controller) {
  var canceled = false;
  var pending = Future<void>.value();

  void queueRead() {
    pending = pending.then((_) async {
      try {
        final color = await readColor();
        if (!canceled && !controller.isClosed) controller.add(color);
      } catch (error, stackTrace) {
        if (!canceled && !controller.isClosed) {
          controller.addError(error, stackTrace);
        }
      }
    });
  }

  final subscription = signals.listen(
    (signal) {
      if (_isAccentColorSettingChanged(signal)) queueRead();
    },
    onError: controller.addError,
    onDone: controller.close,
  );
  controller.onCancel = () async {
    canceled = true;
    await subscription.cancel();
  };
  queueRead();
}, isBroadcast: false);

bool _isAccentColorSettingChanged(DBusSignal signal) =>
    signal.interface == 'org.freedesktop.portal.Settings' &&
    signal.name == 'SettingChanged' &&
    signal.values.length == 3 &&
    signal.values[0] is DBusString &&
    signal.values[0].asString() == 'org.freedesktop.appearance' &&
    signal.values[1] is DBusString &&
    signal.values[1].asString() == 'accent-color';

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
