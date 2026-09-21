package internal

import (
	"github.com/spf13/cobra"
)

// newExecuteCmd creates the run command
func newExecuteCmd() *cobra.Command {
	executeCmd := &cobra.Command{
		Use:   "execute",
		Short: "Execute something. Generally a UserTaskRun.",
	}
	executeCmd.AddCommand(newExecuteUserTaskRunCmd())
	return executeCmd
}
