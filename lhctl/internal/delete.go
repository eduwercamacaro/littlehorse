/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"github.com/spf13/cobra"
)

// newDeleteCmd creates the delete command
func newDeleteCmd(provider ClientProvider) *cobra.Command {
	deleteCmd := &cobra.Command{
		Use:   "delete",
		Short: "Delete a resource.",
		Long: `Delete a resource. Supported resources:
- wfRun
`,
	}
	deleteCmd.AddCommand(
		newBulkDeleteWfRunCmd(provider),
		newDeleteBulkJobCmd(provider),
		newDeleteCorrelatedEventCmd(provider),
		newDeleteExternalEventDefCmd(provider),
		newDeletePrincipalCmd(provider),
		newDeleteQuotaCmd(provider),
		newDeleteStructDefCmd(provider),
		newDeleteTaskDefCmd(provider),
		newDeleteUserTaskDefCmd(provider),
		newDeleteUserTaskRunCommentCmd(provider),
		newDeleteWfRunCmd(provider),
		newDeleteScheduledWfRun(provider),
		newDeleteWfSpecCmd(provider),
		newDeleteWorkflowEventDefCmd(provider),
		newDeleteWorkflowMigrationPlanCmd(provider),
	)
	return deleteCmd
}
