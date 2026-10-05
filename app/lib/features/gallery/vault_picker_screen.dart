import 'package:material_3_expressive/material_3_expressive.dart';
import 'package:material_ui/material_ui.dart' as mui;
import 'package:flutter/widgets.dart'
    show BuildContext, StatelessWidget, VoidCallback, Widget;

import '../../l10n.dart';

class VaultPickerScreen extends StatelessWidget {
  const VaultPickerScreen({
    required this.onChooseVault,
    required this.onShowNoteExample,
    required this.expressiveTheme,
    super.key,
  });

  final VoidCallback onChooseVault;
  final VoidCallback onShowNoteExample;
  final M3EThemeData expressiveTheme;

  @override
  Widget build(BuildContext context) => M3EMaterialApp(
    data: expressiveTheme,
    theme: expressiveTheme.toThemeData(),
    darkTheme: expressiveTheme.toThemeData(),
    initialTheme: expressiveTheme.brightness,
    autoTheming: false,
    dynamicColoring: false,
    home: _VaultPickerContent(
      onChooseVault: onChooseVault,
      onShowNoteExample: onShowNoteExample,
      expressiveTheme: expressiveTheme,
    ),
  );
}

class _VaultPickerContent extends StatelessWidget {
  const _VaultPickerContent({
    required this.onChooseVault,
    required this.onShowNoteExample,
    required this.expressiveTheme,
  });

  final VoidCallback onChooseVault;
  final VoidCallback onShowNoteExample;
  final M3EThemeData expressiveTheme;

  @override
  Widget build(BuildContext context) => mui.Scaffold(
    appBar: const M3EAppBar.top(),
    body: mui.Stack(
      children: [
        mui.Padding(
          padding: const mui.EdgeInsets.only(bottom: 80),
          child: mui.Center(
            child: mui.Padding(
              padding: const mui.EdgeInsets.all(24),
              child: mui.Column(
                mainAxisSize: mui.MainAxisSize.min,
                children: [
                  mui.Container(
                    width: 88,
                    height: 88,
                    decoration: mui.BoxDecoration(
                      color: expressiveTheme.colorScheme.primaryContainer,
                      borderRadius: mui.BorderRadius.circular(36),
                    ),
                    child: mui.Icon(
                      mui.Icons.photo_library_outlined,
                      size: 42,
                      color: expressiveTheme.colorScheme.onPrimaryContainer,
                    ),
                  ),
                  const mui.SizedBox(height: 24),
                  mui.Text(
                    'Kaede Gallery',
                    style: expressiveTheme.typography.baseline.headlineMedium
                        .copyWith(fontWeight: mui.FontWeight.w600),
                  ),
                  const mui.SizedBox(height: 8),
                  mui.Text(
                    context.l10n.browseMediaFromYourObsidianVaultOffline,
                    textAlign: mui.TextAlign.center,
                    style: expressiveTheme.typography.baseline.bodyLarge
                        .copyWith(
                          color: expressiveTheme.colorScheme.onSurfaceVariant,
                        ),
                  ),
                  const mui.SizedBox(height: 24),
                  M3EButton.icon(
                    onPressed: onChooseVault,
                    icon: const mui.Icon(mui.Icons.folder_open),
                    label: mui.Text(context.l10n.chooseVault),
                    semanticLabel: context.l10n.chooseVault,
                  ),
                ],
              ),
            ),
          ),
        ),
        mui.Align(
          alignment: mui.Alignment.bottomCenter,
          child: mui.SafeArea(
            minimum: const mui.EdgeInsets.fromLTRB(16, 8, 16, 16),
            child: M3EButton.icon(
              onPressed: onShowNoteExample,
              style: M3EButtonStyle.tonal,
              icon: const mui.Icon(mui.Icons.description_outlined),
              label: mui.Text(context.l10n.viewNoteFormatExample),
              semanticLabel: context.l10n.viewNoteFormatExample,
            ),
          ),
        ),
      ],
    ),
  );
}
