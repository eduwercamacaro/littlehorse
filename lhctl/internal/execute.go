package internal

import (
	"github.com/spf13/cobra"
)

// newExecuteCmd creates the run command
func newExecuteCmd(provider ClientProvider) *cobra.Command {
	executeCmd := &cobra.Command{
		Use:   "execute",
		Short: "Execute something. Generally a UserTaskRun.",
	}
	executeCmd.AddCommand(newExecuteUserTaskRunCmd(provider))
	return executeCmd
}
