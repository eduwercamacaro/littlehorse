package internal

import (
	"github.com/littlehorse-enterprises/littlehorse/sdk-go/lhproto"
	"github.com/littlehorse-enterprises/littlehorse/sdk-go/littlehorse"
	"github.com/spf13/cobra"
)

func newGetTaskWorkerGroup(provider ClientProvider) *cobra.Command {
	getTaskWorkerGroup := &cobra.Command{
		Use:   "taskWorkerGroup <taskDefName>",
		Short: "",
		Long:  `Gets the registered task worker group associated with a specific TaskDef`,
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			littlehorse.PrintResp(provider.Client(cmd).GetTaskWorkerGroup(
				provider.RequestContext(cmd),
				&lhproto.TaskDefId{
					Name: args[0],
				},
			))
		},
	}
	return getTaskWorkerGroup
}
