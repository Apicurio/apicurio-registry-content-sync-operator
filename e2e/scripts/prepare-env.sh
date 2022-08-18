#!/bin/bash
set -x

IMAGE=quay.io/apicurio/apicurio-registry-kube-sync:latest-snapshot
TEST_IMAGE=localhost:5000/apicurio/apicurio-registry-kube-sync:latest-snapshot

docker tag $IMAGE $TEST_IMAGE
docker push $TEST_IMAGE

TEST_MANIEFSTS_DIR=dist/kubernetes/simple/target/kubernetes/manifests
sed -i -e "s,$IMAGE,$TEST_IMAGE,g" $TEST_MANIEFSTS_DIR/03-deployment-simple.yaml

NAMESPACE=e2e-kube-sync

kubectl create namespace $NAMESPACE | true

kubectl apply -f $TEST_MANIEFSTS_DIR -n $NAMESPACE

echo "Waiting for deployment to be ready"

kubectl rollout status deployment/apicurio-registry -n $NAMESPACE

kubectl get ingress apicurio-registry-ingress -n $NAMESPACE -o yaml
