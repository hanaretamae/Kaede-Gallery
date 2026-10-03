part of 'gallery_screen.dart';

class _NoteViewerScreen extends ConsumerWidget {
  const _NoteViewerScreen({
    required this.noteId,
    required this.initialMediaId,
    this.initialPreview,
    this.initiallyShowDetails = false,
  });

  final int noteId;
  final int? initialMediaId;
  final Uint8List? initialPreview;
  final bool initiallyShowDetails;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detail = ref.watch(galleryNoteDetailProvider(noteId));
    return detail.when(
      loading: () => Scaffold(
        appBar: AppBar(),
        body: const ColoredBox(
          color: Colors.black,
          child: Center(child: CircularProgressIndicator()),
        ),
      ),
      error: (_, _) => Scaffold(
        appBar: AppBar(),
        body: const Center(child: Text('ノートの詳細を読み込めませんでした。')),
      ),
      data: (note) => note == null
          ? Scaffold(
              appBar: AppBar(),
              body: const Center(child: Text('ノートが見つかりません。再走査してください。')),
            )
          : _NoteViewerContent(
              note: note,
              initialMediaId: initialMediaId,
              initialPreview: initialPreview,
              initiallyShowDetails: initiallyShowDetails,
            ),
    );
  }
}

class _NoteViewerContent extends ConsumerStatefulWidget {
  const _NoteViewerContent({
    required this.note,
    required this.initialMediaId,
    this.initialPreview,
    this.initiallyShowDetails = false,
  });

  final GalleryNoteDetail note;
  final int? initialMediaId;
  final Uint8List? initialPreview;
  final bool initiallyShowDetails;

  @override
  ConsumerState<_NoteViewerContent> createState() => _NoteViewerContentState();
}

