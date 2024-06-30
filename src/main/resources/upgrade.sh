#!/bin/bash
cd "$(dirname "$0")/.."
rm -f plugins/packlodge-system.jar
mv plugins/packlodge-system-*.jar plugins/packlodge-system.jar

