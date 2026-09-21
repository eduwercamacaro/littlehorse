package internal

import (
	"github.com/spf13/cobra"
)

// newSearchCmd creates the search command
func newSearchCmd(provider ClientProvider) *cobra.Command {
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
		newSearchBulkJobCmd(provider),
		newSearchCorrelatedEventCmd(provider),
		newSearchExternalEventCmd(provider),
		newSearchExternalEventDefCmd(provider),
		newSearchNodeRunCmd(provider),
		newSearchPrincipalCmd(provider),
		newSearchQuotaCmd(provider),
		newSearchStructDefCmd(provider),
		newSearchTaskDefCmd(provider),
		newSearchTaskRunCmd(provider),
		newSearchTenantCmd(provider),
		newSearchUserTaskDefCmd(provider),
		newSearchUserTaskRunCmd(provider),
		newSearchVariableCmd(provider),
		newSearchWfMetricWindowCmd(provider),
		newSearchWfRunCmd(provider),
		newSearchScheduledWfsCmd(provider),
		newSearchWfSpecCmd(provider),
		newSearchWorkflowEventCmd(provider),
		newSearchWorkflowEventDefCmd(provider),
		newSearchWorkflowMigrationPlanCmd(provider),
	)
	return searchCmd
}
