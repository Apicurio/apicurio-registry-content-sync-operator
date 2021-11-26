#!/bin/bash

cp dist/kubernetes/standalone/target/kubernetes/manifests/*.yaml deploy/standalone/
cp dist/kubernetes/simple/target/kubernetes/manifests/*.yaml deploy/simple