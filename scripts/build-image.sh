#!/usr/bin/env bash
# Build the Home KTV server image locally.
# 本地构建 Home KTV 服务端镜像。
#
# The build context must stay at the repository root because backend/Dockerfile
# also copies h5/ and dist/tv-apk into the image.
# 构建上下文必须在仓库根目录，因为 backend/Dockerfile 还会复制 h5/ 和 dist/tv-apk。
#
# Examples / 示例:
#   ./scripts/build-image.sh
#   ./scripts/build-image.sh --tag v1.2.3
#   ./scripts/build-image.sh --platform linux/amd64,linux/arm64 --tag master --push
set -euo pipefail

IMAGE="${KTV_IMAGE:-ghcr.io/mrtangji/ktv-home}"
TAG="${KTV_IMAGE_TAG:-dev}"
PLATFORM="${KTV_IMAGE_PLATFORM:-}"
PUSH="${KTV_IMAGE_PUSH:-false}"

usage() {
  cat <<'EOF'
Usage: scripts/build-image.sh [options]

Options:
  -i, --image IMAGE      Image repository (default: ghcr.io/mrtangji/ktv-home)
  -t, --tag TAG          Image tag (default: dev)
  -p, --platform LIST    Comma-separated platforms, e.g. linux/amd64,linux/arm64
                         Omit to build only for the current machine (default)
      --push             Push the image after a successful build
  -h, --help             Show this help

Environment overrides: KTV_IMAGE, KTV_IMAGE_TAG, KTV_IMAGE_PLATFORM, KTV_IMAGE_PUSH
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    -i|--image)    IMAGE="$2"; shift 2 ;;
    -t|--tag)      TAG="$2"; shift 2 ;;
    -p|--platform) PLATFORM="$2"; shift 2 ;;
    --push)        PUSH=true; shift ;;
    -h|--help)     usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 1 ;;
  esac
done

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

command -v docker >/dev/null 2>&1 || { echo "docker not found in PATH" >&2; exit 1; }

build_args=(
  --file backend/Dockerfile
  --build-arg "KTV_RELEASE_VERSION=${KTV_RELEASE_VERSION:-0.1.0-dev}"
  --build-arg "KTV_RELEASE_VERSION_CODE=${KTV_RELEASE_VERSION_CODE:-1}"
  --tag "${IMAGE}:${TAG}"
)

# Warn early: without signed APKs the image only ships the .gitkeep placeholder,
# so the TV client download page stays empty until a release injects them.
if [ -z "$(find dist/tv-apk -name '*.apk' -print -quit 2>/dev/null)" ]; then
  echo "Note: dist/tv-apk has no APK; the image will not bundle a TV client." >&2
  echo "提示：dist/tv-apk 中没有 APK，镜像不会内置电视端安装包。" >&2
fi

if [ -n "$PLATFORM" ]; then
  docker buildx version >/dev/null 2>&1 || {
    echo "Docker Buildx is required for --platform" >&2
    exit 1
  }
  # A multi-platform result cannot be loaded into the local daemon, so pushing
  # is the only way to keep it.
  if [[ "$PLATFORM" == *,* ]] && [ "$PUSH" != "true" ]; then
    echo "Multi-platform builds require --push; the local daemon cannot load them." >&2
    exit 1
  fi
  if [ "$PUSH" = "true" ]; then
    docker buildx build --platform "$PLATFORM" --push "${build_args[@]}" .
  else
    docker buildx build --platform "$PLATFORM" --load "${build_args[@]}" .
  fi
else
  docker build "${build_args[@]}" .
  if [ "$PUSH" = "true" ]; then
    docker push "${IMAGE}:${TAG}"
  fi
fi

echo
echo "Image: ${IMAGE}:${TAG}${PLATFORM:+ (platforms: $PLATFORM)}"
if [ "$PUSH" != "true" ]; then
  echo "Not pushed. Run with --push, or: docker push ${IMAGE}:${TAG}"
fi
