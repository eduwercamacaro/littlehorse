/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"github.com/spf13/cobra"
)

// newDeployCmd creates the deploy command
func newDeployCmd() *cobra.Command {
	deployCmd := &cobra.Command{
		Use:   "deploy",
		Short: "Deploy a resource specification in a file on your system.",
		Long: `
The 'deploy' command currently only supports reading from your filesystem.

Your file may be in protobuf or json format. Default is JSON.`,
	}
	deployCmd.PersistentFlags().Bool(
		"proto",
		false,
		"Whether file is in proto format. Default JSON",
	)
	deployCmd.AddCommand(
		newDeployExternalEventDefCmd(),
		newDeployPrincipalCmd(),
		newDeployTaskDefCmd(),
		newDeployUserTaskDefCmd(),
		newDeployWfSpecCmd(),
	)
	return deployCmd
}
