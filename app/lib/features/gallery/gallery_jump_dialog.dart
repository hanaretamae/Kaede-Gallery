part of 'gallery_screen.dart';

Future<void> _showGalleryStartIndexDialog(
  BuildContext context,
  WidgetRef ref,
  int maximum,
) async {
  final value = await showDialog<int>(
    context: context,
    builder: (context) => _GalleryStartIndexDialog(
      initialValue: ref.read(galleryLastJumpIndexProvider).clamp(1, maximum),
      maximum: maximum,
      onSubmit: (index) =>
          ref.read(galleryLastJumpIndexProvider.notifier).set(index),
    ),
  );
  if (value != null) {
    ref.read(galleryJumpStatusProvider.notifier).set(GalleryJumpStatus.loading);
    ref.read(galleryJumpTargetProvider.notifier).set(value - 1);
    final mode = ref.read(galleryDisplayModeProvider);
    if (mode == GalleryDisplayMode.byNote) {
      unawaited(ref.read(galleryItemsProvider.notifier).jumpTo(value - 1));
    } else {
      unawaited(ref.read(galleryMediaItemsProvider.notifier).jumpTo(value - 1));
    }
    if (context.mounted) {
      await showDialog<void>(
        context: context,
        barrierDismissible: false,
        builder: (context) =>
            _GalleryJumpProgressDialog(targetIndex: value - 1),
      );
      ref.read(galleryJumpTargetProvider.notifier).set(null);
      ref.read(galleryJumpStatusProvider.notifier).set(GalleryJumpStatus.idle);
    }
  }
}

class _GalleryJumpProgressDialog extends ConsumerWidget {
  const _GalleryJumpProgressDialog({required this.targetIndex});

  final int targetIndex;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final status = ref.watch(galleryJumpStatusProvider);
    ref.listen(galleryJumpStatusProvider, (previous, next) {
      if (next == GalleryJumpStatus.completed && context.mounted) {
        Navigator.of(context).pop();
      }
    });
    final message = switch (status) {
      GalleryJumpStatus.loading => '${targetIndex + 1} 件目のページを読み込んでいます。',
      GalleryJumpStatus.positioning => '${targetIndex + 1} 件目へ移動しています。',
      GalleryJumpStatus.completed => '${targetIndex + 1} 件目へ移動しました。',
      GalleryJumpStatus.notFound => '指定位置の項目が見つかりませんでした。',
      GalleryJumpStatus.failed => '指定位置の読み込みに失敗しました。',
      GalleryJumpStatus.idle => '移動を準備しています。',
    };
    final loading =
        status == GalleryJumpStatus.loading ||
        status == GalleryJumpStatus.positioning ||
        status == GalleryJumpStatus.idle;
    return AlertDialog(
      title: const Text('指定位置へ移動'),
      content: Row(
        children: [
          if (loading) ...[
            const SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(strokeWidth: 3),
            ),
            const SizedBox(width: 16),
          ],
          Expanded(child: Text(message)),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () {
            ref.read(galleryJumpTargetProvider.notifier).set(null);
            ref
                .read(galleryJumpStatusProvider.notifier)
                .set(GalleryJumpStatus.idle);
            Navigator.of(context).pop();
          },
          child: Text(loading ? 'キャンセル' : '閉じる'),
        ),
      ],
    );
  }
}

class _GalleryStartIndexDialog extends StatefulWidget {
  const _GalleryStartIndexDialog({
    required this.initialValue,
    required this.maximum,
    required this.onSubmit,
  });

  final int initialValue;
  final int maximum;
  final ValueChanged<int> onSubmit;

  @override
  State<_GalleryStartIndexDialog> createState() =>
      _GalleryStartIndexDialogState();
}

class _GalleryStartIndexDialogState extends State<_GalleryStartIndexDialog> {
  late final controller = TextEditingController(text: '${widget.initialValue}');
  String? errorMessage;

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text('指定した位置へ移動'),
    content: TextField(
      controller: controller,
      autofocus: true,
      keyboardType: TextInputType.number,
      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
      onChanged: (_) {
        if (errorMessage != null) setState(() => errorMessage = null);
      },
      decoration: InputDecoration(
        labelText: '何件目から表示',
        errorText: errorMessage,
        helperText: errorMessage == null ? '1 から ${widget.maximum} 件目まで' : null,
      ),
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.of(context).pop(),
        child: const Text('キャンセル'),
      ),
      FilledButton(
        onPressed: () {
          final parsed = int.tryParse(controller.text);
          if (parsed == null || parsed < 1 || parsed > widget.maximum) {
            setState(
              () => errorMessage = '1 から ${widget.maximum} の範囲で入力してください。',
            );
            return;
          }
          FocusScope.of(context).unfocus();
          widget.onSubmit(parsed);
          Navigator.of(context).pop(parsed);
        },
        child: const Text('移動'),
      ),
    ],
  );
}
