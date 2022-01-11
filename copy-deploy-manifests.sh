#!/bin/bash

cp dist/kubernetes/standalone/target/kubernetes/manifests/*.yaml deploy/standalone/
cp dist/kubernetes/simple/target/kubernetes/manifests/*.yaml deploy/simple

rm deploy/simple.yaml
for file in dist/kubernetes/simple/target/kubernetes/manifests/*.yaml; do
    echo "---" | tee -a deploy/simple.yaml
    cat $file | tee -a deploy/simple.yaml
done