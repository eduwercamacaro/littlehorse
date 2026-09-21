package internal

import (
	"github.com/spf13/cobra"
)

// newScheduleCmd creates the run command
func newScheduleCmd(provider ClientProvider) *cobra.Command {
	scheduleCmd := &cobra.Command{
		Use:   "schedule",
		Short: "Schedule a repeated operation",
	}
	scheduleCmd.AddCommand(newScheduleWfCmd(provider))
	return scheduleCmd
}
