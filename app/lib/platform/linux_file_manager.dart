import 'package:dbus/dbus.dart';

Future<void> revealFileInLinuxFileManager(String filePath) async {
  final client = DBusClient.session(introspectable: false);
  try {
    await client.callMethod(
      destination: 'org.freedesktop.FileManager1',
      path: DBusObjectPath('/org/freedesktop/FileManager1'),
      interface: 'org.freedesktop.FileManager1',
      name: 'ShowItems',
      values: [
        DBusArray.string([Uri.file(filePath).toString()]),
        const DBusString(''),
      ],
      replySignature: DBusSignature(''),
    );
  } finally {
    await client.close();
  }
}
