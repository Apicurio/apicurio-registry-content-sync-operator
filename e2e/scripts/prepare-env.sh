#!/bin/bash

# NAMESPACE=apicurio-registry-operator-namespace
NAMESPACE=e2e-kube-sync

kubectl create namespace $NAMESPACE

# kubectl apply -f https://github.com/Apicurio/apicurio-registry-operator/raw/master/docs/resources/install.yaml

# kubectl wait --for=condition=Ready deployment/apicurio-registry-operator -n $NAMESPACE

# kubectl apply -f https://raw.githubusercontent.com/Apicurio/apicurio-registry-operator/master/config/examples/resources/apicurioregistry_mem_cr.yaml -n $NAMESPACE

# kubectl wait --for=condition=Ready deployment/example-apicurioregistry-mem -n $NAMESPACE

kubectl apply -f deploy/simple -n $NAMESPACE

echo "Waiting for deployment to be ready"

# kubectl wait --for=condition=Ready deployment/apicurio-registry -n $NAMESPACE
kubectl rollout status deployment/apicurio-registry -n $NAMESPACE

kubectl get ingress apicurio-registry-ingress -n $NAMESPACE -o yaml
