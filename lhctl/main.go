/*
Copyright © 2022 NAME HERE <EMAIL ADDRESS>
*/
package main

import (
	"os"

	"github.com/littlehorse-enterprises/lhctl/internal"
)

var (
	version = "0.0.0-development"
	commit  = "none"
	date    = "unknown"
)

func main() {
	root := internal.NewRootCommand(version, commit, date)
	if err := root.Execute(); err != nil {
		os.Exit(1)
	}
}
