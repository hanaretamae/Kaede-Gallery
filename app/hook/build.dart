import 'dart:io';

import 'package:flutter_rust_bridge_hooks/flutter_rust_bridge_hooks.dart';
import 'package:path/path.dart' as path;

Future<String> rustCompilerPath(String workingDirectory) async {
  final configuredPath = Platform.environment['RUSTC']?.trim();
  if (configuredPath != null && configuredPath.isNotEmpty) {
    return configuredPath;
  }

  final result = await Process.run(Platform.isWindows ? 'where' : 'which', [
    'rustc',
  ], workingDirectory: workingDirectory);
  final compilerPath = result.stdout
      .toString()
      .split(RegExp(r'[\r\n]+'))
      .first
      .trim();
  if (result.exitCode != 0 || compilerPath.isEmpty) {
    throw ProcessException(
      Platform.isWindows ? 'where' : 'which',
      ['rustc'],
      result.stderr.toString(),
      result.exitCode,
    );
  }
  return compilerPath;
}

void main(List<String> args) async {
  await build(args, (input, output) async {
    final crateDirectory = path.normalize(
      path.join(
        path.fromUri(input.packageRoot),
        '..',
        'crates',
        'gallery-bridge',
      ),
    );

    await FlutterRustBridgeNativeAssetsBuilder(
      cratePath: '../crates/gallery-bridge',
      extraCargoEnvironmentVariables: {
        'RUSTC': await rustCompilerPath(crateDirectory),
      },
    ).run(input: input, output: output);
  });
}
