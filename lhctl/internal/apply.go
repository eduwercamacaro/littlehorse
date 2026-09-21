package internal

import (
	"github.com/spf13/cobra"
)

// newApplyCmd creates the apply command
func newApplyCmd(provider ClientProvider) *cobra.Command {
	applyCmd := &cobra.Command{
		Use:   "apply",
		Short: "Apply a resource to a running object.",
	}
	applyCmd.AddCommand(newApplyWorkflowMigrationPlanCmd(provider))
	return applyCmd
}