class _NoteViewerContentState extends ConsumerState<_NoteViewerContent>
    with SingleTickerProviderStateMixin {
  /// Minimum fraction of the viewer height reserved for media once the
  /// detail panel has expanded as far as scrolling allows. Keeps a "reasonable
  /// minimum" media area visible instead of letting it collapse entirely.
  static const double _minMediaHeightFraction = 0.18;

  /// Default (collapsed) fraction of the viewer height used by the detail
  /// panel; matches the pre-existing behavior.
  static const double _baseDetailsHeightFraction = 0.4;

  /// Scroll distance (in logical pixels) over which the detail panel expands
  /// from its base height up to its maximum height as the user scrolls
  /// through the lower detail content.
  static const double _detailsExpandDistance = 220;

  late final PageController pageController;
  late final ScrollController detailsScrollController;
  late final AnimationController _detailsVisibilityController;
  late final CurvedAnimation _detailsVisibilityAnimation;
  late final Listenable _detailsLayout = Listenable.merge([
    _detailsVisibilityController,
    _detailsScrollOffset,
  ]);
  final ValueNotifier<double> _detailsScrollOffset = ValueNotifier(0);
  late int currentPage;
  bool imageZoomed = false;
  bool controlsVisible = false;
  bool fullscreen = false;
  Timer? horizontalPageChangeCooldown;
  double trackpadPanDistance = 0;
  double horizontalDragDistance = 0;

  @override
  void initState() {
    super.initState();
    controlsVisible = widget.initiallyShowDetails;
    fullscreen = Platform.isAndroid && !controlsVisible;
    _detailsVisibilityController = AnimationController(
      vsync: this,
      duration: GalleryMotion.emphasized,
    )..value = controlsVisible ? 1 : 0;
    _detailsVisibilityAnimation = CurvedAnimation(
      parent: _detailsVisibilityController,
      curve: GalleryMotion.emphasizedCurve,
    );
    if (fullscreen) unawaited(_setFullscreen(true));
    final initialIndex = widget.note.media.indexWhere(
      (media) => media.id == widget.initialMediaId,
    );
    currentPage = initialIndex < 0 ? 0 : initialIndex;
    pageController = PageController(initialPage: currentPage);
    detailsScrollController = ScrollController()
      ..addListener(() {
        if (!detailsScrollController.hasClients) return;
        final offset = detailsScrollController.offset.clamp(
          0.0,
          _detailsExpandDistance,
        );
        if (offset != _detailsScrollOffset.value) {
          _detailsScrollOffset.value = offset;
        }
      });
  }

  @override
  void dispose() {
    pageController.dispose();
    detailsScrollController.dispose();
    _detailsVisibilityAnimation.dispose();
    _detailsVisibilityController.dispose();
    _detailsScrollOffset.dispose();
    horizontalPageChangeCooldown?.cancel();
    if (fullscreen) unawaited(_setFullscreen(false));
    super.dispose();
  }

  /// Forwards a vertical scroll made while the pointer is over the media area
  /// to the detail panel's scroll position, so users can scroll the note
  /// details without first moving the pointer down to the overlay. The top
  /// overlay is a separate fixed [Positioned] widget and is never affected.
  void _forwardScrollToDetails(double deltaY) {
    if (!controlsVisible || !detailsScrollController.hasClients) return;
    final position = detailsScrollController.position;
    final target = (position.pixels + deltaY).clamp(
      position.minScrollExtent,
      position.maxScrollExtent,
    );
    if (target != position.pixels) {
      detailsScrollController.jumpTo(target);
    }
  }

  void _toggleControls() {
    if (!controlsVisible) {
      unawaited(_showDetails());
      return;
    }
    setState(() {
      controlsVisible = false;
      imageZoomed = false;
      if (Platform.isAndroid) fullscreen = true;
    });
    _detailsVisibilityController.reverse();
    if (Platform.isAndroid) unawaited(_setFullscreen(true));
  }

  Future<void> _setFullscreen(bool value) async {
    if (Platform.isLinux || Platform.isWindows || Platform.isMacOS) {
      await windowManager.setFullScreen(value);
    } else {
      await SystemChrome.setEnabledSystemUIMode(
        value ? SystemUiMode.immersiveSticky : SystemUiMode.edgeToEdge,
      );
    }
  }

  Future<void> _toggleFullscreen() async {
    final next = !fullscreen;
    try {
      await _setFullscreen(next);
      if (mounted) {
        setState(() {
          fullscreen = next;
          controlsVisible = !next;
          imageZoomed = false;
        });
        if (next) {
          _detailsVisibilityController.reverse();
        } else {
          _detailsVisibilityController.forward();
        }
      }
    } on PlatformException {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('全画面表示を切り替えられませんでした。')));
      }
    }
  }

  Future<void> _showDetails() async {
    try {
      if (fullscreen) await _setFullscreen(false);
      if (mounted) {
        setState(() {
          fullscreen = false;
          controlsVisible = true;
          imageZoomed = false;
        });
        _detailsVisibilityController.forward();
      }
    } on PlatformException {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('詳細表示に切り替えられませんでした。')));
      }
    }
  }

  void _changePage(int delta) {
    if (imageZoomed ||
        !pageController.hasClients ||
        widget.note.media.isEmpty) {
      return;
    }
    final page = (currentPage + delta).clamp(0, widget.note.media.length - 1);
    if (page == currentPage) return;
    pageController.animateToPage(
      page,
      duration: GalleryMotion.medium,
      curve: GalleryMotion.emphasizedCurve,
    );
  }

  void _handleTrackpadPageDelta(double delta, {bool invertDirection = false}) {
    if (delta.abs() < 8 || imageZoomed || widget.note.media.length < 2) return;
    if (horizontalPageChangeCooldown?.isActive ?? false) return;
    final next = (delta > 0) != invertDirection;
    _changePage(next ? 1 : -1);
    horizontalPageChangeCooldown = Timer(
      GalleryMotion.medium,
      () => horizontalPageChangeCooldown = null,
    );
  }

  double _detailsHeight(double maxHeight) {
    final baseHeight = maxHeight * _baseDetailsHeightFraction;
    final maxHeightWithMedia = math.max(
      baseHeight,
      maxHeight - maxHeight * _minMediaHeightFraction,
    );
    final expansion = (_detailsScrollOffset.value / _detailsExpandDistance)
        .clamp(0.0, 1.0);
    return baseHeight + (maxHeightWithMedia - baseHeight) * expansion;
  }

  double get _detailsVisibility => _detailsVisibilityAnimation.value;

  @override
  Widget build(BuildContext context) {
    final note = widget.note;
    final session = ref.watch(vaultSessionProvider).value;
    final blockOrder =
        ref
            .watch(galleryTagSettingsProvider)
            .asData
            ?.value
            .noteStructure
            .blockOrder ??
        GalleryNoteStructureSettings.defaultBlockOrder;
    final viewerTitle = _viewerTitle(note);
    final profileUrl = _isWebUri(note.authorUrl)
        ? Uri.parse(note.authorUrl!)
        : _profileUrl(note.url);
    final author = note.author ?? _viewerAuthor(note, profileUrl);
    final authorPosition = blockOrder.indexOf(GalleryNoteBlock.author);
    final mediaPosition = blockOrder.indexOf(GalleryNoteBlock.media);
    final authorBeforeMedia =
        authorPosition >= 0 &&
        mediaPosition >= 0 &&
        authorPosition < mediaPosition;
    final isVideo =
        note.media.isNotEmpty &&
        currentPage < note.media.length &&
        note.media[currentPage].isVideo;
    final currentMedia = currentPage < note.media.length
        ? note.media[currentPage]
        : null;
    final mediaSource = currentMedia == null
        ? null
        : ref.watch(galleryMediaSourcePathProvider(currentMedia.id));
    return Scaffold(
      backgroundColor: Colors.black,
      body: Focus(
        autofocus: true,
        onKeyEvent: (node, event) {
          if (event is! KeyDownEvent) return KeyEventResult.ignored;
          switch (event.logicalKey) {
            case LogicalKeyboardKey.arrowRight:
              _changePage(1);
              return KeyEventResult.handled;
            case LogicalKeyboardKey.arrowLeft:
              _changePage(-1);
              return KeyEventResult.handled;
            case LogicalKeyboardKey.escape:
              if (fullscreen) {
                _showDetails();
                return KeyEventResult.handled;
              }
              if (controlsVisible) {
                _toggleControls();
                return KeyEventResult.handled;
              }
          }
          return KeyEventResult.ignored;
        },
        child: Listener(
          onPointerSignal: (event) {
            if (event is PointerScrollEvent) {
              final delta = event.scrollDelta.dx;
              if (delta.abs() > event.scrollDelta.dy.abs()) {
                _handleTrackpadPageDelta(delta);
              }
            }
          },
          onPointerPanZoomUpdate: (event) {
            final delta = event.panDelta.dx;
            if (delta.abs() > event.panDelta.dy.abs()) {
              trackpadPanDistance += delta;
              if (trackpadPanDistance.abs() >= 24) {
                _handleTrackpadPageDelta(
                  trackpadPanDistance,
                  invertDirection: true,
                );
                trackpadPanDistance = 0;
              }
            }
          },
          child: LayoutBuilder(
            builder: (context, constraints) {
              return GestureDetector(
                onHorizontalDragStart: (_) => horizontalDragDistance = 0,
                onHorizontalDragUpdate: (details) {
                  horizontalDragDistance += details.primaryDelta ?? 0;
                },
                onHorizontalDragEnd: (details) {
                  final drag = horizontalDragDistance;
                  horizontalDragDistance = 0;
                  if (imageZoomed || note.media.length < 2) return;
                  if (drag.abs() >= 24 ||
                      (details.primaryVelocity?.abs() ?? 0) >= 250) {
                    final direction = drag.abs() >= 24
                        ? drag
                        : details.primaryVelocity!;
                    _changePage(direction < 0 ? 1 : -1);
                  }
                },
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    AnimatedBuilder(
                      animation: _detailsLayout,
                      child: Listener(
                        // Scrolling (mouse wheel / trackpad) while the pointer
                        // is over the media forwards to the detail panel's
                        // scroll position, so users do not need to move the
                        // pointer down onto the overlay to read note details.
                        // This never touches the top overlay, which is a
                        // separate fixed Positioned widget outside this Stack
                        // entry.
                        onPointerSignal: (event) {
                          if (event is! PointerScrollEvent) return;
                          final dy = event.scrollDelta.dy;
                          final dx = event.scrollDelta.dx;
                          if (dy.abs() > dx.abs()) {
                            _forwardScrollToDetails(dy);
                          }
                        },
                        child: GestureDetector(
                          onTap: _toggleControls,
                          onVerticalDragUpdate: controlsVisible && !imageZoomed
                              ? (details) => _forwardScrollToDetails(
                                  -(details.primaryDelta ?? 0),
                                )
                              : null,
                          child: PageView.builder(
                            controller: pageController,
                            physics: const NeverScrollableScrollPhysics(),
                            itemCount: note.media.isEmpty
                                ? 1
                                : note.media.length,
                            onPageChanged: (page) => setState(() {
                              currentPage = page;
                              imageZoomed = false;
                            }),
                            itemBuilder: (context, index) => note.media.isEmpty
                                ? const _ViewerMessage('このノートに表示できるメディアはありません。')
                                : _ViewerMedia(
                                    media: note.media[index],
                                    active: index == currentPage,
                                    key: ValueKey(note.media[index].id),
                                    initialPreview:
                                        index == currentPage &&
                                            note.media[index].id ==
                                                widget.initialMediaId
                                        ? widget.initialPreview
                                        : null,
                                    onImageZoomChanged: (zoomed) =>
                                        setState(() => imageZoomed = zoomed),
                                    controlsVisible: controlsVisible,
                                  ),
                          ),
                        ),
                      ),
                      builder: (context, child) => Positioned(
                        left: 0,
                        right: 0,
                        top: 0,
                        bottom:
                            _detailsHeight(constraints.maxHeight) *
                            _detailsVisibility,
                        child: child!,
                      ),
                    ),
                    if (!fullscreen && note.media.length > 1)
                      AnimatedBuilder(
                        animation: _detailsLayout,
                        child: IgnorePointer(
                          child: DecoratedBox(
                            decoration: BoxDecoration(
                              color: Colors.black.withValues(alpha: 0.55),
                              borderRadius: BorderRadius.circular(
                                GalleryShape.medium,
                              ),
                            ),
                            child: Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 12,
                                vertical: 7,
                              ),
                              child: Text(
                                '${currentPage + 1} / ${note.media.length}',
                                style: const TextStyle(color: Colors.white),
                              ),
                            ),
                          ),
                        ),
                        builder: (context, child) => Positioned(
                          top: null,
                          left: null,
                          right: 20,
                          bottom:
                              _detailsHeight(constraints.maxHeight) *
                                  _detailsVisibility +
                              (isVideo ? 130 : 24),
                          child: child!,
                        ),
                      ),
                    _ViewerTopOverlay(
                      visible: controlsVisible,
                      visibilityAnimation: _detailsVisibilityAnimation,
                      title: viewerTitle,
                      author: authorBeforeMedia ? author : null,
                      profileUrl: profileUrl,
                      postUrl: note.url,
                      fullscreen: fullscreen,
                      onClose: () => Navigator.of(context).pop(),
                      onToggleFullscreen: _toggleFullscreen,
                      onOpenProfile: profileUrl == null
                          ? null
                          : () => _openExternalUri(context, profileUrl),
                      onOpenObsidian:
                          session == null ||
                              session.vaultPath.startsWith('content://')
                          ? null
                          : () => _openExternalUri(
                              context,
                              Uri(
                                scheme: 'obsidian',
                                host: 'open',
                                queryParameters: {
                                  'path': p.join(session.vaultPath, note.path),
                                },
                              ),
                            ),
                      onOpenPost: !_isWebUri(note.url)
                          ? null
                          : () =>
                                _openExternalUri(context, Uri.parse(note.url!)),
                      onCopyPostUrl: !_isWebUri(note.url)
                          ? null
                          : () async {
                              await Clipboard.setData(
                                ClipboardData(text: note.url!),
                              );
                              if (context.mounted) {
                                ScaffoldMessenger.of(context).showSnackBar(
                                  const SnackBar(
                                    content: Text('URL をコピーしました。'),
                                  ),
                                );
                              }
                            },
                      mediaPath: mediaSource?.asData?.value,
                      vaultPath: session?.vaultPath,
                    ),
                    AnimatedBuilder(
                      animation: _detailsLayout,
                      child: ClipRect(
                        child: IgnorePointer(
                          ignoring: !controlsVisible,
                          child: FadeTransition(
                            key: const ValueKey('viewer-details-opacity'),
                            opacity: _detailsVisibilityAnimation,
                            child: _ViewerBottomOverlay(
                              note: note,
                              author: author,
                              showAuthor: !authorBeforeMedia,
                              scrollController: detailsScrollController,
                            ),
                          ),
                        ),
                      ),
                      builder: (context, child) => Positioned(
                        left: 0,
                        right: 0,
                        bottom: 0,
                        height:
                            _detailsHeight(constraints.maxHeight) *
                            _detailsVisibility,
                        child: child!,
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}

class _ViewerTopOverlay extends StatelessWidget {
  const _ViewerTopOverlay({
    required this.visible,
    required this.visibilityAnimation,
    required this.title,
    required this.author,
    required this.profileUrl,
    required this.postUrl,
    required this.fullscreen,
    required this.onClose,
    required this.onToggleFullscreen,
    required this.onOpenProfile,
    required this.onOpenObsidian,
    required this.onOpenPost,
    required this.onCopyPostUrl,
    required this.mediaPath,
    required this.vaultPath,
  });

  final bool visible;
  final Animation<double> visibilityAnimation;
  final String title;
  final String? author;
  final Uri? profileUrl;
  final String? postUrl;
  final bool fullscreen;
  final VoidCallback onClose;
  final VoidCallback onToggleFullscreen;
  final VoidCallback? onOpenProfile;
  final VoidCallback? onOpenObsidian;
  final VoidCallback? onOpenPost;
  final VoidCallback? onCopyPostUrl;
  final String? mediaPath;
  final String? vaultPath;

  @override
  Widget build(BuildContext context) => Positioned(
    top: 0,
    left: 0,
    right: 0,
    child: IgnorePointer(
      ignoring: !visible,
      child: FadeTransition(
        key: const ValueKey('viewer-top-overlay-opacity'),
        opacity: visibilityAnimation,
        child: Material(
          color: Theme.of(context).colorScheme.surface,
          elevation: 2,
          child: SafeArea(
            bottom: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(4, 0, 4, 4),
              child: Row(
                children: [
                  IconButton(
                    tooltip: '閉じる',
                    onPressed: onClose,
                    icon: const Icon(Icons.arrow_back),
                  ),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (author != null)
                          Text(
                            author!,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: Theme.of(context).textTheme.labelMedium,
                          ),
                        Text(
                          title,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: fullscreen ? '全画面表示を終了' : '全画面表示',
                    onPressed: onToggleFullscreen,
                    icon: Icon(
                      fullscreen ? Icons.fullscreen_exit : Icons.fullscreen,
                    ),
                  ),
                  if (profileUrl != null)
                    IconButton(
                      tooltip: '投稿者のプロフィールを開く',
                      onPressed: onOpenProfile,
                      icon: const Icon(Icons.person_outline),
                    ),
                  if (_isWebUri(postUrl))
                    IconButton(
                      tooltip: '元のページを開く',
                      onPressed: onOpenPost,
                      icon: const Icon(Icons.link),
                    ),
                  if (_isWebUri(postUrl))
                    IconButton(
                      tooltip: 'ページ URL をコピー',
                      onPressed: onCopyPostUrl,
                      icon: const Icon(Icons.copy),
                    ),
                  if (mediaPath != null)
                    IconButton(
                      tooltip: 'メディアを開く',
                      onPressed: () => _showMediaOpenActions(
                        context,
                        mediaPath!,
                        vaultPath: vaultPath,
                      ),
                      icon: const Icon(Icons.perm_media_outlined),
                    ),
                  if (onOpenObsidian != null)
                    IconButton(
                      tooltip: 'Obsidian でノートを開く',
                      onPressed: onOpenObsidian,
                      icon: const Icon(Icons.open_in_new),
                    ),
                ],
              ),
            ),
          ),
        ),
      ),
    ),
  );
}

Future<void> _showMediaOpenActions(
  BuildContext context,
  String mediaPath, {
  String? vaultPath,
}) async {
  final openFolder = await showModalBottomSheet<bool>(
    context: context,
    showDragHandle: true,
    builder: (context) => SafeArea(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(
            leading: const Icon(Icons.open_in_new),
            title: const Text('画像・動画の既定アプリで開く'),
            onTap: () => Navigator.of(context).pop(false),
          ),
          ListTile(
            leading: const Icon(Icons.folder_open),
            title: const Text('ファイルマネージャーでフォルダーを開く'),
            subtitle: const Text('開くアプリを選択します'),
            onTap: () => Navigator.of(context).pop(true),
          ),
        ],
      ),
    ),
  );
  if (openFolder == null || !context.mounted) return;
  if (mediaPath.startsWith('content://')) {
    if (vaultPath == null || !vaultPath.startsWith('content://')) return;
    try {
      await const AndroidSafAccess().openMedia(
        vaultPath,
        mediaPath,
        openFolder: openFolder,
      );
    } on PlatformException catch (error) {
      if (!context.mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            error.code == 'SAF_UNSUPPORTED'
                ? '選択したアプリで開けませんでした。'
                : 'メディアまたはフォルダーを開けませんでした。',
          ),
        ),
      );
    }
    return;
  }
  final uri = openFolder
      ? Uri.directory(p.dirname(mediaPath))
      : Uri.file(mediaPath);
  await _openExternalUri(context, uri);
}

class _ViewerBottomOverlay extends StatelessWidget {
  const _ViewerBottomOverlay({
    required this.note,
    required this.author,
    required this.showAuthor,
    required this.scrollController,
  });

  final GalleryNoteDetail note;
  final String? author;
  final bool showAuthor;
  final ScrollController scrollController;

  @override
  Widget build(BuildContext context) => Material(
    color: Theme.of(context).colorScheme.surfaceContainerLow,
    elevation: 3,
    child: SingleChildScrollView(
      controller: scrollController,
      padding: const EdgeInsets.only(top: 16, bottom: 16),
      child: _NoteDetailsPanel(
        note: note,
        author: author,
        showAuthor: showAuthor,
      ),
    ),
  );
}

class _ViewerMedia extends ConsumerWidget {
  const _ViewerMedia({
    super.key,
    required this.media,
    required this.active,
    this.initialPreview,
    required this.onImageZoomChanged,
    required this.controlsVisible,
  });

  final GalleryMediaItem media;
  final bool active;
  final Uint8List? initialPreview;
  final ValueChanged<bool> onImageZoomChanged;
  final bool controlsVisible;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (!media.exists) {
      return const _ViewerMessage('メディアファイルが見つかりません。再走査してください。');
    }
    final thumbnail = media.isVideo
        ? null
        : ref.watch(galleryThumbnailProvider(media.id));
    final preview = media.isVideo
        ? thumbnail?.asData?.value ?? initialPreview
        : null;
    final source = ref.watch(galleryMediaSourcePathProvider(media.id));
    return source.when(
      loading: () => preview == null
          ? const Center(child: CircularProgressIndicator())
          : Image.memory(preview, fit: BoxFit.contain, gaplessPlayback: true),
      error: (_, _) => preview == null
          ? const _ViewerMessage('Vault のメディアを開けませんでした。')
          : Image.memory(preview, fit: BoxFit.contain, gaplessPlayback: true),
      data: (path) {
        if (path == null) {
          return const _ViewerMessage('メディアファイルにアクセスできません。');
        }
        if (media.isVideo) {
          return active
              ? Platform.isAndroid
                    ? _AndroidVideoPlayer(
                        key: ValueKey(media.id),
                        path: path,
                        controlsVisible: controlsVisible,
                      )
                    : _LocalVideoPlayer(
                        path: path,
                        controlsVisible: controlsVisible,
                      )
              : _VideoPreview(mediaId: media.id);
        }
        final pixelWidth =
            (MediaQuery.sizeOf(context).width *
                    MediaQuery.devicePixelRatioOf(context))
                .round()
                .clamp(1, 4096);
        return _ViewerImage(
          key: ValueKey(media.id),
          path: path,
          cacheWidth: pixelWidth,
          onZoomChanged: onImageZoomChanged,
          controlsVisible: controlsVisible,
        );
      },
    );
  }
}

