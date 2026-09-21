/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package internal

import (
	"github.com/spf13/cobra"
)

// newSaveCmd creates the save command
func newSaveCmd() *cobra.Command {
	saveCmd := &cobra.Command{
		Use:   "save",
		Short: "Save the state of an object.",
	}
	saveCmd.AddCommand(newSaveUserTaskRunProgressCmd())
	return saveCmd
}
