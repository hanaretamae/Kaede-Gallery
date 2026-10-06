import 'dart:async';
import 'dart:io';

import 'package:dynamic_color/dynamic_color.dart' as dynamic_color;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

const _accentPollInterval = Duration(seconds: 2);

/// Windows has no accent-change notification exposed to Dart, so poll it.
final windowsAccentColorProvider = StreamProvider<Color?>(
  (ref) => watchWindowsAccentColor(),
);

Stream<Color?> watchWindowsAccentColor({
  Future<Color?> Function()? read,
  Duration interval = _accentPollInterval,
  bool? isWindows,
}) {
  if (!(isWindows ?? Platform.isWindows)) return Stream.value(null);
  final reader = read ?? dynamic_color.DynamicColorPlugin.getAccentColor;
  return (() async* {
    Color? last;
    var first = true;
    while (true) {
      Color? current;
      try {
        current = await reader();
      } catch (_) {
        current = last;
      }
      if (first || current != last) {
        first = false;
        last = current;
        yield current;
      }
      await Future<void>.delayed(interval);
    }
  })();
}
