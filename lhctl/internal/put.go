package internal

import (
	"github.com/spf13/cobra"
)

// newPutCmd creates the run command
func newPutCmd() *cobra.Command {
	putCmd := &cobra.Command{
		Use:   "put",
		Short: "Create or update an object",
	}
	putCmd.AddCommand(
		newPutCorrelatedEventCmd(),
		newPutPrincipalCmd(),
		newPutQuotaCmd(),
		newPutTenantCmd(),
		newPutUserTaskRunCommentCmd(),
		newPutWorkflowEventDefCmd(),
		newPutWorkflowMigrationPlanCmd(),
	)
	return putCmd
}
