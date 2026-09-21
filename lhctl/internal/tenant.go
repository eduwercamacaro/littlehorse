package internal

import (
	"github.com/littlehorse-enterprises/littlehorse/sdk-go/lhproto"
	"github.com/littlehorse-enterprises/littlehorse/sdk-go/littlehorse"

	"github.com/spf13/cobra"
)

func newPutTenantCmd(provider ClientProvider) *cobra.Command {
	putTenantCmd := &cobra.Command{
		Use:   "tenant <id>",
		Short: "Create or update a Tenant.",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			putTenantReq := &lhproto.PutTenantRequest{
				Id: args[0],
			}

			if cmd.Flag("all-entity-events").Changed {
				putTenantReq.OutputTopicConfig = &lhproto.OutputTopicConfig{
					DefaultRecordingLevel: lhproto.OutputTopicConfig_ALL_ENTITY_EVENTS,
				}
			} else if cmd.Flag("no-entity-events").Changed {
				putTenantReq.OutputTopicConfig = &lhproto.OutputTopicConfig{
					DefaultRecordingLevel: lhproto.OutputTopicConfig_NO_ENTITY_EVENTS,
				}
			}
			littlehorse.PrintResp(provider.Client(cmd).PutTenant(provider.RequestContext(cmd), putTenantReq))

		},
	}
	putTenantCmd.Flags().Bool("all-entity-events", false, "If set, the output topic will be enabled for this Tenant for all eligible entities.")
	putTenantCmd.Flags().Bool("no-entity-events", false, "If set, the output topic will be enabled for this Tenant, but only for explicitly-enabled entities.")
	putTenantCmd.MarkFlagsMutuallyExclusive("all-entity-events", "no-entity-events")
	return putTenantCmd
}

func newSearchTenantCmd(provider ClientProvider) *cobra.Command {
	searchTenantCmd := &cobra.Command{
		Use:   "tenant",
		Short: "Search for all available TenantIds for current Principal",
		Args:  cobra.ExactArgs(0),
		Run: func(cmd *cobra.Command, args []string) {
			bookmark, _ := cmd.Flags().GetBytesBase64("bookmark")
			limit, _ := cmd.Flags().GetInt32("limit")
			littlehorse.PrintResp(provider.Client(cmd).SearchTenant(
				provider.RequestContext(cmd),
				&lhproto.SearchTenantRequest{
					Bookmark: bookmark,
					Limit:    &limit,
				},
			))
		},
	}
	return searchTenantCmd
}

func newGetTenantCmd(provider ClientProvider) *cobra.Command {
	getTenantCmd := &cobra.Command{
		Use:   "tenant <id>",
		Short: "Get a Tenant",
		Args:  cobra.ExactArgs(1),
		Run: func(cmd *cobra.Command, args []string) {
			littlehorse.PrintResp(provider.Client(cmd).GetTenant(
				provider.RequestContext(cmd),
				&lhproto.TenantId{
					Id: args[0],
				},
			))
		},
	}
	return getTenantCmd
}
