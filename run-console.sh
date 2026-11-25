#!/bin/sh
cd "$(dirname "$0")"
./compile.sh && java -cp out Main
