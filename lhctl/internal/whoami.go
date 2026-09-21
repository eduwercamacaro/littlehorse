package internal

import (
	"github.com/littlehorse-enterprises/littlehorse/sdk-go/littlehorse"
	"github.com/spf13/cobra"
	"google.golang.org/protobuf/types/known/emptypb"
)

// newWhoamiCmd creates the run command
func newWhoamiCmd(provider ClientProvider) *cobra.Command {
	whoamiCmd := &cobra.Command{
		Use:   "whoami",
		Short: "Prints the current logged principal",
		Run: func(cmd *cobra.Command, args []string) {
			littlehorse.PrintResp(provider.Client(cmd).Whoami(provider.RequestContext(cmd), &emptypb.Empty{}))
		},
	}
	return whoamiCmd
}