class _ViewerImage extends ConsumerStatefulWidget {
  const _ViewerImage({
    super.key,
    required this.path,
    required this.cacheWidth,
    required this.onZoomChanged,
    required this.controlsVisible,
  });

  final String path;
  final int cacheWidth;
  final ValueChanged<bool> onZoomChanged;
  final bool controlsVisible;

  @override
  ConsumerState<_ViewerImage> createState() => _ViewerImageState();
}

class _ViewerImageState extends ConsumerState<_ViewerImage> {
  final TransformationController transformationController =
      TransformationController();
  bool zoomed = false;
  bool fullImageLoaded = false;
  bool fullImageFailed = false;

  @override
  void didUpdateWidget(covariant _ViewerImage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.path != widget.path) {
      fullImageLoaded = false;
      fullImageFailed = false;
    }
    if (!oldWidget.controlsVisible &&
        widget.controlsVisible &&
        transformationController.value.getMaxScaleOnAxis() > 1.001) {
      transformationController.value = Matrix4.identity();
      zoomed = false;
    }
  }

  @override
  void dispose() {
    transformationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.path.startsWith('content://')) {
      final bytes = ref.watch(gallerySafImageBytesProvider(widget.path));
      final image = bytes.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, _) => const _ViewerMessage('画像を表示できませんでした。'),
        data: (value) => _imageWithFullImage(MemoryImage(value)),
      );
      return _buildImageViewer(image);
    } else {
      return _buildImageViewer(
        _imageWithFullImage(FileImage(File(widget.path))),
      );
    }
  }

  Widget _imageWithFullImage(ImageProvider<Object> provider) => Stack(
    fit: StackFit.expand,
    children: [
      const ColoredBox(color: Colors.black),
      AnimatedOpacity(
        opacity: fullImageLoaded ? 1 : 0,
        duration: const Duration(milliseconds: 120),
        child: Center(
          child: Image(
            image: ResizeImage(provider, width: widget.cacheWidth),
            fit: BoxFit.contain,
            gaplessPlayback: true,
            frameBuilder: (context, child, frame, synchronous) {
              if ((synchronous || frame != null) && !fullImageLoaded) {
                WidgetsBinding.instance.addPostFrameCallback((_) {
                  if (mounted && !fullImageLoaded) {
                    setState(() => fullImageLoaded = true);
                  }
                });
              }
              return child;
            },
            errorBuilder: (context, error, stackTrace) {
              if (!fullImageFailed) {
                WidgetsBinding.instance.addPostFrameCallback((_) {
                  if (mounted && !fullImageFailed) {
                    setState(() => fullImageFailed = true);
                  }
                });
              }
              return const SizedBox.shrink();
            },
          ),
        ),
      ),
      if (fullImageFailed) const _ViewerMessage('画像を表示できませんでした。'),
    ],
  );

  Widget _buildImageViewer(Widget image) {
    return InteractiveViewer(
      transformationController: transformationController,
      minScale: 0.5,
      maxScale: 5,
      panEnabled: zoomed,
      onInteractionUpdate: (_) {
        final isZoomed =
            transformationController.value.getMaxScaleOnAxis() > 1.001;
        if (zoomed != isZoomed) setState(() => zoomed = isZoomed);
      },
      onInteractionEnd: (_) {
        zoomed = transformationController.value.getMaxScaleOnAxis() > 1.001;
        widget.onZoomChanged(zoomed);
      },
      child: Center(child: image),
    );
  }
}

