package internal

import (
	"github.com/spf13/cobra"
)

// newAssignCmd creates the run command
func newAssignCmd(provider ClientProvider) *cobra.Command {
	assignCmd := &cobra.Command{
		Use:   "assign",
		Short: "Assign something. Generally a UserTaskRun.",
	}
	assignCmd.AddCommand(newAssignUserTaskRunCmd(provider))
	return assignCmd
}
