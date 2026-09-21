package internal

import (
	"github.com/spf13/cobra"
)

func newEditCmd(provider ClientProvider) *cobra.Command {
	editCmd := &cobra.Command{
		Use:   "edit",
		Short: "Edit an object",
	}
	editCmd.AddCommand(newEditUserTaskRunCommentCmd(provider))
	return editCmd
}
