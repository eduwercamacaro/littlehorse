package internal

import (
	"github.com/spf13/cobra"
)

// newCancelUserTaskCmd creates the run command
func newCancelUserTaskCmd(provider ClientProvider) *cobra.Command {
	cancelUserTaskCmd := &cobra.Command{
		Use:   "cancel",
		Short: "Cancel a LH object. Generally a UserTaskRun.",
	}
	cancelUserTaskCmd.AddCommand(newCancelUserTaskRunCmd(provider))
	return cancelUserTaskCmd
}
