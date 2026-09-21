/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"github.com/spf13/cobra"
)

// newGetCmd creates the get command
func newGetCmd(provider ClientProvider) *cobra.Command {
	getCmd := &cobra.Command{
		Use:   "get",
		Short: "Utility to GET LittleHorse API resources.",
	}
	getCmd.AddCommand(
		newGetBulkJobCmd(provider),
		newGetCorrelatedEventCmd(provider),
		newGetExternalEventCmd(provider),
		newGetExternalEventDefCmd(provider),
		newGetNodeRunCmd(provider),
		newGetPrincipalCmd(provider),
		newGetQuotaCmd(provider),
		newGetStructDefCmd(provider),
		newGetTaskDefCmd(provider),
		newGetTaskRunCmd(provider),
		newGetTaskWorkerGroup(provider),
		newGetTenantCmd(provider),
		newGetUserTaskDefCmd(provider),
		newGetUserTaskRunCmd(provider),
		newGetVariableCmd(provider),
		newGetMetricWindowCmd(provider),
		newGetWfRunCmd(provider),
		newGetScheduledWfRun(provider),
		newGetWfSpecCmd(provider),
		newGetWorkflowEventCmd(provider),
		newGetWorkflowEventDefCmd(provider),
		newGetWorkflowMigrationPlanCmd(provider),
	)
	return getCmd
}
