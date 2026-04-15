#!/bin/sh

set -e

BASEDIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec mvn -f "$BASEDIR/pom.xml" "$@"
