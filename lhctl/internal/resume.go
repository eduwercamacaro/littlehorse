/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"github.com/spf13/cobra"
)

// newResumeCmd creates the resume command
func newResumeCmd() *cobra.Command {
	resumeCmd := &cobra.Command{
		Use:   "resume",
		Short: "Resume a resource.",
		Long: `Resume a resource. Supported resources:
- wfRun
`,
	}
	resumeCmd.AddCommand(newResumeWfRunCmd())
	return resumeCmd
}
