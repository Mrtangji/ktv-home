# Builds the Home KTV server image locally on Windows.
# 在 Windows 上本地构建 Home KTV 服务端镜像。
#
# The build context must stay at the repository root because backend/Dockerfile
# also copies h5/ and dist/tv-apk into the image.
# 构建上下文必须在仓库根目录，因为 backend/Dockerfile 还会复制 h5/ 和 dist/tv-apk。
#
# Examples / 示例:
#   .\scripts\build-image.ps1
#   .\scripts\build-image.ps1 -Tag v1.2.3
#   .\scripts\build-image.ps1 -Platform linux/amd64,linux/arm64 -Tag master -Push

param(
    [string]$Image,
    [string]$Tag,
    [string]$Platform,
    [switch]$Push
)

$ErrorActionPreference = 'Stop'

if (-not $Image)    { $Image    = if ($env:KTV_IMAGE)          { $env:KTV_IMAGE }          else { 'ghcr.io/mrtangji/ktv-home' } }
if (-not $Tag)      { $Tag      = if ($env:KTV_IMAGE_TAG)      { $env:KTV_IMAGE_TAG }      else { 'dev' } }
if (-not $Platform) { $Platform = $env:KTV_IMAGE_PLATFORM }
if (-not $Push)     { $Push     = ($env:KTV_IMAGE_PUSH -eq 'true') }

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'docker not found in PATH / 未找到 docker 命令'
}

$releaseVersion = if ($env:KTV_RELEASE_VERSION) { $env:KTV_RELEASE_VERSION } else { '0.1.0-dev' }
$releaseCode    = if ($env:KTV_RELEASE_VERSION_CODE) { $env:KTV_RELEASE_VERSION_CODE } else { '1' }

$ref = "$($Image):$Tag"

# Warn early: without signed APKs the image only ships the .gitkeep placeholder,
# so the TV client download page stays empty until a release injects them.
$apks = Get-ChildItem -Path (Join-Path $repoRoot 'dist/tv-apk') -Filter '*.apk' -ErrorAction SilentlyContinue
if (-not $apks) {
    Write-Warning 'dist/tv-apk has no APK; the image will not bundle a TV client. / dist/tv-apk 中没有 APK，镜像不会内置电视端安装包。'
}

$buildArgs = @(
    '--file', 'backend/Dockerfile',
    '--build-arg', "KTV_RELEASE_VERSION=$releaseVersion",
    '--build-arg', "KTV_RELEASE_VERSION_CODE=$releaseCode",
    '--tag', $ref
)

if ($Platform) {
    # A multi-platform result cannot be loaded into the local daemon, so
    # pushing is the only way to keep it.
    if ($Platform -like '*,*' -and -not $Push) {
        throw 'Multi-platform builds require -Push; the local daemon cannot load them.'
    }
    $mode = if ($Push) { '--push' } else { '--load' }
    & docker buildx build --platform $Platform $mode @buildArgs .
} else {
    & docker build @buildArgs .
    if ($Push) { & docker push $ref }
}

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ''
Write-Host "Image: $ref"
if (-not $Push) {
    Write-Host "Not pushed. Run with -Push, or: docker push $ref"
}
