package internal

import (
	"fmt"
	"log"

	"github.com/spf13/cobra"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/emptypb"
)

func newVersionCmd(provider ClientProvider) *cobra.Command {
	versionCmd := &cobra.Command{
		Use:   "version",
		Short: "Print Client and Server Version Information.",
		Args:  cobra.ExactArgs(0),
		Run: func(cmd *cobra.Command, args []string) {
			fmt.Println("lhctl version: " + cmd.Root().Version)

			resp, err := provider.Client(cmd).GetServerVersion(provider.RequestContext(cmd), &emptypb.Empty{})
			if err != nil {
				if grpcStatus, ok := status.FromError(err); ok && grpcStatus.Code() == codes.Unimplemented {
					fmt.Println("Server is outdated")
				} else {
					log.Fatal(err)
				}
			} else {
				serverVersion := fmt.Sprintf("%d.%d", resp.MajorVersion, resp.MinorVersion)

				if resp.PatchVersion != nil {
					serverVersion = fmt.Sprintf("%s.%d", serverVersion, *resp.PatchVersion)
				}

				if resp.PreReleaseIdentifier != nil {
					serverVersion = serverVersion + "-" + *resp.PreReleaseIdentifier
				}

				fmt.Println("Server version: " + serverVersion)
			}
		},
	}
	return versionCmd
}
