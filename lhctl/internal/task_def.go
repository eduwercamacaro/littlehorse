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

func newDeployTaskDefCmd() *cobra.Command {
	deployTaskDefCmd := &cobra.Command{
		Use:   "taskDef <filename>",
		Short: "Create a TaskDef from a JSON or Protobuf file.",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			ptd := &lhproto.PutTaskDefRequest{}

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
				err = proto.Unmarshal(dat, ptd)
			} else {
				err = protojson.Unmarshal(dat, ptd)
			}
			if err != nil {
				log.Fatal("Failed reading deploy file: " + err.Error())

			}

			littlehorse.PrintResp(getGlobalClient(cmd).PutTaskDef(requestContext(cmd), ptd))
		},
	}
	return deployTaskDefCmd
}

// newGetTaskDefCmd creates the taskRun command
func newGetTaskDefCmd() *cobra.Command {
	getTaskDefCmd := &cobra.Command{
		Use:   "taskDef <name>",
		Short: "Get a TaskDef by Name",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			name := args[0]

			littlehorse.PrintResp(
				getGlobalClient(cmd).GetTaskDef(
					requestContext(cmd),
					&lhproto.TaskDefId{
						Name: name,
					},
				),
			)
		},
	}
	return getTaskDefCmd
}

func newSearchTaskDefCmd() *cobra.Command {
	searchTaskDefCmd := &cobra.Command{
		Use:   "taskDef [<prefix>]",
		Short: "Search for TaskDefs",
		Long: `Search for TaskDefs.
	
You can search for all TaskDefs or may optionally provide a prefix for your search.
	`,
		Args: cobra.MaximumNArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			bookmark, _ := cmd.Flags().GetBytesBase64("bookmark")
			limit, _ := cmd.Flags().GetInt32("limit")
			search := &lhproto.SearchTaskDefRequest{
				Bookmark: bookmark,
				Limit:    &limit,
			}

			if len(args) > 0 {
				search.Prefix = &args[0]
			}

			littlehorse.PrintResp(
				getGlobalClient(cmd).SearchTaskDef(
					requestContext(cmd),
					search,
				),
			)
		},
	}
	return searchTaskDefCmd
}

func newDeleteTaskDefCmd() *cobra.Command {
	deleteTaskDefCmd := &cobra.Command{
		Use:   "taskDef <name>",
		Short: "Delete a TaskDef.",
		Long: `Delete a TaskDef. You must provide the name and of the TaskDef to delete.
	`,
		Args: cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			name := args[0]

			littlehorse.PrintResp(
				getGlobalClient(cmd).DeleteTaskDef(
					requestContext(cmd),
					&lhproto.DeleteTaskDefRequest{
						Id: &lhproto.TaskDefId{
							Name: name,
						},
					}),
			)
		},
	}
	return deleteTaskDefCmd
}
