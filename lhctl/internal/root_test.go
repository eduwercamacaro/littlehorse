package internal

import (
	"bytes"
	"io"
	"strings"
	"testing"

	"github.com/spf13/cobra"
	"github.com/spf13/pflag"
)

func TestNewRootCommandOwnsCommandAndFlagState(t *testing.T) {
	first := NewRootCommand("first", "abc", "unknown")
	second := NewRootCommand("second", "def", "unknown")
	var compare func(*cobra.Command, *cobra.Command)
	compare = func(a, b *cobra.Command) {
		t.Helper()
		if a == b {
			t.Fatalf("shared command: %s", a.CommandPath())
		}
		a.LocalFlags().VisitAll(func(flag *pflag.Flag) {
			other := b.LocalFlags().Lookup(flag.Name)
			if other == nil {
				t.Fatalf("missing flag %s on %s", flag.Name, b.CommandPath())
			}
			if flag == other || flag.Value == other.Value {
				t.Fatalf("shared flag %s on %s", flag.Name, a.CommandPath())
			}
		})
		childrenA, childrenB := a.Commands(), b.Commands()
		if len(childrenA) != len(childrenB) {
			t.Fatalf("different child counts for %s", a.CommandPath())
		}
		for i, child := range childrenA {
			if child.Name() != childrenB[i].Name() {
				t.Fatalf("different children for %s", a.CommandPath())
			}
			if child.Parent() != a || childrenB[i].Parent() != b {
				t.Fatalf("incorrect parent for %s", child.Name())
			}
			compare(child, childrenB[i])
		}
	}
	compare(first, second)
}

func TestNewRootCommandDoesNotLeakParsedFlags(t *testing.T) {
	first := NewRootCommand("test", "abc", "unknown")
	first.SetOut(io.Discard)
	first.SetErr(io.Discard)
	first.SetArgs([]string{"search", "externalEvent", "--isClaimed=false", "--limit=7", "--configFile=first.config", "--help"})
	if err := first.Execute(); err != nil {
		t.Fatal(err)
	}
	firstSearch, _, err := first.Find([]string{"search", "externalEvent"})
	if err != nil {
		t.Fatal(err)
	}
	if !firstSearch.Flags().Changed("isClaimed") {
		t.Fatal("first command did not record explicit false flag")
	}

	second := NewRootCommand("test", "abc", "unknown")
	var output bytes.Buffer
	second.SetOut(&output)
	second.SetErr(&output)
	// Missing arguments must still fail: the first tree's --help must not leak.
	second.SetArgs([]string{"get", "tenant"})
	if err := second.Execute(); err == nil {
		t.Fatal("expected missing tenant ID error")
	}
	secondSearch, _, err := second.Find([]string{"search", "externalEvent"})
	if err != nil {
		t.Fatal(err)
	}
	if secondSearch.Flags().Changed("isClaimed") {
		t.Fatal("isClaimed state leaked between roots")
	}
	search := secondSearch.Parent()
	limit, err := search.PersistentFlags().GetInt32("limit")
	if err != nil || limit != 100 {
		t.Fatalf("limit = %d, err = %v; want 100", limit, err)
	}
	config, err := second.PersistentFlags().GetString("configFile")
	if err != nil || config != "${HOME}/.config/littlehorse.config" {
		t.Fatalf("configFile = %q, err = %v", config, err)
	}
}

func TestNewRootCommandValidation(t *testing.T) {
	cases := []struct {
		name string
		args []string
		want string
	}{
		{"arguments", []string{"get", "tenant"}, "accepts 1 arg(s), received 0"},
		{"required flags", []string{"search", "variable"}, "required flag(s)"},
		{"exclusive flags", []string{"put", "tenant", "test", "--all-entity-events", "--no-entity-events"}, "none of the others can be"},
		{"nested command", []string{"search", "wfRun", "byParent"}, "accepts 1 arg(s), received 0"},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			root := NewRootCommand("test", "abc", "unknown")
			root.SetOut(io.Discard)
			root.SetErr(io.Discard)
			root.SetArgs(tc.args)
			err := root.Execute()
			if err == nil || !strings.Contains(err.Error(), tc.want) {
				t.Fatalf("error = %v; want %q", err, tc.want)
			}
		})
	}
}

func TestNewRootCommandVersion(t *testing.T) {
	first := NewRootCommand("1.2.3", "abc", "unknown")
	second := NewRootCommand("4.5.6", "def", "unknown")
	for _, tc := range []struct {
		root *cobra.Command
		want string
	}{
		{first, "lhctl version 1.2.3 (Git SHA abc)\n"},
		{second, "lhctl version 4.5.6 (Git SHA def)\n"},
	} {
		var output bytes.Buffer
		tc.root.SetOut(&output)
		tc.root.SetErr(&output)
		tc.root.SetArgs([]string{"--configFile=does-not-exist.config", "--version"})
		if err := tc.root.Execute(); err != nil {
			t.Fatal(err)
		}
		if output.String() != tc.want {
			t.Fatalf("version = %q; want %q", output.String(), tc.want)
		}
	}
}
