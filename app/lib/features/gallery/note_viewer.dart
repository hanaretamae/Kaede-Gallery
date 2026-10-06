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
        appBar: M3EAppBar.top(leading: _expressiveBackButton(context)),
        body: const ColoredBox(
          color: Colors.black,
          child: Center(child: M3EProgressIndicator.circular()),
        ),
      ),
      error: (_, _) => Scaffold(
        appBar: M3EAppBar.top(leading: _expressiveBackButton(context)),
        body: Center(child: Text(context.l10n.couldnLoadTheNoteDetails)),
      ),
      data: (note) => note == null
          ? Scaffold(
              appBar: M3EAppBar.top(leading: _expressiveBackButton(context)),
              body: Center(
                child: Text(context.l10n.theNoteWasNotFoundPleaseRescan),
              ),
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
      duration: GalleryMotion.duration(GalleryMotion.emphasized),
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
        M3ESnackbar.show(context, message: context.l10n.couldnToggleFullscreen);
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
        M3ESnackbar.show(
          context,
          message: context.l10n.couldnSwitchToTheDetailsView,
        );
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
      duration: GalleryMotion.duration(GalleryMotion.medium),
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
                                ? _ViewerMessage(
                                    context.l10n.thereIsNoMediaToShowInThisNote,
                                  )
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
                      safAccess: ref.read(vaultPlatformProvider).safAccess,
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
                      onOpenObsidian: session == null
                          ? null
                          : () => _openExternalUri(
                              context,
                              obsidianOpenUri(session.vaultPath, note.path),
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
                                M3ESnackbar.show(
                                  context,
                                  message: context.l10n.copiedTheURL,
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
    required this.safAccess,
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

  final SafVaultAccess safAccess;
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
        child: M3EAppBar.top(
          semanticLabel: context.l10n.noteDetailsActions,
          backgroundColor: Theme.of(context).colorScheme.surface,
          foregroundColor: Theme.of(context).colorScheme.onSurface,
          elevation: 2,
          title: galleryAppBarTitle(context, title),
          subtitleText: author,
          leading: M3EIconButton(
            variant: M3EIconButtonVariant.standard,
            tooltip: context.l10n.close,
            onPressed: onClose,
            icon: const Icon(Icons.arrow_back),
          ),
          actions: [
            M3EIconButton(
              variant: M3EIconButtonVariant.standard,
              tooltip: fullscreen
                  ? context.l10n.exitFullscreen
                  : context.l10n.enterFullscreen,
              onPressed: onToggleFullscreen,
              icon: Icon(fullscreen ? Icons.fullscreen_exit : Icons.fullscreen),
            ),
            if (profileUrl != null)
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: context.l10n.openAuthorProfile,
                onPressed: onOpenProfile,
                icon: const Icon(Icons.person_outline),
              ),
            if (_isWebUri(postUrl))
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: context.l10n.openOriginalPage,
                onPressed: onOpenPost,
                icon: const Icon(Icons.link),
              ),
            if (_isWebUri(postUrl))
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: context.l10n.copyPageURL,
                onPressed: onCopyPostUrl,
                icon: const Icon(Icons.copy),
              ),
            if (mediaPath != null)
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: context.l10n.openMedia,
                onPressed: () => _showMediaOpenActions(
                  context,
                  mediaPath!,
                  safAccess: safAccess,
                  vaultPath: vaultPath,
                ),
                icon: const Icon(Icons.perm_media_outlined),
              ),
            if (onOpenObsidian != null)
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: context.l10n.openNoteInObsidian,
                onPressed: onOpenObsidian,
                icon: const Icon(Icons.open_in_new),
              ),
          ],
        ),
      ),
    ),
  );
}

