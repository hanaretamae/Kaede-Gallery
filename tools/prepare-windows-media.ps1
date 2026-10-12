param(
  [Parameter(Mandatory = $true)]
  [string]$Bundle,
  [Parameter(Mandatory = $true)]
  [ValidateSet('x64', 'arm64')]
  [string]$Arch
)

$ErrorActionPreference = 'Stop'
$media = switch ($Arch) {
  'x64' {
    @{
      Name = 'x86_64'
      Mpv = '769a9bb120b35aad20c25ae502c44ed8f59b4e607fcbc7f6cdde61f98dceabaf'
      Ffmpeg = '417681c7e000d2fb3d25d3403bd02d542a34fdb0d35a708f22b98750668b94c5'
    }
  }
  'arm64' {
    @{
      Name = 'aarch64'
      Mpv = 'b40be5bfc5ac5ac2f7086f1a1c3ec26e9fccebb1f6f20c3be6c9a56de7e33826'
      Ffmpeg = 'fd3e75c5775443dcc6a296757a0ba26557ad3b62e63d280e84f3a47a2d97b11b'
    }
  }
}
$sevenZip = (Get-Command 7z -ErrorAction SilentlyContinue).Source
if (-not $sevenZip) { $sevenZip = 'C:\Program Files\7-Zip\7z.exe' }
if (-not (Test-Path -LiteralPath $sevenZip -PathType Leaf)) {
  throw '7-Zip is required to unpack mpv and FFmpeg.'
}
$bundlePath = (Resolve-Path -LiteralPath $Bundle).Path
$tools = Join-Path $bundlePath 'tools'
$licenses = Join-Path $bundlePath 'licenses'
New-Item -ItemType Directory -Force -Path $tools, $licenses | Out-Null
$base = 'https://github.com/zhongfly/mpv-winbuild/releases/download/2026-10-11-88eb9f2e73'
$downloads = @(
  @{ Name = "mpv-$($media.Name)-20261011-git-88eb9f2e73.7z"; Sha = $media.Mpv; Exe = 'mpv.exe' },
  @{ Name = "ffmpeg-$($media.Name)-git-b5902e69c.7z"; Sha = $media.Ffmpeg; Exe = 'ffmpeg.exe' }
)
foreach ($item in $downloads) {
  $archive = Join-Path $env:RUNNER_TEMP $item.Name
  Invoke-WebRequest -Uri "$base/$($item.Name)" -OutFile $archive
  $actual = (Get-FileHash -Algorithm SHA256 -LiteralPath $archive).Hash.ToLowerInvariant()
  if ($actual -ne $item.Sha) { throw "Checksum mismatch for $($item.Name)" }
  & $sevenZip x -y "-o$tools" $archive $item.Exe | Out-Null
  if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath (Join-Path $tools $item.Exe))) {
    throw "Could not unpack $($item.Name)"
  }
}

$source = Join-Path $PSScriptRoot '../kotlin/shared-assets/licenses'
Copy-Item -Path (Join-Path $source '*') -Destination $licenses -Recurse -Force
foreach ($name in 'Apache-2.0.txt', 'LGPL-2.1.txt', 'GPL-3.0.txt',
                  'RUST-DEPENDENCY-LICENSES.txt') {
  if (-not (Test-Path -LiteralPath (Join-Path $licenses $name) -PathType Leaf)) {
    throw "License notice $name is missing from the Windows distribution."
  }
}
