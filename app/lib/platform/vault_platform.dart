import 'package:file_selector/file_selector.dart';
import 'package:path/path.dart' as path;
import 'package:path_provider/path_provider.dart';

class GalleryPaths {
  const GalleryPaths({
    required this.dataDirectory,
    required this.indexPath,
    required this.thumbnailDirectory,
  });

  final String dataDirectory;
  final String indexPath;
  final String thumbnailDirectory;
}

abstract interface class VaultPlatform {
  Future<String?> chooseVault();
  Future<GalleryPaths> galleryPaths();
}

class NativeVaultPlatform implements VaultPlatform {
  const NativeVaultPlatform();

  @override
  Future<String?> chooseVault() =>
      getDirectoryPath(confirmButtonText: 'この Vault を選択');

  @override
  Future<GalleryPaths> galleryPaths() async {
    final support = await getApplicationSupportDirectory();
    final directory = path.join(support.path, 'vault-gallery');
    return GalleryPaths(
      dataDirectory: directory,
      indexPath: path.join(directory, 'index.sqlite'),
      thumbnailDirectory: path.join(directory, 'thumbnails'),
    );
  }
}
