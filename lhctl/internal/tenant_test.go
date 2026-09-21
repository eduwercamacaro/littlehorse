package internal

import (
	"context"
	"io"
	"testing"

	"github.com/littlehorse-enterprises/littlehorse/sdk-go/lhproto"
	"github.com/littlehorse-enterprises/littlehorse/sdk-go/littlehorse"
	"github.com/spf13/cobra"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
	"google.golang.org/protobuf/proto"
)

type tenantClient struct {
	lhproto.LittleHorseClient
	getTenantRequest *lhproto.TenantId
	getTenantContext context.Context
}

func (c *tenantClient) GetTenant(
	ctx context.Context,
	request *lhproto.TenantId,
	_ ...grpc.CallOption,
) (*lhproto.Tenant, error) {
	c.getTenantContext = ctx
	c.getTenantRequest = request
	return &lhproto.Tenant{Id: request}, nil
}

type testClientProvider struct {
	client lhproto.LittleHorseClient
	ctx    context.Context
}

func (p testClientProvider) Client(*cobra.Command) lhproto.LittleHorseClient {
	return p.client
}

func (p testClientProvider) RequestContext(*cobra.Command) context.Context {
	return p.ctx
}

func TestGetTenantWithProvider(t *testing.T) {
	t.Parallel()
	client := &tenantClient{}
	ctx := metadata.NewOutgoingContext(context.Background(), metadata.Pairs("tenantId", "configured-tenant"))
	command := newGetCmd(testClientProvider{client: client, ctx: ctx})
	command.SetOut(io.Discard)
	command.SetErr(io.Discard)
	command.SetArgs([]string{"tenant", "my-tenant"})
	if err := command.Execute(); err != nil {
		t.Fatal(err)
	}
	if want := (&lhproto.TenantId{Id: "my-tenant"}); !proto.Equal(client.getTenantRequest, want) {
		t.Fatalf("GetTenant request = %v; want %v", client.getTenantRequest, want)
	}
	if client.getTenantContext != ctx {
		t.Fatal("GetTenant did not receive the provider's context")
	}
}

func TestGetTenant(t *testing.T) {
	testCases := []struct {
		name         string
		requestedID  string
		configuredID string
	}{
		{name: "first root", requestedID: "first-tenant", configuredID: "first-configured-tenant"},
		{name: "second root", requestedID: "second-tenant", configuredID: "second-configured-tenant"},
	}

	for _, testCase := range testCases {
		t.Run(testCase.name, func(t *testing.T) {
			t.Parallel()

			client := &tenantClient{}
			root := NewRootCommand(
				"test",
				"abc",
				"unknown",
				WithClient(client),
				WithConfig(&littlehorse.LHConfig{TenantId: &testCase.configuredID}),
			)
			root.SetOut(io.Discard)
			root.SetErr(io.Discard)
			root.SetArgs([]string{"get", "tenant", testCase.requestedID})

			if err := root.Execute(); err != nil {
				t.Fatal(err)
			}

			want := &lhproto.TenantId{Id: testCase.requestedID}
			if !proto.Equal(client.getTenantRequest, want) {
				t.Fatalf("GetTenant request = %v; want %v", client.getTenantRequest, want)
			}

			requestMetadata, ok := metadata.FromOutgoingContext(client.getTenantContext)
			if !ok {
				t.Fatal("request context has no outgoing metadata")
			}
			if tenants := requestMetadata.Get("tenantId"); len(tenants) != 1 || tenants[0] != testCase.configuredID {
				t.Fatalf("tenantId metadata = %v; want [%s]", tenants, testCase.configuredID)
			}
		})
	}
}
