/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"github.com/spf13/cobra"
)

// newDeleteCmd creates the delete command
func newDeleteCmd() *cobra.Command {
	deleteCmd := &cobra.Command{
		Use:   "delete",
		Short: "Delete a resource.",
		Long: `Delete a resource. Supported resources:
- wfRun
`,
	}
	deleteCmd.AddCommand(
		newBulkDeleteWfRunCmd(),
		newDeleteBulkJobCmd(),
		newDeleteCorrelatedEventCmd(),
		newDeleteExternalEventDefCmd(),
		newDeletePrincipalCmd(),
		newDeleteQuotaCmd(),
		newDeleteStructDefCmd(),
		newDeleteTaskDefCmd(),
		newDeleteUserTaskDefCmd(),
		newDeleteUserTaskRunCommentCmd(),
		newDeleteWfRunCmd(),
		newDeleteScheduledWfRun(),
		newDeleteWfSpecCmd(),
		newDeleteWorkflowEventDefCmd(),
		newDeleteWorkflowMigrationPlanCmd(),
	)
	return deleteCmd
}
