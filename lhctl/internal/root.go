package internal

import (
	"context"
	"fmt"
	"log"
	"os"

	"github.com/littlehorse-enterprises/littlehorse/sdk-go/lhproto"
	"github.com/littlehorse-enterprises/littlehorse/sdk-go/littlehorse"
	"github.com/spf13/cobra"
	"google.golang.org/grpc/metadata"
)

// ClientProvider supplies the client and request context needed by RPC commands.
type ClientProvider interface {
	Client(cmd *cobra.Command) lhproto.LittleHorseClient
	RequestContext(cmd *cobra.Command) context.Context
}

type dependencies struct {
	client lhproto.LittleHorseClient
	config *littlehorse.LHConfig
}

// RootOption customizes the dependencies used by one command tree.
type RootOption func(*dependencies)

// WithClient supplies the client used by the command tree.
func WithClient(client lhproto.LittleHorseClient) RootOption {
	return func(d *dependencies) { d.client = client }
}

// WithConfig supplies the configuration used by the command tree.
func WithConfig(config *littlehorse.LHConfig) RootOption {
	return func(d *dependencies) { d.config = config }
}

// NewRootCommand builds a fresh command tree with its own lazy client and configuration caches.
func NewRootCommand(version, commit, date string, options ...RootOption) *cobra.Command {
	d := &dependencies{}
	for _, option := range options {
		option(d)
	}
	rootCmd := &cobra.Command{
		Use:   "lhctl",
		Short: "Interact with the LittleHorse API",
		Long: `LittleHorse CLI: lhctl allows you to perform almost any action against a LittleHorse
cluster, ranging from managing metadata (WfSpec, TaskDef, etc) to running
a WfRun, to searching for various objects.
`,
	}
	rootCmd.Version = fmt.Sprintf("%s (Git SHA %s)", version, commit)
	rootCmd.PersistentFlags().String(
		"configFile",
		"${HOME}/.config/littlehorse.config",
		"Configuration File Location",
	)
	rootCmd.AddCommand(
		newApplyCmd(d),
		newAssignCmd(d),
		newCancelUserTaskCmd(d),
		newCountCmd(d),
		newDeleteCmd(d),
		newDeployCmd(d),
		newEditCmd(d),
		newExecuteCmd(d),
		newGetCmd(d),
		newListCmd(d),
		newLoginCmd(d),
		newPostEventCmd(d),
		newPutCmd(d),
		newRescueCmd(d),
		newResumeCmd(d),
		newRunCmd(d),
		newSaveCmd(d),
		newScheduleCmd(d),
		newSearchCmd(d),
		newStopCmd(d),
		newVersionCmd(d),
		newWhoamiCmd(d),
	)
	return rootCmd
}

func (d *dependencies) Config(cmd *cobra.Command) littlehorse.LHConfig {
	if d.config != nil {
		return *d.config
	}

	configLoc, err := cmd.Flags().GetString("configFile")
	if err != nil {
		log.Fatal(err)
	}

	d.config, err = littlehorse.NewConfigFromProps(configLoc)

	if err != nil {
		if os.IsNotExist(err) {
			if configLoc != "${HOME}/.config/littlehorse.config" {
				log.Fatal("provided config file does not exist")
			}
		} else {
			log.Fatal(err)
		}
		d.config = littlehorse.NewConfigFromEnv()
	}

	return *d.config
}

func (d *dependencies) Client(cmd *cobra.Command) lhproto.LittleHorseClient {
	if d.client != nil {
		return d.client
	}

	config := d.Config(cmd)
	client, err := config.GetGrpcClient()
	if err != nil {
		log.Fatal(err)
	}

	d.client = *client
	return d.client
}

func (d *dependencies) RequestContext(cmd *cobra.Command) context.Context {
	config := d.Config(cmd)
	if config.TenantId != nil {
		tenantId := *config.TenantId
		md := metadata.Pairs("tenantId", tenantId)
		return metadata.NewOutgoingContext(context.Background(), md)
	}
	return context.Background()
}
