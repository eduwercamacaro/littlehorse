/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"github.com/spf13/cobra"
)

// newResumeCmd creates the resume command
func newResumeCmd(provider ClientProvider) *cobra.Command {
	resumeCmd := &cobra.Command{
		Use:   "resume",
		Short: "Resume a resource.",
		Long: `Resume a resource. Supported resources:
- wfRun
`,
	}
	resumeCmd.AddCommand(newResumeWfRunCmd(provider))
	return resumeCmd
}
