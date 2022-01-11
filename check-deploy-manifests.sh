#!/bin/bash

if grep -q localhost:5000 deploy/standalone/03-deployment.yaml; then
    echo "Standalone manifests are not the expected ones, contains reference to test registry"
    exit 1
fi

diff dist/kubernetes/standalone/target/kubernetes/manifests/ deploy/standalone/
ret=$?

if [[ $ret -eq 0 ]]; then
    echo "Standalone manifests ok."
else
    echo "Standalone manifests are not the expected ones, are not updated"
    exit 1
fi

if grep -q localhost:5000 deploy/simple/03-deployment-simple.yaml; then
    echo "Simple manifests are not the expected ones, contains reference to test registry"
    exit 1
fi

diff dist/kubernetes/simple/target/kubernetes/manifests/ deploy/simple/
ret=$?
if [[ $ret -eq 0 ]]; then
    echo "Simple manifests ok."
else
    echo "Simple manifests are not the expected ones, are not updated"
    exit 1
fi

# all in one file

if grep -q localhost:5000 deploy/simple.yaml; then
    echo "Simple manifests are not the expected ones, contains reference to test registry"
    exit 1
fi

rm -rf deploy/check-simple.yaml
for file in dist/kubernetes/simple/target/kubernetes/manifests/*.yaml; do
    echo "---" | tee -a deploy/check-simple.yaml
    cat $file | tee -a deploy/check-simple.yaml
done

diff deploy/check-simple.yaml deploy/simple.yaml
ret=$?
if [[ $ret -eq 0 ]]; then
    echo "Simple one-file manifests ok."
else
    echo "Simple one-file manifests are not the expected ones, are not updated"
    exit 1
fi