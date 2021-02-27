# The Apicurio registry artifact operator
## Why
This operator aims to provide Kubernetes CRD's to manage the lifecycle of artifacts with meta data and versions.  
This will enable DevOps teams to manage all artifacts provided by the Apicurio Registry with GitOps and can build process based on it.

# What
There for this operator implemented with operator-sdk and golang is implemented. It will keep track on a CRD and keep the Apicurio Registry in-sync
with the defined CRD's in Kubernetes.

## How
The operator will use the API provided by the Apicurio found at [https://www.apicur.io/registry/docs](https://www.apicur.io/registry/docs).  
This implementation takes the openAPI definition of the Apicurio Registry API and converts the definition into a golang client SDK.  
On `reconciliation` the client SDK will be used to represent the CRD defined state in the Registry.

## Behavior
### Versioning
The version of the Apicurio registry don't correspond to the version in the content (e.g. OpenAPI version).  
Apicurio will look at the content and decides by itself.  
The API is configured with [RETURN_OR_UPDATE](https://www.apicur.io/registry/docs/apicurio-registry/1.3.3.Final/assets-attachments/registry-rest-api.htm#operation/createArtifact).

> If you like to have control over the version yourself, it's suggested you create a artifact for each version.

### Deletion
If you delete a CRD the operator will not delete the artifact as it's possible that other parties rely on your artifact.  
If you like physically delete the artifact you can do so by setting the metadata annotation `apicurio.artifact.operator/force-delete: "true"`.

## Development
For development you should use `minikube` or any other possible kubernetes compatible implementation, as advised by the operator-sdk framework.  
Second you have to define the Apicurio registry endpoint with a environment variable called `export APICURIO_ENDPOINT=http://192.168.99.102:30199/api`.

### Update the Apicurio Registry client SDK
In install the [oapi-codegen](https://github.com/deepmap/oapi-codegen). For example by running ´go get github.com/deepmap/oapi-codegen/cmd/oapi-codegen´.  
Next make sure you updated the ´registry_api/openapi.yaml´ and run
```
oapi-codegen --config=./registry_api/config.yaml ./registry_api/openapi.yaml
```
> You have to remove all `format: date-time` and change to `type: integer` as of [issue 1215](https://github.com/Apicurio/apicurio-registry/issues/1215)

### Update the CRD
To update the CRD use the following command
```
make generate
make manifests
```

### Build and run
To install the CRD to the Kubernetes and run the operator outside of Kubernetes use
```
make install run
```

### Apicurio Registry (dev)
You can find a simple in-memory deployment for the Apicurio Registry in ´config/samples/apicurio_registry.yaml´.  
To access the UI you can open a tunnel to the service. In minikube you do so with ´minikube service apicurio-registry --url´.  
> Just make sure your using virtualbox on macOS, else you can't expose node ports

Don't use this in production!!! Use something like the [Registry operator](https://operatorhub.io/operator/apicurio-registry) instead.

## Deployment
To update the docker image use this command
```
make docker-build docker-push IMG=docker.io/dweber019/apicurio-registry-artifact-operator:v0.0.1
```
After this you can run with
```
make deploy IMG=docker.io/dweber019/apicurio-registry-artifact-operator:v0.0.1
```
Don't forget to set env `APICURIO_ENDPOINT`.

## Useful links
- [Kubebilder](https://book.kubebuilder.io)
- [Operator tutorial](https://sdk.operatorframework.io/docs/building-operators/golang/tutorial/)
- [Apicurio API reference](https://www.apicur.io/registry/docs/apicurio-registry/1.3.3.Final/assets-attachments/registry-rest-api.htm)
- [Openapi generator for golang](https://github.com/deepmap/oapi-codegen)
