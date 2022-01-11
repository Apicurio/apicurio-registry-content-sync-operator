# apicurio-registry-content-sync-operator

A Kubernetes Operator that allows to manage the lifecycle of Artifacts in [Apicurio Registry](https://www.apicur.io/registry/).

```yaml
apiVersion: artifact.apicur.io/v1alpha1
kind: Artifact
metadata:
  name: avro-example
spec:
  artifactId: pests-record
  name: pets
  description: "Avro record for Pet entity"
  type: AVRO
  labels:
    - avro
    - kafka
  content: |
    {"namespace": "example.avro",
     "type": "record",
     "name": "Pet",
     "fields": [
         {"name": "name", "type": "string"},
         {"name": "holderId",  "type": ["int", "null"]},
         {"name": "type", "type": ["string", "null"]}
     ]
    }
```
You can find more artifact examples in [`dist/kubernetes/examples`](./dist/kubernetes/examples)

This project provides a Kubernetes Custom Resource Definition that represents an Artifact Version in Apicurio Registry. The declaration can be found in the `api` module.

Currently this project only consists of a controller responsible of the synchronization of an Apicurio Registry instance against `Artifact` CRDs in Kubernetes.
The controller ensures the `Artifacts` exist in the target Apicurio Registry instance. The controller code can be found in the `sync` module.

# How to deploy this?
You can find two examples of how to deploy this operator in the `deploy` folder.

The manifests in the `simple` folder are an example of a Kubernetes-only deployment of Apicurio Registry. This deploys the in-memory storage variant of Apicurio Registry (`quay.io/apicurio/apicurio-registry-mem`) and also deploys the content-sync controller as a sidecar to the registry. This way we can create artifacts in the registry through Kubernetes CRDs and the long term storage for the registry is Kubernetes.

You can deploy the `simple` installation like this:

```
kubectl apply -f deploy/simple
```

The manifests in the `standalone` folder are an example of a deployment of this operator in isolation. With this deployment method it's assumed the registry is already deployed. The target Apicurio Registry instance is configured with the env var `APICURIO_REGISTRY_URL` located in the `03-deployment.yaml` file.

The `simple` manifests folder is the only one that works out of the box, the `standalone` one you may have to tweak it to point to your own Apicurio Registry.