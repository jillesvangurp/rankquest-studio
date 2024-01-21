#!/usr/bin/env bash

set -e

die () {
    echo >&2 "$@"
    exit 1
}

[[ -z $(git status -s) ]] || die "git status is not clean"

gradle jsBrowserProductionWebpack

echo "publishing $TAG"

rsync -azpv --exclude maven* --exclude bmath --delete-after  build/kotlin-webpack/js/productionExecutable/* jillesvangurpcom@ftp.jillesvangurp.com:/srv/home/jillesvangurpcom/domains/jillesvangurp.com/htdocs/rankquest