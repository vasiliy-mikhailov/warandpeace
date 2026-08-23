#!/bin/sh
# THE ONE DEPENDENCY THAT IS NOT ON MAVEN CENTRAL, INSTALLED AT THE VERSION THIS REPOSITORY PINS.
#
# ratchet ships a source tree and tags rather than deploying anywhere, which is its choice and not
# an oversight — which artifact repository a build resolves from belongs to that build. So this is
# the one place that knows both halves: which version this repository wants, and where to put it.
#
#   ./install-ratchet.sh                    into ~/.m2
#   ./install-ratchet.sh -r ~/.m2-fitness   into a repository another build reads
set -eu

repo=""
[ "${1:-}" = "-r" ] && { repo="${2:?-r wants a directory}"; }

cd "$(dirname "$0")"

# THE PIN IS READ FROM THE POM, not written here as well. Two copies of one version number is how a
# build ends up installing something it does not depend on and reporting success.
version=$(sed -n 's|.*<ratchet.version>\(.*\)</ratchet.version>.*|\1|p' pom.xml | head -1)
[ -n "$version" ] || { echo "install-ratchet.sh: pom.xml declares no ratchet.version" >&2; exit 1; }

src="${RATCHET_SRC:-$HOME/.cache/ratchet-src}"
if [ -d "$src/.git" ]; then
    git -C "$src" fetch -q --tags
else
    mkdir -p "$(dirname "$src")"
    git clone -q https://github.com/vasiliy-mikhailov/ratchet.git "$src"
fi

echo "warandpeace: this repository pins ratchet $version"
"$src/install.sh" "v$version" ${repo:+-r "$repo"}
