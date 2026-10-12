param(
  [Parameter(Mandatory = $true)]
  [string]$Bundle
)

$ErrorActionPreference = 'Stop'
$bundlePath = (Resolve-Path -LiteralPath $Bundle).Path
$sourceRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$launcher = Join-Path $bundlePath 'desktopApp.exe'
$configuration = Join-Path $bundlePath 'app/desktopApp.cfg'
if (-not (Test-Path -LiteralPath $launcher -PathType Leaf) -or
    -not (Test-Path -LiteralPath $configuration -PathType Leaf)) {
  throw 'Compose Desktop launcher or configuration is missing from the Windows distribution.'
}

# The launcher looks for a configuration file with the same base name.
Rename-Item -LiteralPath $launcher -NewName 'Kaede-Gallery.exe'
Rename-Item -LiteralPath $configuration -NewName 'Kaede-Gallery.cfg'
foreach ($name in @('LICENSE', 'LICENSE.ja.md', 'README.md', 'README.ja.md',
                    'THIRD_PARTY_NOTICES.md', 'THIRD_PARTY_NOTICES.ja.md')) {
  Copy-Item -LiteralPath (Join-Path $sourceRoot $name) -Destination $bundlePath
}
