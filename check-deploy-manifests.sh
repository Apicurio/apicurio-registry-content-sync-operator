#!/bin/bash

diff dist/kubernetes/standalone/target/kubernetes/manifests/ deploy/standalone/
ret=$?

if [[ $ret -eq 0 ]]; then
    echo "Standalone manifests ok."
else
    echo "Standalone manifests are not the expected ones."
    exit 1
fi

diff dist/kubernetes/simple/target/kubernetes/manifests/ deploy/simple/
ret=$?
if [[ $ret -eq 0 ]]; then
    echo "Simple manifests ok."
else
    echo "Simple manifests are not the expected ones."
    exit 1
fi