/*
Copyright 2021.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package v1alpha1

import (
	RegistryApi "github.com/dweber019/apicurio-registry-artifact-operator/registry_api"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// ApicurioSpec defines the desired state of Apicurio
// +kubebuilder:printcolumn:name="Id",type=string,JSONPath=`.spec.id`
// +kubebuilder:printcolumn:name="Name",type=string,JSONPath=`.spec.name`
// +kubebuilder:printcolumn:name="Type",type=string,JSONPath=`.spec.type`
// +kubebuilder:printcolumn:name="Registry",type=string,JSONPath=`.spec.registryApiEndpoint`
type ApicurioSpec struct {
	// The artifact id which will be the reference point for all operations towards the Registry API
	// +kubebuilder:validation:Required
	Id string `json:"artifactId"`

	// The name of the artifact
	// +kubebuilder:validation:Required
	Name string `json:"name"`

	// The description of the artifact. This will always overwrite the content description
	// +kubebuilder:validation:Optional
	Description string `json:"description,omitempty"`

	// The type of the artifact
	// +kubebuilder:default=OPENAPI
	// +kubebuilder:validation:Required
	Type ArtifactType `json:"type"`

	// The user which owns the artifact
	// +kubebuilder:validation:Required
	CreatedBy string `json:"username"`

	// This are some arbitrary labels
	// +kubebuilder:validation:Optional
	Labels []string `json:"labels,omitempty"`

	// And some arbitrary properties
	// +kubebuilder:validation:Optional
	Properties map[string]string `json:"properties,omitempty"`

	// The state of the artifact not tied to any version
	// +kubebuilder:default=ENABLED
	// +kubebuilder:validation:Optional
	State ArtifactState `json:"state,omitempty"`

	// This will define the validity rule
	// +kubebuilder:default=FULL
	// +kubebuilder:validation:Required
	RuleValidity ArtifactRuleValidity `json:"ruleValidity,omitempty"`

	// This will define the compatibility rule
	// +kubebuilder:validation:Optional
	RuleCompatibility ArtifactRuleCompatibility `json:"ruleCompatibility,omitempty"`

	// This defines the content type of the API request, mostly json but not always
	// +kubebuilder:default=application/json
	// +kubebuilder:validation:Required
	ContentType string `json:"contentType,omitempty"`

	// The content of the artifact, for an openAPI would be json (content or externalContent is required)
	// +kubebuilder:validation:Optional
	Content string `json:"content"`

	// The content of the artifact, provide a valid URL to the content (content or externalContent is required)
	// +kubebuilder:validation:Optional
	ExternalContent string `json:"externalContent"`
}

// +kubebuilder:validation:Enum=AVRO;PROTOBUF;PROTOBUF_FD;JSON;KCONNECT;OPENAPI;ASYNCAPI;GRAPHQL;WSDL;XSD
type ArtifactType RegistryApi.ArtifactType

// +kubebuilder:validation:Enum=DELETED;DEPRECATED;DISABLED;ENABLED
type ArtifactState RegistryApi.ArtifactState

// +kubebuilder:validation:Enum=FULL;SYNTAX_ONLY;NONE
type ArtifactRuleValidity string

// +kubebuilder:validation:Enum=BACKWARD;BACKWARD_TRANSITIVE;FORWARD;FORWARD_TRANSITIVE;FULL;FULL_TRANSITIVE;NONE
type ArtifactRuleCompatibility string

// ApicurioStatus defines the observed state of Apicurio
type ApicurioStatus struct {
	// INSERT ADDITIONAL STATUS FIELD - define observed state of cluster
	// Important: Run "make" to regenerate code after modifying this file
}

// +kubebuilder:object:root=true
// +kubebuilder:subresource:status

// Apicurio is the Schema for the apicurios API
type Apicurio struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   ApicurioSpec   `json:"spec,omitempty"`
	Status ApicurioStatus `json:"status,omitempty"`
}

// +kubebuilder:object:root=true

// ApicurioList contains a list of Apicurio
type ApicurioList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []Apicurio `json:"items"`
}

func init() {
	SchemeBuilder.Register(&Apicurio{}, &ApicurioList{})
}
