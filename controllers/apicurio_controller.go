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

package controllers

import (
	"context"
	RegistryApi "github.com/dweber019/apicurio-registry-artifact-operator/registry_api"
	"io/ioutil"
	"k8s.io/apimachinery/pkg/api/errors"
	"net/http"
	"strings"

	"github.com/go-logr/logr"
	"k8s.io/apimachinery/pkg/runtime"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"

	artifactv1alpha1 "github.com/dweber019/apicurio-registry-artifact-operator/api/v1alpha1"
)

// ApicurioReconciler reconciles a Apicurio object
type ApicurioReconciler struct {
	client.Client
	Log            logr.Logger
	Scheme         *runtime.Scheme
	ApicurioClient RegistryApi.ClientWithResponses
}

// +kubebuilder:rbac:groups=artifact.w3tec.ch,resources=apicurios,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=artifact.w3tec.ch,resources=apicurios/status,verbs=get;update;patch
// +kubebuilder:rbac:groups=artifact.w3tec.ch,resources=apicurios/finalizers,verbs=update

// Reconcile is part of the main kubernetes reconciliation loop which aims to
// move the current state of the cluster closer to the desired state.
// TODO(user): Modify the Reconcile function to compare the state specified by
// the Apicurio object against the actual cluster state, and then
// perform operations to make the cluster state reflect the state specified by
// the user.
//
// For more details, check Reconcile and its Result here:
// - https://pkg.go.dev/sigs.k8s.io/controller-runtime@v0.7.0/pkg/reconcile
func (r *ApicurioReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	log := r.Log.WithValues("apicurio", req.NamespacedName)
	registryApiClient := r.ApicurioClient

	apicurioArtifact := &artifactv1alpha1.Apicurio{}
	err := r.Get(ctx, req.NamespacedName, apicurioArtifact)
	if err != nil {
		if errors.IsNotFound(err) {
			log.Info("Artifact resource not found. Ignoring since object must be deleted.")
			return ctrl.Result{}, nil
		}
		log.Error(err, "Failed to get artifact.")
		return ctrl.Result{}, err
	}

	log = log.WithValues("artifactId", apicurioArtifact.Spec.Id, "type", apicurioArtifact.Spec.Type)

	// Content validation
	if apicurioArtifact.Spec.Content == "" && apicurioArtifact.Spec.ExternalContent == "" {
		log.Error(&ValidationContentError{}, "Artifact has content or externalContent not defined")
		return ctrl.Result{}, &ValidationContentError{}
	}

	value, exists := apicurioArtifact.Annotations["apicurio.artifact.operator/force-delete"]
	if exists && value == "true" {
		log.Info("Artifact will be deleted as it's forced.")
		_, err := registryApiClient.DeleteArtifact(ctx, apicurioArtifact.Spec.Id)
		if err != nil {
			log.Error(err, "Artifact deleted in registry failed.")
			return ctrl.Result{}, err
		}
		log.Info("Artifact deleted in registry.")
		return ctrl.Result{}, nil
	}

	// Create artifact with RETURN_OR_UPDATE
	var ifExists = "RETURN_OR_UPDATE"
	var artifactType = string(apicurioArtifact.Spec.Type)

	var content = apicurioArtifact.Spec.Content
	if content == "" {
		resp, err := http.Get(apicurioArtifact.Spec.ExternalContent)
		if err != nil {
			log.Error(err, "Could not load content ", "content", apicurioArtifact.Spec.ExternalContent)
			return ctrl.Result{}, err
		}
		defer resp.Body.Close()
		err = WrapRegistryApiIssues(err, resp)
		if err != nil {
			log.Error(err, "Could not load content", "content", apicurioArtifact.Spec.ExternalContent)
			return ctrl.Result{}, err
		}
		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			log.Error(err, "Could not convert response body for", apicurioArtifact.Spec.ExternalContent)
			return ctrl.Result{}, err
		}
		content = string(body)
	}

	responseArtifact, err := registryApiClient.CreateArtifactWithBodyWithResponse(ctx, &RegistryApi.CreateArtifactParams{
		IfExists:              &ifExists,
		XRegistryArtifactType: &artifactType,
		XRegistryArtifactId:   &apicurioArtifact.Spec.Id,
	}, apicurioArtifact.Spec.ContentType, strings.NewReader(content))
	err = WrapRegistryApiIssues(err, responseArtifact.HTTPResponse)
	if err != nil {
		log.Error(err, "Could create or update artifact")
		return ctrl.Result{}, err
	}
	log.Info("Artifact created or updated.", "version", responseArtifact.JSON200.Version)

	// Update metadata
	responseMetadata, err := registryApiClient.UpdateArtifactVersionMetaDataWithResponse(ctx, responseArtifact.JSON200.Id, int(responseArtifact.JSON200.Version), RegistryApi.UpdateArtifactVersionMetaDataJSONRequestBody{
		Description: &apicurioArtifact.Spec.Description,
		Labels:      &apicurioArtifact.Spec.Labels,
		Name:        &apicurioArtifact.Spec.Name,
		Properties: &RegistryApi.Properties{
			AdditionalProperties: apicurioArtifact.Spec.Properties,
		},
	})
	err = WrapRegistryApiIssues(err, responseMetadata.HTTPResponse)
	if err != nil {
		log.Error(err, "Could update artifact metadata")
		return ctrl.Result{}, err
	}
	log.Info("Metadata for artifact updated")

	// Update state if configured
	if &apicurioArtifact.Spec.State != nil {
		responseVersionMetaData, err := registryApiClient.GetArtifactVersionMetaDataWithResponse(ctx, responseArtifact.JSON200.Id, int(responseArtifact.JSON200.Version))
		err = WrapRegistryApiIssues(err, responseVersionMetaData.HTTPResponse)
		if err != nil {
			log.Error(err, "Could get artifact version metadata")
			return ctrl.Result{}, err
		}

		if *responseVersionMetaData.JSON200.State != RegistryApi.ArtifactState(apicurioArtifact.Spec.State) {
			responseStateUpdate, err := registryApiClient.UpdateArtifactVersionState(ctx, responseArtifact.JSON200.Id, int(responseArtifact.JSON200.Version), RegistryApi.UpdateArtifactVersionStateJSONRequestBody{
				State: RegistryApi.ArtifactState(apicurioArtifact.Spec.State),
			})
			err = WrapRegistryApiIssues(err, responseStateUpdate)
			if err != nil {
				log.Error(err, "Could update artifact version state")
				return ctrl.Result{}, err
			}
			log.Info("State for artifact updated", "state", apicurioArtifact.Spec.State)
		}
	}

	// Get artifact rules current configrtion
	if &apicurioArtifact.Spec.RuleValidity != nil || &apicurioArtifact.Spec.RuleCompatibility != nil {
		responseArtifactRules, err := registryApiClient.ListArtifactRulesWithResponse(ctx, responseArtifact.JSON200.Id)
		err = WrapRegistryApiIssues(err, responseArtifactRules.HTTPResponse)
		if err != nil {
			log.Error(err, "Could update artifact version state")
			return ctrl.Result{}, err
		}

		// Update rule VALIDITY if configured
		if &apicurioArtifact.Spec.RuleValidity != nil {
			var ruleType = RegistryApi.RuleType_VALIDITY
			if ContainsRuleType(*responseArtifactRules.JSON200, ruleType) {
				response, err := registryApiClient.UpdateArtifactRuleConfig(ctx, responseArtifact.JSON200.Id, string(ruleType), RegistryApi.UpdateArtifactRuleConfigJSONRequestBody{
					Config: string(apicurioArtifact.Spec.RuleValidity),
					Type:   &ruleType,
				})
				err = WrapRegistryApiIssues(err, response)
				if err != nil {
					log.Error(err, "Could update artifact rule VALIDITY")
					return ctrl.Result{}, err
				}
			} else {
				response, err := registryApiClient.CreateArtifactRule(ctx, responseArtifact.JSON200.Id, RegistryApi.CreateArtifactRuleJSONRequestBody{
					Config: string(apicurioArtifact.Spec.RuleValidity),
					Type:   &ruleType,
				})
				err = WrapRegistryApiIssues(err, response)
				if err != nil {
					log.Error(err, "Could create artifact rule VALIDITY")
					return ctrl.Result{}, err
				}
			}
			log.Info("Rule VALIDITY for artifact updated", "VALIDITY", apicurioArtifact.Spec.RuleValidity)
		}

		// Update rule COMPATIBILITY if configured
		if &apicurioArtifact.Spec.RuleCompatibility != nil {
			var ruleType = RegistryApi.RuleType_COMPATIBILITY
			if ContainsRuleType(*responseArtifactRules.JSON200, ruleType) {
				response, err := registryApiClient.UpdateArtifactRuleConfig(ctx, responseArtifact.JSON200.Id, string(ruleType), RegistryApi.UpdateArtifactRuleConfigJSONRequestBody{
					Config: string(apicurioArtifact.Spec.RuleCompatibility),
					Type:   &ruleType,
				})
				err = WrapRegistryApiIssues(err, response)
				if err != nil {
					log.Error(err, "Could create artifact rule COMPATIBILITY")
					return ctrl.Result{}, err
				}
			} else {
				response, err := registryApiClient.CreateArtifactRule(ctx, responseArtifact.JSON200.Id, RegistryApi.CreateArtifactRuleJSONRequestBody{
					Config: string(apicurioArtifact.Spec.RuleCompatibility),
					Type:   &ruleType,
				})
				err = WrapRegistryApiIssues(err, response)
				if err != nil {
					log.Error(err, "Could create artifact rule COMPATIBILITY")
					return ctrl.Result{}, err
				}
			}
			log.Info("Rule COMPATIBILITY for artifact updated", "COMPATIBILITY", apicurioArtifact.Spec.RuleCompatibility)
		}
	}

	log.Info("Reconcile done for Artifact successfully")
	return ctrl.Result{}, nil
}

// SetupWithManager sets up the controller with the Manager.
func (r *ApicurioReconciler) SetupWithManager(mgr ctrl.Manager) error {
	return ctrl.NewControllerManagedBy(mgr).
		For(&artifactv1alpha1.Apicurio{}).
		Complete(r)
}

func ContainsRuleType(a []RegistryApi.RuleType, x RegistryApi.RuleType) bool {
	for _, n := range a {
		if x == n {
			return true
		}
	}
	return false
}

type ValidationContentError struct{}

func (m *ValidationContentError) Error() string {
	return "You have to provide the filed content or externalContent"
}

type BadRequestError struct {
	message string
}

func (m *BadRequestError) Error() string {
	return m.message
}

func WrapRegistryApiIssues(err error, response *http.Response) error {
	if err != nil {
		return err
	}
	if response.StatusCode >= 400 {
		return &BadRequestError{response.Status}
	}
	return nil
}
