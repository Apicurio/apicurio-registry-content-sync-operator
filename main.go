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

package main

import (
	"flag"
	"fmt"
	RegistryApi "github.com/dweber019/apicurio-registry-artifact-operator/registry_api"
	"os"
	"sigs.k8s.io/controller-runtime/pkg/cache"
	"strings"

	// Import all Kubernetes client auth plugins (e.g. Azure, GCP, OIDC, etc.)
	// to ensure that exec-entrypoint and run can make use of them.
	_ "k8s.io/client-go/plugin/pkg/client/auth"

	"k8s.io/apimachinery/pkg/runtime"
	utilruntime "k8s.io/apimachinery/pkg/util/runtime"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/healthz"
	"sigs.k8s.io/controller-runtime/pkg/log/zap"

	artifactv1alpha1 "github.com/dweber019/apicurio-registry-artifact-operator/api/v1alpha1"
	"github.com/dweber019/apicurio-registry-artifact-operator/controllers"
	// +kubebuilder:scaffold:imports
)

var (
	scheme   = runtime.NewScheme()
	setupLog = ctrl.Log.WithName("setup")
)

func init() {
	utilruntime.Must(clientgoscheme.AddToScheme(scheme))

	utilruntime.Must(artifactv1alpha1.AddToScheme(scheme))
	// +kubebuilder:scaffold:scheme
}

func main() {
	var metricsAddr string
	var enableLeaderElection bool
	var probeAddr string
	flag.StringVar(&metricsAddr, "metrics-bind-address", ":8080", "The address the metric endpoint binds to.")
	flag.StringVar(&probeAddr, "health-probe-bind-address", ":8081", "The address the probe endpoint binds to.")
	flag.BoolVar(&enableLeaderElection, "leader-elect", false,
		"Enable leader election for controller manager. "+
			"Enabling this will ensure there is only one active controller manager.")
	opts := zap.Options{
		Development: true,
	}
	opts.BindFlags(flag.CommandLine)
	flag.Parse()

	ctrl.SetLogger(zap.New(zap.UseFlagOptions(&opts)))

	watchNamespace, err := getWatchNamespace()
	if err != nil {
		setupLog.Error(err, "unable to get WatchNamespace, "+
			"the manager will watch and manage resources in all namespaces")
	}

	options := ctrl.Options{
		Scheme:             scheme,
		MetricsBindAddress: metricsAddr,
		Port:               9443,
		LeaderElection:     enableLeaderElection,
		LeaderElectionID:   "446af786.apicurio.io",
		Namespace:          watchNamespace, // namespaced-scope when the value is not an empty string
	}

	// Add support for MultiNamespace set in WATCH_NAMESPACE (e.g ns1,ns2)
	if strings.Contains(watchNamespace, ",") {
		setupLog.Info("manager set up with multiple namespaces", "namespaces", watchNamespace)
		// configure cluster-scoped with MultiNamespacedCacheBuilder
		options.Namespace = ""
		options.NewCache = cache.MultiNamespacedCacheBuilder(strings.Split(watchNamespace, ","))
	}

	mgr, err := ctrl.NewManager(ctrl.GetConfigOrDie(), options)
	if err != nil {
		setupLog.Error(err, "unable to start manager")
		os.Exit(1)
	}

	apicurioEndpoint, err := getApicurioEndpoint()
	if err != nil {
		setupLog.Error(err, "unable to get Apicurio endpoint, "+
			"this is fatal, please define env APICURIO_ENDPOINT")
		os.Exit(1)
	}

	setupLog.Info("Using Apicurio registry endpoint ", "endpoint", apicurioEndpoint)
	registryApiClient, err := RegistryApi.NewClientWithResponses(apicurioEndpoint)
	if err != nil {
		setupLog.Error(err, "unable to create Apicurio client")
		os.Exit(1)
	}

	if err = (&controllers.ApicurioReconciler{
		Client:         mgr.GetClient(),
		Log:            ctrl.Log.WithName("controllers").WithName("Apicurio"),
		Scheme:         mgr.GetScheme(),
		ApicurioClient: *registryApiClient,
	}).SetupWithManager(mgr); err != nil {
		setupLog.Error(err, "unable to create controller", "controller", "Apicurio")
		os.Exit(1)
	}
	// +kubebuilder:scaffold:builder

	if err := mgr.AddHealthzCheck("health", healthz.Ping); err != nil {
		setupLog.Error(err, "unable to set up health check")
		os.Exit(1)
	}
	if err := mgr.AddReadyzCheck("check", healthz.Ping); err != nil {
		setupLog.Error(err, "unable to set up ready check")
		os.Exit(1)
	}

	setupLog.Info("starting manager")
	if err := mgr.Start(ctrl.SetupSignalHandler()); err != nil {
		setupLog.Error(err, "problem running manager")
		os.Exit(1)
	}
}

func getWatchNamespace() (string, error) {
	var watchNamespaceEnvVar = "WATCH_NAMESPACE"
	ns, found := os.LookupEnv(watchNamespaceEnvVar)
	if !found {
		return "", fmt.Errorf("%s must be set", watchNamespaceEnvVar)
	}
	return ns, nil
}

func getApicurioEndpoint() (string, error) {
	var apicurioEndpointEnvVar = "APICURIO_ENDPOINT"

	ns, found := os.LookupEnv(apicurioEndpointEnvVar)
	if !found {
		return "", fmt.Errorf("%s must be set", apicurioEndpointEnvVar)
	}
	return ns, nil
}
