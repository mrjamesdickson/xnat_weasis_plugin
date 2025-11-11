#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_JAVA_HOME="/usr/local/opt/openjdk@8"
JAVA_HOME="${JAVA_HOME:-$DEFAULT_JAVA_HOME}"

if [[ ! -x "${JAVA_HOME}/bin/java" ]]; then
  echo "Error: expected Java at ${JAVA_HOME}/bin/java. Set JAVA_HOME to a valid JDK before running." >&2
  exit 1
fi

export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${PATH}"

GRADLE_CMD="./gradlew"
if [[ ! -x "${ROOT_DIR}/gradlew" ]]; then
  if command -v gradle >/dev/null 2>&1; then
    GRADLE_CMD="gradle"
  else
    echo "Error: gradlew not found and \"gradle\" is not on PATH." >&2
    exit 1
  fi
fi

if [[ $# -gt 0 ]]; then
  GRADLE_ARGS=("$@")
else
  GRADLE_ARGS=(clean build)
fi

echo "[build.sh] JAVA_HOME=${JAVA_HOME}"
echo "[build.sh] Running ${GRADLE_CMD} ${GRADLE_ARGS[*]}"

if [[ "${GRADLE_CMD}" == "./gradlew" ]]; then
  (cd "${ROOT_DIR}" && "${GRADLE_CMD}" "${GRADLE_ARGS[@]}")
else
  (cd "${ROOT_DIR}" && "${GRADLE_CMD}" "${GRADLE_ARGS[@]}")
fi

shopt -s nullglob
artifacts=("${ROOT_DIR}"/build/libs/*.jar "${ROOT_DIR}"/build/libs/*.war)
shopt -u nullglob
SELECTED_ARTIFACT=""
for artifact in "${artifacts[@]}"; do
  [[ -e "$artifact" ]] || continue
  case "$artifact" in
    *-plain.jar) continue ;;
  esac
  SELECTED_ARTIFACT="$artifact"
  break
done

if [[ -n "${SELECTED_ARTIFACT}" ]]; then
  SIZE=$(ls -lh "${SELECTED_ARTIFACT}" | awk '{print $5}')
  REL_PATH="${SELECTED_ARTIFACT#${ROOT_DIR}/}"
  echo "[build.sh] Artifact created: $REL_PATH ($SIZE)"
else
  echo "[build.sh] No packaged artifact found under build/libs" >&2
fi
