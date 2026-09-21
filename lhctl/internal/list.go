/*
Copyright © 2023 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"github.com/spf13/cobra"
)

// newListCmd creates the list command
func newListCmd() *cobra.Command {
	listCmd := &cobra.Command{
		Use:   "list",
		Short: "List objects according to some prefix",
		Long: `List objects according to some prefix

	Whereas 'lhctl search' returns the ID's of objects that satisfy a given
	search, 'lhctl list' returns the actual objects.`,
	}
	listCmd.AddCommand(newListExternalEventCmd())
	listCmd.PersistentFlags().Int32("limit", 100, "Guideline for number of response items to fetch per request.")
	listCmd.PersistentFlags().BytesBase64("bookmark", nil, "Optional bookmark for paginated scans.")
	listCmd.AddCommand(
		newListWfMetricsCmd(),
		newListNodeRunCmd(),
		newListQuotaMetricsCmd(),
		newListTaskMetricsCmd(),
		newListTaskRunCmd(),
		newListUserTaskRunCmd(),
		newListVariableCmd(),
		newListWorkflowEventCmd(),
	)
	return listCmd
}
