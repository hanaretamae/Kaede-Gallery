import 'package:flutter/material.dart';
import 'package:material_3_expressive/material_3_expressive.dart' as m3e;
import 'package:material_ui/material_ui.dart' as mui;

abstract final class GalleryMotion {
  static const short = m3e.M3EMotion.short3;
  static const medium = m3e.M3EMotion.medium1;
  static const emphasized = m3e.M3EMotion.medium3;
  static const emphasizedCurve = m3e.M3EMotion.emphasized;

  static Duration duration(Duration token) =>
      WidgetsBinding
          .instance
          .platformDispatcher
          .accessibilityFeatures
          .disableAnimations
      ? Duration.zero
      : token;
}

abstract final class GalleryShape {
  static const small = 16.0;
  static const medium = 22.0;
  static const large = 28.0;
  static const extraLarge = 36.0;
}

Widget galleryAppBarTitle(BuildContext context, String title) => Text(
  title,
  maxLines: 1,
  overflow: TextOverflow.ellipsis,
  style: Theme.of(context).textTheme.titleLarge
      ?.copyWith(fontWeight: FontWeight.w600),
);

ColorScheme materialYouScheme(
  ColorScheme systemScheme, {
  required Brightness brightness,
}) => ColorScheme.fromSeed(
  seedColor: systemScheme.primary,
  brightness: brightness,
);

m3e.M3EButtonDecoration galleryChoiceButtonDecoration(ColorScheme scheme) =>
    m3e.M3EButtonDecoration(
      backgroundColor: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.disabled)) {
          return Color.alphaBlend(
            scheme.onSurface.withValues(alpha: 0.12),
            scheme.surfaceContainerHighest,
          );
        }
        return states.contains(WidgetState.selected)
            ? scheme.primary
            : scheme.surfaceContainerHighest;
      }),
      foregroundColor: WidgetStateProperty.resolveWith((states) {
        if (states.contains(WidgetState.disabled)) {
          return scheme.onSurface.withValues(alpha: 0.38);
        }
        return states.contains(WidgetState.selected)
            ? scheme.onPrimary
            : scheme.onSurface;
      }),
      shadowColor: const WidgetStatePropertyAll(Colors.transparent),
      elevation: const WidgetStatePropertyAll(0),
      side: const WidgetStatePropertyAll(BorderSide.none),
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
      filled: false,
      fillColor: Colors.transparent,
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

final _expressiveThemeCache = Expando<m3e.M3EThemeData>();

// ColorScheme.fromSeed は重いため、ThemeData ごとに結果を使い回す。
m3e.M3EThemeData expressiveThemeFromMaterial(ThemeData theme) =>
    _expressiveThemeCache[theme] ??= _buildExpressiveTheme(theme);

m3e.M3EThemeData _buildExpressiveTheme(ThemeData theme) {
  final flutterScheme = theme.colorScheme;
  final scheme =
      mui.ColorScheme.fromSeed(
        seedColor: flutterScheme.primary,
        brightness: flutterScheme.brightness,
      ).copyWith(
        primary: flutterScheme.primary,
        onPrimary: flutterScheme.onPrimary,
        primaryContainer: flutterScheme.primaryContainer,
        onPrimaryContainer: flutterScheme.onPrimaryContainer,
        secondary: flutterScheme.secondary,
        onSecondary: flutterScheme.onSecondary,
        secondaryContainer: flutterScheme.secondaryContainer,
        onSecondaryContainer: flutterScheme.onSecondaryContainer,
        tertiary: flutterScheme.tertiary,
        onTertiary: flutterScheme.onTertiary,
        tertiaryContainer: flutterScheme.tertiaryContainer,
        onTertiaryContainer: flutterScheme.onTertiaryContainer,
        error: flutterScheme.error,
        onError: flutterScheme.onError,
        errorContainer: flutterScheme.errorContainer,
        onErrorContainer: flutterScheme.onErrorContainer,
        surface: flutterScheme.surface,
        onSurface: flutterScheme.onSurface,
        onSurfaceVariant: flutterScheme.onSurfaceVariant,
        surfaceContainerLowest: flutterScheme.surfaceContainerLowest,
        surfaceContainerLow: flutterScheme.surfaceContainerLow,
        surfaceContainer: flutterScheme.surfaceContainer,
        surfaceContainerHigh: flutterScheme.surfaceContainerHigh,
        surfaceContainerHighest: flutterScheme.surfaceContainerHighest,
        surfaceDim: flutterScheme.surfaceDim,
        surfaceBright: flutterScheme.surfaceBright,
        inverseSurface: flutterScheme.inverseSurface,
        onInverseSurface: flutterScheme.onInverseSurface,
        inversePrimary: flutterScheme.inversePrimary,
        outline: flutterScheme.outline,
        outlineVariant: flutterScheme.outlineVariant,
        shadow: flutterScheme.shadow,
        scrim: flutterScheme.scrim,
        surfaceTint: flutterScheme.surfaceTint,
      );
  return m3e.M3EThemeData.fromMaterial(mui.ThemeData.from(colorScheme: scheme));
}

class ExpressiveMaterialScope extends StatelessWidget {
  const ExpressiveMaterialScope({
    required this.theme,
    required this.child,
    super.key,
  });

  final ThemeData theme;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final expressiveTheme = expressiveThemeFromMaterial(theme);
    return m3e.M3ETheme(
      data: expressiveTheme,
      child: mui.Theme(
        data: expressiveTheme.toThemeData(),
        child: mui.Material(type: mui.MaterialType.transparency, child: child),
      ),
    );
  }
}
