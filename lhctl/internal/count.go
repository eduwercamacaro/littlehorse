package internal

import (
	"github.com/spf13/cobra"
)

func newCountCmd() *cobra.Command {
	countCmd := &cobra.Command{
		Use:   "count",
		Short: "Count API Resources based on criteria.",
		Long: `Returns the count of API Resources matching the given criteria.

Each resource (eg. NodeRun) has different criteria options.
For information about how to count a specific resource, consult:
'lhctl count <resourceType> --help'
`,
	}
	countCmd.AddCommand(
		newCountNodeRunCmd(),
		newCountTaskRunCmd(),
	)
	return countCmd
}
