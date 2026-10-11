package com.hanaretamae.kaede.desktop

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.TimeUnit

private const val FOLDER_PICKER_EXIT_CANCELLED = 1
private const val FOLDER_PICKER_TIMEOUT_MINUTES = 30L

// Uses the Windows Vista+ IFileOpenDialog (the modern Explorer-style picker) through PowerShell.
private val FOLDER_PICKER_SCRIPT = """
${'$'}ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class KaedeFolderPicker {
  [ComImport, Guid("DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7")] class FileOpenDialogCom {}
  [ComImport, Guid("43826d1e-e718-42ee-bc55-a1e261c37bfe"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
  interface IShellItem {
    void BindToHandler(IntPtr pbc, ref Guid bhid, ref Guid riid, out IntPtr ppv);
    void GetParent(out IShellItem psi);
    void GetDisplayName(uint sigdnName, [MarshalAs(UnmanagedType.LPWStr)] out string name);
  }
  [ComImport, Guid("42f85136-db7e-439c-85f1-e4075d135fc8"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
  interface IFileDialog {
    [PreserveSig] int Show(IntPtr parent);
    void SetFileTypes(uint c, IntPtr t);
    void SetFileTypeIndex(uint i);
    void GetFileTypeIndex(out uint i);
    void Advise(IntPtr e, out uint c);
    void Unadvise(uint c);
    void SetOptions(uint o);
    void GetOptions(out uint o);
    void SetDefaultFolder(IShellItem s);
    void SetFolder(IShellItem s);
    void GetFolder(out IShellItem s);
    void GetCurrentSelection(out IShellItem s);
    void SetFileName([MarshalAs(UnmanagedType.LPWStr)] string n);
    void GetFileName([MarshalAs(UnmanagedType.LPWStr)] out string n);
    void SetTitle([MarshalAs(UnmanagedType.LPWStr)] string t);
    void SetOkButtonLabel([MarshalAs(UnmanagedType.LPWStr)] string t);
    void SetFileNameLabel([MarshalAs(UnmanagedType.LPWStr)] string t);
    void GetResult(out IShellItem s);
  }
  public static string Pick(string title) {
    IFileDialog dialog = (IFileDialog)new FileOpenDialogCom();
    uint options;
    dialog.GetOptions(out options);
    dialog.SetOptions(options | 0x20 | 0x40);
    dialog.SetTitle(title);
    int result = dialog.Show(IntPtr.Zero);
    if (result == unchecked((int)0x800704C7)) return null;
    if (result != 0) Marshal.ThrowExceptionForHR(result);
    IShellItem item;
    dialog.GetResult(out item);
    string path;
    item.GetDisplayName(0x80058000, out path);
    return path;
  }
}
'@
${'$'}selected = [KaedeFolderPicker]::Pick(${'$'}env:KAEDE_PICKER_TITLE)
if (${'$'}null -eq ${'$'}selected) { exit $FOLDER_PICKER_EXIT_CANCELLED }
[Console]::Out.Write(${'$'}selected)
""".trimIndent()

internal fun windowsFolderPickerCommand(): List<String> = listOf(
    "powershell.exe",
    "-NoProfile",
    "-NonInteractive",
    "-STA",
    "-EncodedCommand",
    Base64.getEncoder().encodeToString(FOLDER_PICKER_SCRIPT.toByteArray(StandardCharsets.UTF_16LE)),
)

/** Returns the chosen directory, or null when cancelled. Throws IOException if the picker is unavailable. */
internal fun chooseWindowsDirectory(title: String): String? {
    val builder = ProcessBuilder(windowsFolderPickerCommand())
        .redirectError(ProcessBuilder.Redirect.DISCARD)
    builder.environment()["KAEDE_PICKER_TITLE"] = title
    val process = try {
        builder.start()
    } catch (failure: SecurityException) {
        throw IOException("System folder picker is unavailable.", failure)
    }
    process.outputStream.close()
    val output = process.inputStream.use { it.readNBytes(32 * 1024) }
    if (!process.waitFor(FOLDER_PICKER_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
        process.destroyForcibly()
        throw IOException("System folder picker timed out.")
    }
    return when (process.exitValue()) {
        0 -> String(output, StandardCharsets.UTF_8).trim().takeIf {
            it.isNotEmpty() && runCatching { Path.of(it).isAbsolute }.getOrDefault(false)
        } ?: throw IOException("System folder picker returned an invalid path.")
        FOLDER_PICKER_EXIT_CANCELLED -> null
        else -> throw IOException("System folder picker failed.")
    }
}
