/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"github.com/spf13/cobra"
)

// newGetCmd creates the get command
func newGetCmd() *cobra.Command {
	getCmd := &cobra.Command{
		Use:   "get",
		Short: "Utility to GET LittleHorse API resources.",
	}
	getCmd.AddCommand(
		newGetBulkJobCmd(),
		newGetCorrelatedEventCmd(),
		newGetExternalEventCmd(),
		newGetExternalEventDefCmd(),
		newGetNodeRunCmd(),
		newGetPrincipalCmd(),
		newGetQuotaCmd(),
		newGetStructDefCmd(),
		newGetTaskDefCmd(),
		newGetTaskRunCmd(),
		newGetTaskWorkerGroup(),
		newGetTenantCmd(),
		newGetUserTaskDefCmd(),
		newGetUserTaskRunCmd(),
		newGetVariableCmd(),
		newGetMetricWindowCmd(),
		newGetWfRunCmd(),
		newGetScheduledWfRun(),
		newGetWfSpecCmd(),
		newGetWorkflowEventCmd(),
		newGetWorkflowEventDefCmd(),
		newGetWorkflowMigrationPlanCmd(),
	)
	return getCmd
}