Future<void> _showMediaOpenActions(
  BuildContext context,
  String mediaPath, {
  required SafVaultAccess safAccess,
  String? vaultPath,
}) async {
  final canSetWallpaper = _canSetWallpaper(mediaPath);
  final options = <({String label, IconData icon, _MediaOpenAction action})>[
    (
      label: context.l10n.chooseAnAppToOpenTheImageOrVideo,
      icon: Icons.open_in_new,
      action: _MediaOpenAction.chooseApp,
    ),
    if (canSetWallpaper)
      (
        label: context.l10n.setImageAsWallpaper,
        icon: Icons.wallpaper,
        action: _MediaOpenAction.setWallpaper,
      ),
    (
      label: context.l10n.showFileInFileManager,
      icon: Icons.folder_open,
      action: _MediaOpenAction.revealInFileManager,
    ),
  ];
  final _MediaOpenAction? action;
  if (Platform.isAndroid) {
    action = await M3EBottomSheet.showAdaptive<_MediaOpenAction>(
      context,
      title: context.l10n.openMedia,
      builder: (context) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
          child: _settingsGroup(context, [
            for (final option in options)
              M3EListItem(
                headline: option.label,
                leading: Icon(option.icon),
                onTap: () => Navigator.of(context).pop(option.action),
              ),
          ]),
        ),
      ),
    );
  } else {
    // Desktop side sheets are too narrow for these labels; use a dialog.
    action = await showDialog<_MediaOpenAction>(
      context: context,
      builder: (context) => SimpleDialog(
        title: Text(context.l10n.openMedia),
        children: [
          for (final option in options)
            ListTile(
              leading: Icon(option.icon),
              title: Text(option.label),
              onTap: () => Navigator.of(context).pop(option.action),
            ),
        ],
      ),
    );
  }
  if (action == null || !context.mounted) return;
  final revealInFileManager = action == _MediaOpenAction.revealInFileManager;
  final setAsWallpaper = action == _MediaOpenAction.setWallpaper;
  if (mediaPath.startsWith('content://')) {
    if (vaultPath == null || !vaultPath.startsWith('content://')) return;
    try {
      await safAccess.openMedia(
        vaultPath,
        mediaPath,
        revealInFileManager: revealInFileManager,
        setAsWallpaper: setAsWallpaper,
      );
    } on PlatformException catch (error) {
      if (!context.mounted) return;
      M3ESnackbar.show(
        context,
        message: error.code == 'SAF_UNSUPPORTED'
            ? context.l10n.theSelectedAppCouldNotShowTheFile
            : context.l10n.couldnOpenTheMedia,
      );
    }
    return;
  }
  if (setAsWallpaper) {
    final ok = await _setLinuxWallpaper(mediaPath);
    if (!context.mounted) return;
    M3ESnackbar.show(
      context,
      message: ok
          ? context.l10n.setAsWallpaper
          : context.l10n.couldnSetAsWallpaper,
    );
    return;
  }
  if (revealInFileManager && Platform.isLinux) {
    try {
      await revealFileInLinuxFileManager(mediaPath);
    } on DBusMethodResponseException {
      if (!context.mounted) return;
      M3ESnackbar.show(
        context,
        message: context.l10n.couldnShowTheFileInTheFileManager,
      );
    } on DBusReplySignatureException {
      if (!context.mounted) return;
      M3ESnackbar.show(
        context,
        message: context.l10n.couldnShowTheFileInTheFileManager,
      );
    } on SocketException {
      if (!context.mounted) return;
      M3ESnackbar.show(
        context,
        message: context.l10n.couldnLaunchTheFileManager,
      );
    }
    return;
  }
  if (revealInFileManager && Platform.isWindows) {
    try {
      await Process.start('explorer.exe', [
        '/select,',
        p.windows.normalize(normalizeLocalPath(mediaPath)),
      ]);
    } on ProcessException {
      if (!context.mounted) return;
      M3ESnackbar.show(
        context,
        message: context.l10n.couldnLaunchTheFileManager,
      );
    }
    return;
  }
  final uri = Uri.file(normalizeLocalPath(mediaPath));
  await _openExternalUri(context, uri);
}