class _ViewerMessage extends StatelessWidget {
  const _ViewerMessage(this.message);

  final String message;

  @override
  Widget build(BuildContext context) => ColoredBox(
    color: Colors.black,
    child: Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Text(
          message,
          textAlign: TextAlign.center,
          style: const TextStyle(color: Colors.white),
        ),
      ),
    ),
  );
}

class _VideoPreview extends ConsumerWidget {
  const _VideoPreview({required this.mediaId});

  final int mediaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final thumbnail = ref.watch(galleryThumbnailProvider(mediaId));
    return Stack(
      fit: StackFit.expand,
      children: [
        thumbnail.when(
          loading: () => const _MediaPlaceholder(hasVideo: true),
          error: (_, _) => const _MediaPlaceholder(hasVideo: true),
          data: (bytes) => bytes == null
              ? const _MediaPlaceholder(hasVideo: true)
              : Image.memory(bytes, fit: BoxFit.contain),
        ),
        const Center(
          child: Icon(Icons.play_circle_outline, size: 56, color: Colors.white),
        ),
      ],
    );
  }
}

class _LocalVideoPlayer extends StatefulWidget {
  const _LocalVideoPlayer({required this.path, required this.controlsVisible});

  final String path;
  final bool controlsVisible;

  @override
  State<_LocalVideoPlayer> createState() => _LocalVideoPlayerState();
}

