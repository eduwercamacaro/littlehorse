package internal

import (
	"github.com/spf13/cobra"
)

// newSearchCmd creates the search command
func newSearchCmd() *cobra.Command {
	searchCmd := &cobra.Command{
		Use:   "search",
		Short: "Search for API Resources based on criteria.",
		Long: `Retrieves a list of API Resources based on search criteria.

Each resource (eg. WfRun, TaskDef, NodeRun, etc) has different search criteria.
For information about how to search for a specific resource, consult:
'lhctl search <resourceType> --help'
`,
	}
	searchCmd.PersistentFlags().Int32("limit", 100, "Guideline for number of response items to fetch per request.")
	searchCmd.PersistentFlags().BytesBase64("bookmark", nil, "Optional bookmark for paginated scans.")
	searchCmd.AddCommand(
		newSearchBulkJobCmd(),
		newSearchCorrelatedEventCmd(),
		newSearchExternalEventCmd(),
		newSearchExternalEventDefCmd(),
		newSearchNodeRunCmd(),
		newSearchPrincipalCmd(),
		newSearchQuotaCmd(),
		newSearchStructDefCmd(),
		newSearchTaskDefCmd(),
		newSearchTaskRunCmd(),
		newSearchTenantCmd(),
		newSearchUserTaskDefCmd(),
		newSearchUserTaskRunCmd(),
		newSearchVariableCmd(),
		newSearchWfMetricWindowCmd(),
		newSearchWfRunCmd(),
		newSearchScheduledWfsCmd(),
		newSearchWfSpecCmd(),
		newSearchWorkflowEventCmd(),
		newSearchWorkflowEventDefCmd(),
		newSearchWorkflowMigrationPlanCmd(),
	)
	return searchCmd
}
