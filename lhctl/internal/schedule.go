package internal

import (
	"github.com/spf13/cobra"
)

// newScheduleCmd creates the run command
func newScheduleCmd() *cobra.Command {
	scheduleCmd := &cobra.Command{
		Use:   "schedule",
		Short: "Schedule a repeated operation",
	}
	scheduleCmd.AddCommand(newScheduleWfCmd())
	return scheduleCmd
}