class _LocalVideoPlayerState extends State<_LocalVideoPlayer> {
  late final Player player;
  late final VideoController controller;
  late final List<StreamSubscription<Object?>> subscriptions;
  bool playbackFailed = false;
  bool playing = false;
  bool looping = false;
  bool muted = false;
  Duration position = Duration.zero;
  Duration duration = Duration.zero;
  double rate = 1;
  double previousVolume = 100;

  @override
  void initState() {
    super.initState();
    player = Player();
    controller = VideoController(player);
    playing = player.state.playing;
    position = player.state.position;
    duration = player.state.duration;
    rate = player.state.rate;
    looping = player.state.playlistMode == PlaylistMode.single;
    muted = player.state.volume == 0;
    if (!muted) previousVolume = player.state.volume;
    subscriptions = [
      player.stream.position.listen((value) => _update(() => position = value)),
      player.stream.duration.listen((value) => _update(() => duration = value)),
      player.stream.playing.listen((value) => _update(() => playing = value)),
      player.stream.rate.listen((value) => _update(() => rate = value)),
      player.stream.volume.listen((value) {
        _update(() {
          muted = value == 0;
          if (value > 0) previousVolume = value;
        });
      }),
      player.stream.playlistMode.listen(
        (value) => _update(() => looping = value == PlaylistMode.single),
      ),
      player.stream.error.listen((_) => _update(() => playbackFailed = true)),
    ];
    _openMedia();
  }

  void _update(VoidCallback update) {
    if (mounted) setState(update);
  }

  Future<void> _openMedia() async {
    try {
      await player.open(Media(widget.path));
    } catch (_) {
      if (mounted) setState(() => playbackFailed = true);
    }
  }

  @override
  void dispose() {
    for (final subscription in subscriptions) {
      unawaited(subscription.cancel());
    }
    unawaited(player.dispose());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => playbackFailed
      ? const _ViewerMessage('この動画を再生できませんでした。')
      : Stack(
          fit: StackFit.expand,
          children: [
            Video(
              controller: controller,
              fit: BoxFit.contain,
              controls: (_) => const SizedBox.shrink(),
            ),
            Positioned(
              left: 0,
              right: 0,
              bottom: 0,
              child: IgnorePointer(
                ignoring: !widget.controlsVisible,
                child: AnimatedOpacity(
                  opacity: widget.controlsVisible ? 1 : 0,
                  duration: GalleryMotion.medium,
                  child: _VideoControlBar(
                    playing: playing,
                    looping: looping,
                    muted: muted,
                    position: position,
                    duration: duration,
                    rate: rate,
                    onPlayPause: () => playing ? player.pause() : player.play(),
                    onSeek: player.seek,
                    onRate: player.setRate,
                    onLoopChanged: (value) => player.setPlaylistMode(
                      value ? PlaylistMode.single : PlaylistMode.none,
                    ),
                    onMuteChanged: (value) {
                      if (value) {
                        previousVolume = player.state.volume > 0
                            ? player.state.volume
                            : previousVolume;
                        player.setVolume(0);
                      } else {
                        player.setVolume(previousVolume);
                      }
                    },
                  ),
                ),
              ),
            ),
          ],
        );
}

class _AndroidVideoPlayer extends StatefulWidget {
  const _AndroidVideoPlayer({
    super.key,
    required this.path,
    required this.controlsVisible,
  });

  final String path;
  final bool controlsVisible;

