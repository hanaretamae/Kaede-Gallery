import 'package:flutter/material.dart';

abstract final class GalleryMotion {
  static const short = Duration(milliseconds: 160);
  static const medium = Duration(milliseconds: 240);
  static const emphasized = Duration(milliseconds: 360);
  static const emphasizedCurve = Curves.easeInOutCubicEmphasized;
}

abstract final class GalleryShape {
  static const small = 16.0;
  static const medium = 22.0;
  static const large = 28.0;
  static const extraLarge = 36.0;
}

ColorScheme materialYouScheme(
  ColorScheme systemScheme, {
  required Brightness brightness,
}) => ColorScheme.fromSeed(
  seedColor: systemScheme.primary,
  brightness: brightness,
);

ThemeData galleryTheme(
  Color seed, {
  Brightness brightness = Brightness.light,
  ColorScheme? dynamicScheme,
  bool pureBlack = false,
}) {
  final generatedScheme =
      dynamicScheme ??
      ColorScheme.fromSeed(seedColor: seed, brightness: brightness);
  final colorScheme = pureBlack && brightness == Brightness.dark
      ? generatedScheme.copyWith(
          surface: Colors.black,
          surfaceDim: Colors.black,
          surfaceBright: Colors.black,
          surfaceContainerLowest: const Color(0xFF101012),
          surfaceContainerLow: const Color(0xFF191A1E),
          surfaceContainer: const Color(0xFF202126),
          surfaceContainerHigh: const Color(0xFF27282E),
          surfaceContainerHighest: const Color(0xFF2E2F35),
        )
      : generatedScheme;
  final outline = colorScheme.outlineVariant;
  final roundedOutline = OutlineInputBorder(
    borderRadius: BorderRadius.circular(GalleryShape.large),
    borderSide: BorderSide(color: outline),
  );

  return ThemeData(
    colorScheme: colorScheme,
    useMaterial3: true,
    visualDensity: VisualDensity.standard,
    scaffoldBackgroundColor: colorScheme.surface,
    appBarTheme: AppBarTheme(
      backgroundColor: colorScheme.surface,
      foregroundColor: colorScheme.onSurface,
      scrolledUnderElevation: 0,
      titleTextStyle: TextStyle(
        color: colorScheme.onSurface,
        fontSize: 22,
        fontWeight: FontWeight.w600,
        letterSpacing: 0,
      ),
    ),
    cardTheme: CardThemeData(
      color: colorScheme.surfaceContainerLow,
      elevation: 0,
      margin: const EdgeInsets.only(bottom: 8),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(GalleryShape.large),
      ),
    ),
    dialogTheme: DialogThemeData(
      backgroundColor: colorScheme.surfaceContainerHigh,
      elevation: 3,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(GalleryShape.extraLarge),
      ),
      titleTextStyle: TextStyle(
        color: colorScheme.onSurface,
        fontSize: 24,
        fontWeight: FontWeight.w600,
      ),
    ),
    bottomSheetTheme: BottomSheetThemeData(
      backgroundColor: colorScheme.surfaceContainerLow,
      modalBackgroundColor: colorScheme.surfaceContainerLow,
      elevation: 3,
      showDragHandle: true,
      dragHandleColor: colorScheme.outlineVariant,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(GalleryShape.extraLarge),
        ),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: colorScheme.surfaceContainerLow,
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      border: roundedOutline,
      enabledBorder: roundedOutline,
      focusedBorder: roundedOutline.copyWith(
        borderSide: BorderSide(color: colorScheme.primary, width: 2),
      ),
      errorBorder: roundedOutline.copyWith(
        borderSide: BorderSide(color: colorScheme.error),
      ),
      focusedErrorBorder: roundedOutline.copyWith(
        borderSide: BorderSide(color: colorScheme.error, width: 2),
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size(48, 48),
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(GalleryShape.medium),
        ),
      ),
    ),
    elevatedButtonTheme: ElevatedButtonThemeData(
      style: ElevatedButton.styleFrom(
        minimumSize: const Size(48, 48),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(GalleryShape.medium),
        ),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(48, 48),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(GalleryShape.medium),
        ),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        minimumSize: const Size(48, 48),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(GalleryShape.medium),
        ),
      ),
    ),
    iconButtonTheme: IconButtonThemeData(
      style: IconButton.styleFrom(
        minimumSize: const Size(48, 48),
        shape: const CircleBorder(),
      ),
    ),
    segmentedButtonTheme: SegmentedButtonThemeData(
      style: ButtonStyle(
        minimumSize: const WidgetStatePropertyAll(Size(48, 48)),
        shape: WidgetStatePropertyAll(
          RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(GalleryShape.medium),
          ),
        ),
      ),
    ),
    chipTheme: ChipThemeData(
      showCheckmark: true,
      shape: const StadiumBorder(),
      side: BorderSide.none,
      backgroundColor: colorScheme.surfaceContainerHighest,
      selectedColor: colorScheme.secondaryContainer,
      labelStyle: TextStyle(color: colorScheme.onSurface),
      secondaryLabelStyle: TextStyle(color: colorScheme.onSecondaryContainer),
    ),
    listTileTheme: ListTileThemeData(
      minVerticalPadding: 12,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(GalleryShape.medium),
      ),
    ),
    menuTheme: MenuThemeData(
      style: MenuStyle(
        backgroundColor: WidgetStatePropertyAll(
          colorScheme.surfaceContainerHigh,
        ),
        elevation: const WidgetStatePropertyAll(3),
        shape: WidgetStatePropertyAll(
          RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(GalleryShape.large),
          ),
        ),
      ),
    ),
    snackBarTheme: SnackBarThemeData(
      behavior: SnackBarBehavior.floating,
      backgroundColor: colorScheme.inverseSurface,
      contentTextStyle: TextStyle(color: colorScheme.onInverseSurface),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(GalleryShape.medium),
      ),
    ),
    progressIndicatorTheme: ProgressIndicatorThemeData(
      color: colorScheme.primary,
      linearTrackColor: colorScheme.secondaryContainer,
      circularTrackColor: colorScheme.secondaryContainer,
    ),
    dividerTheme: DividerThemeData(
      color: outline.withValues(alpha: 0.5),
      space: 1,
    ),
  );
}
