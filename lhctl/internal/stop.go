/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"github.com/spf13/cobra"
)

// newStopCmd creates the stop command
func newStopCmd(provider ClientProvider) *cobra.Command {
	stopCmd := &cobra.Command{
		Use:   "stop",
		Short: "Stop a resource.",
		Long: `Stop a resource. Supported resources:
- wfRun
`,
	}
	stopCmd.AddCommand(newStopWfRunCmd(provider))
	return stopCmd
}