  @override
  State<_AndroidVideoPlayer> createState() => _AndroidVideoPlayerState();
}

class _AndroidVideoPlayerState extends State<_AndroidVideoPlayer> {
  late final VideoPlayerController controller;
  bool playbackFailed = false;
  bool playing = false;
  bool looping = false;
  bool muted = false;
  Duration position = Duration.zero;
  Duration duration = Duration.zero;
  double rate = 1;
  double previousVolume = 1;

  @override
  void initState() {
    super.initState();
    controller = androidVideoControllerForSource(widget.path);
    controller.addListener(_synchronizeState);
    _openMedia();
  }

  Future<void> _openMedia() async {
    try {
      await controller.initialize();
      if (!mounted) return;
      await controller.setVolume(1);
      await controller.play();
      _synchronizeState();
    } catch (_) {
      if (mounted) setState(() => playbackFailed = true);
    }
  }

  void _synchronizeState() {
    if (!mounted) return;
    final value = controller.value;
    setState(() {
      playbackFailed = value.hasError;
      playing = value.isPlaying;
      position = value.position;
      duration = value.duration;
      rate = value.playbackSpeed;
    });
  }

  Future<void> _toggleMute(bool value) async {
    if (value) {
      previousVolume = controller.value.volume > 0
          ? controller.value.volume
          : previousVolume;
      await controller.setVolume(0);
    } else {
      await controller.setVolume(previousVolume);
    }
    if (mounted) setState(() => muted = value);
  }

  @override
  void dispose() {
    controller.removeListener(_synchronizeState);
    unawaited(controller.dispose());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (playbackFailed) {
      return const _ViewerMessage('この動画を再生できませんでした。');
    }
    final value = controller.value;
    return Stack(
      fit: StackFit.expand,
      children: [
        if (!value.isInitialized)
          const Center(child: CircularProgressIndicator())
        else
          Center(
            child: AspectRatio(
              aspectRatio: value.aspectRatio.isFinite && value.aspectRatio > 0
                  ? value.aspectRatio
                  : 1,
              child: VideoPlayer(controller),
            ),
          ),
        Positioned(
          left: 0,
          right: 0,
          bottom: 0,
          child: IgnorePointer(
            ignoring: !widget.controlsVisible || !value.isInitialized,
            child: AnimatedOpacity(
              opacity: widget.controlsVisible && value.isInitialized ? 1 : 0,
              duration: GalleryMotion.medium,
              child: _VideoControlBar(
                playing: playing,
                looping: looping,
                muted: muted,
                position: position,
                duration: duration,
                rate: rate,
                onPlayPause: () =>
                    playing ? controller.pause() : controller.play(),
                onSeek: controller.seekTo,
                onRate: controller.setPlaybackSpeed,
                onLoopChanged: (value) async {
                  await controller.setLooping(value);
                  if (mounted) setState(() => looping = value);
                },
                onMuteChanged: _toggleMute,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _VideoControlBar extends StatelessWidget {
  const _VideoControlBar({
    required this.playing,
    required this.looping,
    required this.muted,
    required this.position,
    required this.duration,
    required this.rate,
    required this.onPlayPause,
    required this.onSeek,
    required this.onRate,
    required this.onLoopChanged,
    required this.onMuteChanged,
  });

  final bool playing;
  final bool looping;
  final bool muted;
  final Duration position;
  final Duration duration;
  final double rate;
  final VoidCallback onPlayPause;
  final ValueChanged<Duration> onSeek;
  final ValueChanged<double> onRate;
  final ValueChanged<bool> onLoopChanged;
  final ValueChanged<bool> onMuteChanged;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final rawMaxSeconds = duration.inMilliseconds.toDouble();
    final maxSeconds = rawMaxSeconds <= 0 ? 1.0 : rawMaxSeconds;
    final positionMilliseconds = position.inMilliseconds;
    final durationMilliseconds = duration.inMilliseconds;
    final currentSeconds =
        durationMilliseconds > 0 &&
            durationMilliseconds - positionMilliseconds <= 250
        ? maxSeconds
        : positionMilliseconds.toDouble().clamp(0, maxSeconds).toDouble();
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SliderTheme(
            data: SliderTheme.of(context).copyWith(
              activeTrackColor: colorScheme.primary,
              inactiveTrackColor: Colors.white38,
              thumbColor: colorScheme.primary,
              overlayColor: colorScheme.primary.withValues(alpha: 0.2),
            ),
            child: Slider(
              value: currentSeconds,
              max: maxSeconds,
              onChanged: duration == Duration.zero
                  ? null
                  : (value) => onSeek(Duration(milliseconds: value.round())),
            ),
          ),
          Row(
            children: [
              IconButton(
                tooltip: playing ? '一時停止' : '再生',
                onPressed: onPlayPause,
                color: Colors.white,
                icon: Icon(playing ? Icons.pause : Icons.play_arrow),
              ),
              const SizedBox(width: 4),
              Text(
                '${_formatDuration(position)} / ${_formatDuration(duration)}',
                style: Theme.of(context).textTheme.labelMedium
                    ?.copyWith(color: Colors.white),
              ),
              const Spacer(),
              PopupMenuButton<double>(
                tooltip: '再生速度',
                initialValue: rate,
                onSelected: onRate,
                itemBuilder: (context) => [
                  for (final value in const [0.5, 0.75, 1.0, 1.25, 1.5, 2.0])
                    PopupMenuItem(
                      value: value,
                      child: Row(
                        children: [
                          Expanded(child: Text('${value}x')),
                          if (rate == value) const Icon(Icons.check, size: 18),
                        ],
                      ),
                    ),
                ],
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8),
                  child: Text(
                    '${rate}x',
                    style: Theme.of(context).textTheme.labelLarge
                        ?.copyWith(color: Colors.white),
                  ),
                ),
              ),
              IconButton(
                tooltip: looping ? 'ループをオフ' : '1本をループ',
                onPressed: () => onLoopChanged(!looping),
                color: looping ? Colors.white : Colors.white70,
                icon: const Icon(Icons.repeat_one),
              ),
              IconButton(
                tooltip: muted ? 'ミュートを解除' : 'ミュート',
                onPressed: () => onMuteChanged(!muted),
                color: Colors.white,
                icon: Icon(muted ? Icons.volume_off : Icons.volume_up),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

String _formatDuration(Duration value) {
  final hours = value.inHours;
  final minutes = value.inMinutes.remainder(60).toString().padLeft(2, '0');
  final seconds = value.inSeconds.remainder(60).toString().padLeft(2, '0');
  return hours > 0 ? '$hours:$minutes:$seconds' : '$minutes:$seconds';
}

class _NoteDetailsPanel extends ConsumerWidget {
  const _NoteDetailsPanel({
    required this.note,
    required this.author,
    required this.showAuthor,
  });

  final GalleryNoteDetail note;
  final String? author;
  final bool showAuthor;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final postText = _visiblePostText(note.bodyText);
    final order =
        ref
            .watch(galleryTagSettingsProvider)
            .asData
            ?.value
            .noteStructure
            .blockOrder ??
        GalleryNoteStructureSettings.defaultBlockOrder;
    final hiddenBlocks =
        ref
            .watch(galleryTagSettingsProvider)
            .asData
            ?.value
            .noteStructure
            .hiddenBlocks ??
        const <GalleryNoteBlock>[];
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SelectableText(
            note.path,
            style: Theme.of(context).textTheme.bodySmall,
          ),
          if (note.published != null)
            _MetadataRow(
              label: '公開日',
              value: _displayDateTime(note.published!),
            ),
          if (note.created != null)
            _MetadataRow(label: '作成日', value: _displayDateTime(note.created!)),
          if (note.updated != null)
            _MetadataRow(label: '更新日', value: _displayDateTime(note.updated!)),
          if (note.tags.isNotEmpty) ...[
            const SizedBox(height: 12),
            Text('タグ', style: Theme.of(context).textTheme.titleSmall),
            const SizedBox(height: 6),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final tag in [...note.tags]..sort(_compareTags))
                  _ViewerTagChip(tag: tag),
              ],
            ),
          ],
          ...order
              .where(
                (block) =>
                    block != GalleryNoteBlock.media &&
                    block != GalleryNoteBlock.postTextEnd &&
                    !hiddenBlocks.contains(block),
              )
              .map<Widget?>(
                (block) => switch (block) {
                  GalleryNoteBlock.author =>
                    showAuthor && author?.isNotEmpty == true
                        ? _AuthorDetail(author: author!, url: note.authorUrl)
                        : null,
                  GalleryNoteBlock.media ||
                  GalleryNoteBlock.postTextEnd => null,
                  GalleryNoteBlock.postText =>
                    postText.isNotEmpty ? _PostTextCard(text: postText) : null,
                  GalleryNoteBlock.memo =>
                    note.memoLines.isNotEmpty
                        ? _DetailLines(
                            title: '覚書',
                            lines: note.memoLines,
                            commentStyle: true,
                          )
                        : null,
                  GalleryNoteBlock.related =>
                    note.relatedLines.isNotEmpty
                        ? _DetailLines(title: '関連', lines: note.relatedLines)
                        : null,
                },
              )
              .whereType<Widget>(),
        ],
      ),
    );
  }
}

class _AuthorDetail extends StatelessWidget {
  const _AuthorDetail({required this.author, required this.url});

  final String author;
  final String? url;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 16),
    child: Card(
      color: Theme.of(context).colorScheme.surfaceContainerHigh,
      child: ListTile(
        leading: const Icon(Icons.person_outline),
        title: const Text('投稿者'),
        subtitle: Text(author),
        trailing: _isWebUri(url) ? const Icon(Icons.open_in_new) : null,
        onTap: _isWebUri(url)
            ? () => _openExternalUri(context, Uri.parse(url!))
            : null,
      ),
    ),
  );
}

class _MetadataRow extends StatelessWidget {
  const _MetadataRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 8),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(width: 72, child: Text(label)),
        Expanded(child: SelectableText(value)),
      ],
    ),
  );
}

