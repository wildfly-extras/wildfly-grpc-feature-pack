#!/usr/bin/env bash
#
#  Copyright The WildFly Authors
#  SPDX-License-Identifier: Apache-2.0
#


# -------------------------------------------------------
#
# Creates a new release:
#   - prepare and tag the release using Maven Release Plugin
#   - update the working copy to the next snapshot version
#   - deploy the tagged release to Maven Central
#
# -------------------------------------------------------

set -Eeuo pipefail
trap cleanup SIGINT SIGTERM ERR EXIT

VERSION=0.0.1

# Change into the script's directory
# Using relative paths is safe!
script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd -P)
readonly script_dir
cd "${script_dir}"

usage() {
  cat <<EOF
USAGE:
    $(basename "${BASH_SOURCE[0]}") [FLAGS] <release-version> <next-version>

FLAGS:
    -h, --help          Prints help information
    -v, --version       Prints version information
    --no-color          Uses plain text output

ARGS:
    <release-version>   The release version (as semver)
    <next-snapshot>     The next snapshot version  (as semver)
EOF
  exit
}

cleanup() {
  trap - SIGINT SIGTERM ERR EXIT
}

setup_colors() {
  if [[ -t 2 ]] && [[ -z "${NO_COLOR-}" ]] && [[ "${TERM-}" != "dumb" ]]; then
    NOFORMAT='\033[0m' RED='\033[0;31m' GREEN='\033[0;32m' ORANGE='\033[0;33m' BLUE='\033[0;34m' PURPLE='\033[0;35m' CYAN='\033[0;36m' YELLOW='\033[1;33m'
  else
    # shellcheck disable=SC2034
    NOFORMAT='' RED='' GREEN='' ORANGE='' BLUE='' PURPLE='' CYAN='' YELLOW=''
  fi
}

msg() {
  echo >&2 -e "${1-}"
}

die() {
  local msg=$1
  local code=${2-1} # default exit status 1
  msg "$msg"
  exit "$code"
}

version() {
  msg "${BASH_SOURCE[0]} $VERSION"
  exit 0
}

parse_params() {
  while :; do
    case "${1-}" in
    -h | --help) usage ;;
    -v | --version) version ;;
    --no-color) NO_COLOR=1 ;;
    -?*) die "Unknown option: $1" ;;
    *) break ;;
    esac
    shift
  done

  ARGS=("$@")
  [[ ${#ARGS[@]} -eq 2 ]] || die "Missing release and/or snapshot version"
  RELEASE_VERSION=${ARGS[0]}
  NEXT_VERSION=${ARGS[1]}
  return 0
}

is_semver() {
    local version
    version="$1"
    if [[ ! ${version} =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        return 1
    fi
}

version_gt() { test "$(echo "$@" | tr " " "\n" | sort -V | head -n 1)" != "$1"; }

parse_params "$@"
setup_colors

FINAL_VERSION="${RELEASE_VERSION}.Final"
SNAPSHOT_VERSION="${NEXT_VERSION}-SNAPSHOT"
TAG="v${RELEASE_VERSION}"

is_semver "${RELEASE_VERSION}" || die "Release version is not a semantic version"
is_semver "${NEXT_VERSION}" || die "Next version is not a semantic version"
version_gt "${NEXT_VERSION}" "${RELEASE_VERSION}" || die "Next version must be greater than release version"
git diff-index --quiet HEAD || die "You have uncommitted changes"
[[ $(git tag -l "${TAG}") ]] && die "Tag ${TAG} already defined"

msg ""
msg "Codebase is ready to be released."
msg ""
msg "If you decide to continue, this script will "
msg ""
msg "   1. Prepare and push release ${CYAN}${FINAL_VERSION}${NOFORMAT} with tag ${CYAN}${TAG}${NOFORMAT}"
msg "   2. Commit and push the next snapshot version ${CYAN}${SNAPSHOT_VERSION}${NOFORMAT}"
msg "   3. Deploy the tagged release to ${CYAN}Maven Central${NOFORMAT}"
msg ""
echo "Do you wish to continue?"
select yn in "Yes" "No"; do
    case $yn in
        Yes ) break;;
        No ) die "Aborted ";;
    esac
done

msg ""
mvn --batch-mode release:clean release:prepare \
    -DreleaseVersion="${FINAL_VERSION}" \
    -DdevelopmentVersion="${SNAPSHOT_VERSION}" \
    -Dtag="${TAG}" \
    -Dresume=false

mvn --batch-mode release:perform

msg "Done. Monitor validation and publication at ${CYAN}https://central.sonatype.com/publishing/deployments${NOFORMAT}."
