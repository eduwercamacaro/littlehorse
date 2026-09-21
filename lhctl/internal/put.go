package internal

import (
	"github.com/spf13/cobra"
)

// newPutCmd creates the run command
func newPutCmd(provider ClientProvider) *cobra.Command {
	putCmd := &cobra.Command{
		Use:   "put",
		Short: "Create or update an object",
	}
	putCmd.AddCommand(
		newPutCorrelatedEventCmd(provider),
		newPutPrincipalCmd(provider),
		newPutQuotaCmd(provider),
		newPutTenantCmd(provider),
		newPutUserTaskRunCommentCmd(provider),
		newPutWorkflowEventDefCmd(provider),
		newPutWorkflowMigrationPlanCmd(provider),
	)
	return putCmd
}
