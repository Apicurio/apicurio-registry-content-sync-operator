
build-image:
	mvn clean package -Pbuild-image --no-transfer-progress -DtrimStackTrace=false
.PHONY: build-image

build-image-faster:
	mvn clean package -Pbuild-image --no-transfer-progress -DtrimStackTrace=false -DskipTests
.PHONY: build-image-faster

e2e-prepare-env:
	./e2e/scripts/prepare-env.sh
.PHONY: e2e-prepare-env

e2e-restart-deployment:
	kubectl get pod -n e2e-kube-sync
	kubectl delete pod -n e2e-kube-sync -l name=registry
	kubectl get pod -n e2e-kube-sync
	kubectl rollout status deployment/apicurio-registry -n e2e-kube-sync

e2e-run-tests:
	mvn clean verify -Pe2e -pl e2e -am --no-transfer-progress -DtrimStackTrace=false
.PHONY: e2e-run-tests