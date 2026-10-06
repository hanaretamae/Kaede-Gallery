import 'package:flutter_test/flutter_test.dart';
import 'package:vault_gallery/platform/vault_platform.dart';

void main() {
  test('normalizeLocalPath strips Windows verbatim prefixes', () {
    expect(normalizeLocalPath(r'\\?\C:\Users\a\b.mp4'), r'C:\Users\a\b.mp4');
    expect(
      normalizeLocalPath(r'\\?\UNC\srv\share\b.mp4'),
      r'\\srv\share\b.mp4',
    );
    expect(normalizeLocalPath('/home/a/b.mp4'), '/home/a/b.mp4');
  });

  test('obsidianOpenUri omits the verbatim prefix', () {
    final uri = obsidianOpenUri(r'\\?\C:\Users\a\Vault', 'Notes/x.md');
    expect(uri.queryParameters['path'], isNot(contains('?')));
    expect(uri.queryParameters['path'], contains('C:'));
  });
}