enum _MediaOpenAction { chooseApp, revealInFileManager, setWallpaper }

const _wallpaperExtensions = {'.jpg', '.jpeg', '.png', '.webp', '.bmp', '.gif'};

// Android resolves the image type from the content provider; Linux needs a
// local image file and a GNOME session.
bool _canSetWallpaper(String mediaPath) {
  var lower = mediaPath.toLowerCase();
  try {
    lower = Uri.decodeFull(mediaPath).toLowerCase();
  } on ArgumentError {
    // Fall back to the raw path.
  }
  if (!_wallpaperExtensions.any(lower.endsWith)) return false;
  if (mediaPath.startsWith('content://')) return Platform.isAndroid;
  if (!Platform.isLinux) return false;
  final desktop = Platform.environment['XDG_CURRENT_DESKTOP'] ?? '';
  return desktop.toUpperCase().contains('GNOME');
}

Future<bool> _setLinuxWallpaper(String path) async {
  final uri = Uri.file(path).toString();
  try {
    for (final key in const ['picture-uri', 'picture-uri-dark']) {
      final result = await Process.run('gsettings', [
        'set',
        'org.gnome.desktop.background',
        key,
        uri,
      ]);
      if (result.exitCode != 0) return false;
    }
    return true;
  } on ProcessException {
    return false;
  }
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
  Widget build(BuildContext context) => M3ECard(
    variant: M3ECardVariant.filled,
    color: _viewerDetailsColor(context),
    elevation: 3,
    expanded: true,
    focusable: false,
    showFocusRing: false,
    showFocusFill: false,
    trackHover: false,
    borderRadius: BorderRadius.zero,
    padding: EdgeInsets.zero,
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
      return _ViewerMessage(context.l10n.theMediaFileWasNotFoundPleaseRescan);
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
          ? const Center(child: M3EProgressIndicator.circular())
          : Image.memory(preview, fit: BoxFit.contain, gaplessPlayback: true),
      error: (_, _) => preview == null
          ? _ViewerMessage(context.l10n.couldnOpenTheVaultMedia)
          : Image.memory(preview, fit: BoxFit.contain, gaplessPlayback: true),
      data: (path) {
        if (path == null) {
          return _ViewerMessage(context.l10n.canAccessTheMediaFile);
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
        loading: () => const Center(child: M3EProgressIndicator.circular()),
        error: (_, _) => _ViewerMessage(context.l10n.couldnDisplayTheImage),
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
        duration: GalleryMotion.duration(M3EMotion.short2),
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
      if (fullImageFailed) _ViewerMessage(context.l10n.couldnDisplayTheImage),
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
      ? _ViewerMessage(context.l10n.couldnPlayThisVideo)
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
                  duration: GalleryMotion.duration(GalleryMotion.medium),
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
      return _ViewerMessage(context.l10n.couldnPlayThisVideo);
    }
    final value = controller.value;
    return Stack(
      fit: StackFit.expand,
      children: [
        if (!value.isInitialized)
          const Center(child: M3EProgressIndicator.circular())
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
              duration: GalleryMotion.duration(GalleryMotion.medium),
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
          M3ETheme(
            data: _wideIndicatorTheme(context),
            child: M3ESlider(
              value: currentSeconds,
              max: maxSeconds,
              semanticLabel: context.l10n.videoPlaybackPosition,
              label: _formatDuration(
                Duration(milliseconds: currentSeconds.round()),
              ),
              semanticFormatterCallback: (value) =>
                  _formatDuration(Duration(milliseconds: value.round())),
              onChanged: duration == Duration.zero
                  ? null
                  : (value) => onSeek(Duration(milliseconds: value.round())),
            ),
          ),
          Row(
            children: [
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: playing ? context.l10n.pause : context.l10n.play,
                onPressed: onPlayPause,
                icon: Icon(
                  playing ? Icons.pause : Icons.play_arrow,
                  color: Colors.white,
                ),
              ),
              const SizedBox(width: 4),
              Text(
                '${_formatDuration(position)} / ${_formatDuration(duration)}',
                style: Theme.of(context).textTheme.labelMedium
                    ?.copyWith(color: Colors.white),
              ),
              const Spacer(),
              M3EMenu(
                selectedValue: rate,
                onSelected: (value) {
                  if (value is double) onRate(value);
                },
                anchorBuilder: (context, open) => M3EButton.text(
                  onPressed: open,
                  child: Text(
                    '${rate}x',
                    style: Theme.of(context).textTheme.labelLarge
                        ?.copyWith(color: Colors.white),
                  ),
                ),
                children: [
                  for (final value in const [0.5, 0.75, 1.0, 1.25, 1.5, 2.0])
                    M3EMenuSelectable(
                      value: value,
                      label: '${value}x',
                      selected: rate == value,
                    ),
                ],
              ),
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: looping
                    ? context.l10n.turnLoopingOff
                    : context.l10n.loopCurrentVideo,
                onPressed: () => onLoopChanged(!looping),
                icon: Icon(
                  Icons.repeat_one,
                  color: looping ? Colors.white : Colors.white70,
                ),
              ),
              M3EIconButton(
                variant: M3EIconButtonVariant.standard,
                tooltip: muted ? context.l10n.unmute : context.l10n.mute,
                onPressed: () => onMuteChanged(!muted),
                icon: Icon(
                  muted ? Icons.volume_off : Icons.volume_up,
                  color: Colors.white,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

M3EThemeData _wideIndicatorTheme(BuildContext context) {
  final theme = M3ETheme.of(context);
  return theme.copyWith(
    sliderTheme: theme.sliderTheme.copyWith(valueIndicatorWidth: 96),
  );
}

String _formatDuration(Duration value) {
  final minutes = value.inMinutes.remainder(60).toString().padLeft(2, '0');
  final seconds = value.inSeconds.remainder(60).toString().padLeft(2, '0');
  if (value.inHours == 0) return '$minutes:$seconds';
  return '${value.inHours.toString().padLeft(2, '0')}:$minutes:$seconds';
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
          SizedBox(
            width: double.infinity,
            child: SelectableText(
              note.path,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          if (note.published != null ||
              note.created != null ||
              note.updated != null)
            _FrontmatterDates(
              published: note.published,
              created: note.created,
              updated: note.updated,
            ),
          if (note.tags.isNotEmpty) ...[
            const SizedBox(height: 12),
            Text(
              context.l10n.tags,
              style: Theme.of(context).textTheme.titleSmall,
            ),
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
                            title: context.l10n.notes_ab7203,
                            lines: note.memoLines,
                            commentStyle: true,
                          )
                        : null,
                  GalleryNoteBlock.related =>
                    note.relatedLines.isNotEmpty
                        ? _DetailLines(
                            title: context.l10n.related,
                            lines: note.relatedLines,
                          )
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
    child: M3EListItem(
      headline: context.l10n.author,
      supportingText: author,
      leading: const Icon(Icons.person_outline),
      trailing: _isWebUri(url) ? const Icon(Icons.open_in_new) : null,
      onTap: _isWebUri(url)
          ? () => _openExternalUri(context, Uri.parse(url!))
          : null,
    ),
  );
}

class _FrontmatterDates extends StatelessWidget {
  const _FrontmatterDates({this.published, this.created, this.updated});

  final String? published;
  final String? created;
  final String? updated;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final dates = [
      if (published != null) (context.l10n.published, published!),
      if (created != null) (context.l10n.created, created!),
      if (updated != null) (context.l10n.updated, updated!),
    ];
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: M3ECard(
        variant: M3ECardVariant.filled,
        color: scheme.secondaryContainer,
        padding: EdgeInsets.zero,
        borderRadius: BorderRadius.circular(GalleryShape.medium),
        focusable: false,
        showFocusRing: false,
        showFocusFill: false,
        trackHover: false,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              for (final (label, value) in dates)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SizedBox(
                        width: 64,
                        child: Text(
                          label,
                          style: Theme.of(context).textTheme.labelMedium
                              ?.copyWith(
                                color: scheme.onSecondaryContainer,
                                fontWeight: FontWeight.w600,
                              ),
                        ),
                      ),
                      Expanded(
                        child: SelectableText(
                          _displayDateTime(value),
                          style: Theme.of(context).textTheme.bodySmall
                              ?.copyWith(color: scheme.onSecondaryContainer),
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
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
            child: M3ECard(
              variant: M3ECardVariant.filled,
              focusable: false,
              showFocusRing: false,
              showFocusFill: false,
              trackHover: false,
              padding: EdgeInsets.zero,
              color: commentStyle
                  ? Theme.of(context).colorScheme.surfaceContainerHighest
                  : Theme.of(context).colorScheme.surfaceContainerHigh,
              borderRadius: BorderRadius.circular(GalleryShape.medium),
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
                              child: M3EButton(
                                style: M3EButtonStyle.text,
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
                            M3EButton.icon(
                              style: M3EButtonStyle.text,
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
                                      M3EButton.icon(
                                        style: M3EButtonStyle.text,
                                        onPressed: () => _openExternalUri(
                                          context,
                                          Uri.parse(url),
                                        ),
                                        icon: const Icon(
                                          Icons.open_in_new,
                                          size: 16,
                                        ),
                                        label: Text(context.l10n.openLink),
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
      error: (_, _) => M3ETooltip(
        message: context.l10n.couldnLoadTheTagSettings,
        child: Icon(Icons.warning_amber_rounded),
      ),
      data: (value) {
        if (value.hides(tag)) return const SizedBox.shrink();
        final scheme = Theme.of(context).colorScheme;
        final tagColor = value.colorFor(tag);
        final tagScheme = ColorScheme.fromSeed(
          seedColor: tagColor,
          brightness: scheme.brightness,
        );
        return M3ECard(
          variant: M3ECardVariant.filled,
          color: tagScheme.primaryContainer,
          padding: EdgeInsets.zero,
          borderRadius: BorderRadius.circular(GalleryShape.medium),
          focusable: false,
          showFocusRing: false,
          showFocusFill: false,
          trackHover: false,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            child: SelectableText(
              tag,
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                color: tagScheme.onPrimaryContainer,
                fontWeight: FontWeight.w600,
              ),
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
    child: M3ECard(
      variant: M3ECardVariant.filled,
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      borderRadius: BorderRadius.circular(GalleryShape.large),
      focusable: false,
      showFocusRing: false,
      showFocusFill: false,
      trackHover: false,
      padding: EdgeInsets.zero,
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
        AppL10n.current.fictionalAuthorHttpsExampleInvalidAuthorsFiction,
    GalleryNoteBlock.media: '![](<../../media/fictional-rainy-window.webp>)',
    GalleryNoteBlock.postText:
        AppL10n.current.httpsExampleInvalidPostsAoikasumi0001NISketchedA,
    GalleryNoteBlock.postTextEnd: AppL10n.current.document,
    GalleryNoteBlock.related:
        AppL10n.current.relatedNColorStudyFictionalColorStudyMd,
    GalleryNoteBlock.memo:
        AppL10n.current.notesNSoftenTheWindowReflectionsALittleKeepTheBl,
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

${AppL10n.current.observationsAfterTheRain}

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
    M3ESnackbar.show(
      context,
      message: context.l10n.couldnOpenItInAnExternalAppCheckForACompatibleAp,
    );
  }
}

/// Pure-black mode paints the details panel black; otherwise a tonal surface.
Color _viewerDetailsColor(BuildContext context) {
  final scheme = Theme.of(context).colorScheme;
  return scheme.surface == Colors.black
      ? Colors.black
      : scheme.surfaceContainerLow;
}