class _DetailLines extends StatelessWidget {
  const _DetailLines({
    required this.title,
    required this.lines,
    this.commentStyle = false,
  });

  final String title;
  final List<GalleryDetailLine> lines;
  final bool commentStyle;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 16),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: Theme.of(context).textTheme.titleSmall),
        for (final line in lines)
          Padding(
            padding: EdgeInsets.only(
              top: 6,
              left: line.indentLevel.clamp(0, 8) * 16.0,
            ),
            child: Card(
              margin: EdgeInsets.zero,
              color: commentStyle
                  ? Theme.of(context).colorScheme.surfaceContainerHighest
                  : Theme.of(context).colorScheme.surfaceContainerHigh,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(GalleryShape.medium),
                side: BorderSide(
                  color: Theme.of(context).colorScheme.outlineVariant
                      .withValues(alpha: 0.5),
                ),
              ),
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (commentStyle || line.linkedNoteId == null) ...[
                      Icon(
                        commentStyle ? Icons.mode_comment_outlined : Icons.link,
                        size: 20,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                      const SizedBox(width: 8),
                    ],
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (!commentStyle && line.linkedNoteId != null)
                            SizedBox(
                              width: double.infinity,
                              child: TextButton(
                                key: ValueKey(
                                  'related-note-${line.linkedNoteId}',
                                ),
                                onPressed: () => Navigator.of(context).push(
                                  MaterialPageRoute<void>(
                                    builder: (context) => _NoteViewerScreen(
                                      noteId: line.linkedNoteId!,
                                      initialMediaId: null,
                                      initiallyShowDetails: true,
                                    ),
                                  ),
                                ),
                                style: TextButton.styleFrom(
                                  alignment: Alignment.centerLeft,
                                  minimumSize: const Size.fromHeight(48),
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 4,
                                    vertical: 4,
                                  ),
                                  tapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                ),
                                clipBehavior: Clip.antiAlias,
                                child: Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const Padding(
                                      padding: EdgeInsets.only(top: 2),
                                      child: Icon(Icons.link, size: 20),
                                    ),
                                    const SizedBox(width: 8),
                                    Expanded(
                                      child: Text(
                                        line.text,
                                        softWrap: true,
                                        style: const TextStyle(
                                          decoration: TextDecoration.underline,
                                          decorationThickness: 1.5,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            )
                          else if (line.urls.length == 1 &&
                              !commentStyle &&
                              _isWebUri(line.urls.single))
                            TextButton.icon(
                              onPressed: () => _openExternalUri(
                                context,
                                Uri.parse(line.urls.single),
                              ),
                              icon: const Icon(Icons.open_in_new, size: 16),
                              label: Text(line.text),
                            )
                          else ...[
                            SelectableText(line.text),
                            if (line.urls.isNotEmpty)
                              Wrap(
                                spacing: 8,
                                children: [
                                  for (final url in line.urls)
                                    if (_isWebUri(url))
                                      TextButton.icon(
                                        onPressed: () => _openExternalUri(
                                          context,
                                          Uri.parse(url),
                                        ),
                                        icon: const Icon(
                                          Icons.open_in_new,
                                          size: 16,
                                        ),
                                        label: const Text('リンクを開く'),
                                      ),
                                ],
                              ),
                          ],
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
      ],
    ),
  );
}

class _ViewerTagChip extends ConsumerWidget {
  const _ViewerTagChip({required this.tag});

  final String tag;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(galleryTagSettingsProvider);
    return settings.when(
      loading: () => const SizedBox.shrink(),
      error: (_, _) => const Tooltip(
        message: 'タグ設定を読み込めませんでした。',
        child: Icon(Icons.warning_amber_rounded),
      ),
      data: (value) {
        if (value.hides(tag)) return const SizedBox.shrink();
        final color = value.colorFor(tag);
        return DecoratedBox(
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.22),
            borderRadius: BorderRadius.circular(GalleryShape.medium),
            border: Border.all(color: color.withValues(alpha: 0.72)),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
            child: Text(
              tag,
              style: Theme.of(context).textTheme.labelMedium
                  ?.copyWith(color: color, fontWeight: FontWeight.w600),
            ),
          ),
        );
      },
    );
  }
}

