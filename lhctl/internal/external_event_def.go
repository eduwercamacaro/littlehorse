/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"fmt"
	"log"
	"os"

	"github.com/littlehorse-enterprises/littlehorse/sdk-go/lhproto"
	"github.com/littlehorse-enterprises/littlehorse/sdk-go/littlehorse"

	"github.com/spf13/cobra"
	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/proto"
)

func newDeployExternalEventDefCmd(provider ClientProvider) *cobra.Command {
	deployExternalEventDefCmd := &cobra.Command{
		Use:   "externalEventDef <filename>",
		Short: "Create an ExternalEventDef from a JSON or Protobuf file.",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			peed := &lhproto.PutExternalEventDefRequest{}

			// First, read the file
			dat, err := os.ReadFile(args[0])
			if err != nil {
				log.Fatal("Failed to read file: ", err)

			}

			useProto, err := cmd.Flags().GetBool("proto")
			if err != nil {
				log.Fatal("Unexpected error: ", err)
			}

			if useProto {
				fmt.Print("using proto")
				err = proto.Unmarshal(dat, peed)
			} else {
				err = protojson.Unmarshal(dat, peed)
			}
			if err != nil {
				log.Fatal("Failed reading deploy file: " + err.Error())

			}

			littlehorse.PrintResp(provider.Client(cmd).PutExternalEventDef(provider.RequestContext(cmd), peed))
		},
	}
	return deployExternalEventDefCmd
}

// newGetExternalEventDefCmd creates the externalEventDef command
func newGetExternalEventDefCmd(provider ClientProvider) *cobra.Command {
	getExternalEventDefCmd := &cobra.Command{
		Use:   "externalEventDef <name>",
		Short: "Get an ExternalEventDef by name.",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			littlehorse.PrintResp(
				provider.Client(cmd).GetExternalEventDef(
					provider.RequestContext(cmd),
					&lhproto.ExternalEventDefId{
						Name: args[0],
					},
				),
			)
		},
	}
	return getExternalEventDefCmd
}

func newSearchExternalEventDefCmd(provider ClientProvider) *cobra.Command {
	searchExternalEventDefCmd := &cobra.Command{
		Use:   "externalEventDef",
		Short: "Search for ExternalEventDef",
		Long: `Search for ExternalEventDefs.

No option groups for Search ExternalEventDef are supported. Therefore, this command
searches for all ExternalEventDefs.
	`,
		Run: func(cmd *cobra.Command, args []string) {
			bookmark, _ := cmd.Flags().GetBytesBase64("bookmark")
			limit, _ := cmd.Flags().GetInt32("limit")
			prefix, _ := cmd.Flags().GetString("prefix")

			littlehorse.PrintResp(
				provider.Client(cmd).SearchExternalEventDef(
					provider.RequestContext(cmd),
					&lhproto.SearchExternalEventDefRequest{
						Bookmark: bookmark,
						Limit:    &limit,
						Prefix:   &prefix,
					}),
			)
		},
	}
	return searchExternalEventDefCmd
}

func newDeleteExternalEventDefCmd(provider ClientProvider) *cobra.Command {
	deleteExternalEventDefCmd := &cobra.Command{
		Use:   "externalEventDef <name>",
		Short: "Delete an ExternalEventDef.",
		Long: `Delete an ExternalEventDef. You must provide the name of the
ExternalEventDef to delete.
	`,
		Args: cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			name := args[0]

			littlehorse.PrintResp(
				provider.Client(cmd).DeleteExternalEventDef(
					provider.RequestContext(cmd),
					&lhproto.DeleteExternalEventDefRequest{
						Id: &lhproto.ExternalEventDefId{
							Name: name,
						},
					}),
			)
		},
	}
	return deleteExternalEventDefCmd
}
