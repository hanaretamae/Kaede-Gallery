part of 'gallery_screen.dart';

const int _maximumUnboundedGalleryJump = 2147483647;

Future<void> _showGalleryStartIndexDialog(
  BuildContext context,
  WidgetRef ref,
  int? maximum,
) async {
  final value = await M3EDialog.show<int>(
    context,
    dialog: _GalleryStartIndexDialog(
      initialValue: ref
          .read(galleryLastJumpIndexProvider)
          .clamp(1, maximum ?? _maximumUnboundedGalleryJump),
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
      await M3EDialog.show<void>(
        context,
        barrierDismissible: false,
        dialog: _GalleryJumpProgressDialog(targetIndex: value - 1),
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
      GalleryJumpStatus.loading => tr(
        '${targetIndex + 1} 件目のページを読み込んでいます。',
        'Loading the page containing item ${targetIndex + 1}.',
      ),
      GalleryJumpStatus.positioning => tr(
        '${targetIndex + 1} 件目へ移動しています。',
        'Jumping to item ${targetIndex + 1}.',
      ),
      GalleryJumpStatus.completed => tr(
        '${targetIndex + 1} 件目へ移動しました。',
        'Jumped to item ${targetIndex + 1}.',
      ),
      GalleryJumpStatus.notFound => tr(
        '指定位置の項目が見つかりませんでした。',
        'Could not find the item at that position.',
      ),
      GalleryJumpStatus.failed => tr(
        '指定位置の読み込みに失敗しました。',
        'Failed to load the requested position.',
      ),
      GalleryJumpStatus.idle => tr('移動を準備しています。', 'Preparing to jump.'),
    };
    final loading =
        status == GalleryJumpStatus.loading ||
        status == GalleryJumpStatus.positioning ||
        status == GalleryJumpStatus.idle;
    return M3EDialog(
      title: tr('指定位置へ移動', 'Jump to position'),
      content: Row(
        children: [
          if (loading) ...[
            const SizedBox(
              width: 24,
              height: 24,
              child: M3EProgressIndicator.circular(strokeWidth: 3),
            ),
            const SizedBox(width: 16),
          ],
          Expanded(child: Text(message)),
        ],
      ),
      actions: [
        M3EButton.text(
          onPressed: () {
            ref.read(galleryJumpTargetProvider.notifier).set(null);
            ref
                .read(galleryJumpStatusProvider.notifier)
                .set(GalleryJumpStatus.idle);
            Navigator.of(context).pop();
          },
          child: Text(loading ? tr('キャンセル', 'Cancel') : tr('閉じる', 'Close')),
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
  final int? maximum;
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
  Widget build(BuildContext context) => M3EDialog(
    title: tr('指定した位置へ移動', 'Jump to position'),
    content: M3ETextField(
      controller: controller,
      autofocus: true,
      label: tr('何件目から表示', 'Start at item'),
      errorText: errorMessage,
      variant: M3ETextFieldVariant.outlined,
      supportingText: errorMessage == null
          ? widget.maximum == null
                ? tr(
                    '件数を計算中です。正の整数を指定できます。',
                    'The total is still loading. Enter a positive integer.',
                  )
                : tr(
                    '1 から ${widget.maximum} 件目まで',
                    'Items 1 to ${widget.maximum}',
                  )
          : null,
      keyboardType: TextInputType.number,
      inputFormatters: [
        FilteringTextInputFormatter.digitsOnly,
        LengthLimitingTextInputFormatter(10),
      ],
      onChanged: (_) {
        if (errorMessage != null) setState(() => errorMessage = null);
      },
    ),
    actions: [
      M3EButton.text(
        onPressed: () => Navigator.of(context).pop(),
        child: Text(tr('キャンセル', 'Cancel')),
      ),
      M3EButton.filled(
        onPressed: () {
          final parsed = int.tryParse(controller.text);
          final maximum = widget.maximum;
          if (parsed == null ||
              parsed < 1 ||
              parsed > _maximumUnboundedGalleryJump ||
              (maximum != null && parsed > maximum)) {
            setState(
              () => errorMessage = widget.maximum == null
                  ? tr(
                      '1 から $_maximumUnboundedGalleryJump の範囲で入力してください。',
                      'Enter a value from 1 to $_maximumUnboundedGalleryJump.',
                    )
                  : tr(
                      '1 から ${widget.maximum} の範囲で入力してください。',
                      'Enter a value from 1 to ${widget.maximum}.',
                    ),
            );
            return;
          }
          FocusScope.of(context).unfocus();
          widget.onSubmit(parsed);
          Navigator.of(context).pop(parsed);
        },
        child: Text(tr('移動', 'Jump')),
      ),
    ],
  );
}