class _PostTextCard extends StatelessWidget {
  const _PostTextCard({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 16),
    child: Card(
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(GalleryShape.large),
        side: BorderSide(
          color: Theme.of(context).colorScheme.outlineVariant
              .withValues(alpha: 0.85),
        ),
      ),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [SelectableText(text)],
        ),
      ),
    ),
  );
}

int _compareTags(String left, String right) {
  final comparison = left.toLowerCase().compareTo(right.toLowerCase());
  return comparison != 0 ? comparison : left.compareTo(right);
}

String _visiblePostText(String bodyText) => bodyText
    .split('\n')
    .where((line) => !RegExp(r'^>\s*$').hasMatch(line.trim()))
    .join('\n')
    .trim();

String _fictionalNoteExample(GalleryTagSettings settings) {
  final structure = settings.noteStructure;
  final tags = _fictionalNoteTags(settings);
  final blocks = <GalleryNoteBlock, String>{
    GalleryNoteBlock.author:
        '[架空の投稿者](https://example.invalid/authors/fictional)',
    GalleryNoteBlock.media: '![](<../../media/fictional-rainy-window.webp>)',
    GalleryNoteBlock.postText:
        'https://example.invalid/posts/aoikasumi-0001\n\n'
        '> 雨上がりの窓辺で、架空の青い鳥をスケッチしました。',
    GalleryNoteBlock.postTextEnd: '# 文書',
    GalleryNoteBlock.related: '## 関連\n\n- [色の記録](./fictional-color-study.md)',
    GalleryNoteBlock.memo:
        '## 覚書\n\n- 窓の反射を少し弱める\n'
        '  - 青の彩度は控えめにする\n- 次は夕方の光を試す',
  };
  final body = structure.blockOrder
      .where(
        (block) =>
            block == GalleryNoteBlock.media ||
            !structure.hiddenBlocks.contains(block),
      )
      .map((block) => blocks[block])
      .whereType<String>()
      .join('\n\n');
  final yamlTags = tags.isEmpty
      ? 'tags: []'
      : 'tags:\n${tags.map((tag) => '  - $tag').join('\n')}';
  return '''---
url: https://example.invalid/posts/aoikasumi-0001
$yamlTags
published: 2026-09-18T14:20:00
created: 2026-09-18T15:00:00
updated: 2026-09-18T15:10:00
cover: ../../media/fictional-rainy-window.webp
---

# 雨上がりの観測

$body
''';
}

List<String> _fictionalNoteTags(GalleryTagSettings settings) {
  final structure = settings.noteStructure;
  final targetPrefixes = structure.galleryTagPrefixes
      .where((prefix) => prefix != '*')
      .map((prefix) => prefix.replaceFirst(RegExp(r'/+$'), ''))
      .where((prefix) => prefix.isNotEmpty)
      .toList(growable: false);
  final candidates = <String>{};
  if (settings.includedPrefixes.contains('*')) {
    candidates.addAll(targetPrefixes.map((prefix) => '$prefix/example'));
    if (structure.galleryTagPrefixes.contains('*')) {
      candidates.add('source/type/example');
    }
  } else {
    for (final target in targetPrefixes) {
      for (final included in settings.includedPrefixes) {
        if (included == target || included.startsWith('$target/')) {
          candidates.add('$included/example');
        } else if (target.startsWith('$included/')) {
          candidates.add('$target/example');
        }
      }
    }
  }
  return candidates
      .where(settings.includes)
      .where((tag) => !settings.hides(tag))
      .toList()
    ..sort();
}

String _displayDateTime(String value) => value.replaceFirst('T', ' ');

String _viewerTitle(GalleryNoteDetail note) {
  final filename = p.basenameWithoutExtension(note.path);
  final titleSeparator = filename.indexOf('-on-X-');
  if (titleSeparator >= 0 && titleSeparator + 6 < filename.length) {
    return filename.substring(titleSeparator + 6);
  }
  return note.title;
}

String? _viewerAuthor(GalleryNoteDetail note, Uri? profileUrl) {
  if (note.author?.isNotEmpty == true) return note.author;
  if (profileUrl != null && profileUrl.pathSegments.isNotEmpty) {
    return '@${profileUrl.pathSegments.first}';
  }
  final filename = p.basenameWithoutExtension(note.path);
  final titleSeparator = filename.indexOf('-on-X-');
  if (titleSeparator > 0) return filename.substring(0, titleSeparator);
  return null;
}

Uri? _profileUrl(String? postUrl) {
  if (!_isWebUri(postUrl)) return null;
  final uri = Uri.parse(postUrl!);
  final host = uri.host.toLowerCase();
  if (host != 'x.com' &&
      host != 'www.x.com' &&
      host != 'twitter.com' &&
      host != 'www.twitter.com') {
    return null;
  }
  if (uri.pathSegments.length < 3 || uri.pathSegments[1] != 'status') {
    return null;
  }
  final username = uri.pathSegments.first;
  if (username.isEmpty) return null;
  return Uri.https('x.com', '/$username');
}

bool _isWebUri(String? value) {
  if (value == null) return false;
  final uri = Uri.tryParse(value);
  return uri != null &&
      (uri.scheme == 'http' || uri.scheme == 'https') &&
      uri.host.isNotEmpty &&
      uri.userInfo.isEmpty;
}

Future<void> _openExternalUri(BuildContext context, Uri uri) async {
  bool launched;
  try {
    launched = await launchUrl(uri, mode: LaunchMode.externalApplication);
  } on PlatformException {
    launched = false;
  }
  if (!launched && context.mounted) {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('外部アプリで開けませんでした。対応アプリを確認してください。')),
    );
  }
}
