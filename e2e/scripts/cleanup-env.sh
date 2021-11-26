#!/bin/bash

NAMESPACE=e2e-kube-sync

kubectl delete -f deploy/simple -n $NAMESPACE

kubectl delete namespace $NAMESPACE