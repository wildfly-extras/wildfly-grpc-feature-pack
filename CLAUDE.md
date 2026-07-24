# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WildFly gRPC Feature Pack — a Galleon feature pack that adds gRPC subsystem support to WildFly. gRPC services implementing `io.grpc.BindableService` are auto-discovered at deployment time and registered against a Netty-based gRPC server (default port 9555). The subsystem has `PREVIEW` stability.

Group ID: `org.wildfly.grpc` (changed from `org.wildfly.extras.grpc` in 0.1.17).

## Build Commands

```bash
# Full build with tests
mvn install

# Full build including examples
mvn install -P examples

# Build without tests
mvn install -DskipTests

# Run subsystem unit test only
mvn test -pl subsystem

# Format source code (license headers, code formatter, import sorting)
./format.sh

# Validate formatting without modifying files
./validate.sh
```

## Running Examples

The helloworld example provisions a WildFly server with the gRPC subsystem:

```bash
# Start server (ssl: none, oneway, twoway)
mvn wildfly:run -P examples -pl examples/helloworld/service -Dssl=none

# Run client
mvn exec:java -P examples -pl examples/helloworld/client -Dexec.args="Bob none"
```

## Architecture

### Module Structure

- **`subsystem/`** — The WildFly subsystem extension (`org.wildfly.extension.grpc`). Contains the management model definition, XML schema parsing, deployment processors, and the gRPC server service.
- **`galleon-pack/`** — Galleon packaging. `common/` holds shared JBoss Module descriptors and layer definitions. `feature-pack/` and `preview-feature-pack/` produce the installable Galleon feature packs.
- **`build/`** — Provisions a local WildFly server with the gRPC subsystem for integration testing.
- **`testsuite/integration/subsystem/`** — Arquillian-based integration tests (helloworld, interceptors, SSL variants).
- **`examples/`** — Standalone examples (helloworld, chat), each split into proto/service/client modules.
- **`code-parent/`** — Parent POM for modules containing code; manages all dependency versions and plugin configurations.
- **`build-config/`** — Checkstyle configuration.

### Key Subsystem Classes

- `GrpcExtension` — Entry point; registers the subsystem with WildFly. Stability: `PREVIEW`.
- `GrpcSubsystemDefinition` — Defines all management attributes (flow control, keep-alive, TLS, etc.).
- `GrpcSubsystemSchema` — XML schema for subsystem configuration (`urn:wildfly:grpc:1.0`).
- `GrpcServerService` — MSC service that starts/stops the Netty gRPC server, manages service registration, and handles SSL context setup.
- `GrpcDeploymentProcessor` — Scans deployments for `BindableService` and `ServerInterceptor` implementations using Jandex, then registers them with the gRPC server.
- `GrpcDeploymentXMLParser` — Parses per-deployment gRPC configuration from `META-INF/grpc-deployment.xml`.

### How Deployment Works

1. `GrpcDependencyProcessor` adds gRPC module dependencies to the deployment.
2. `GrpcDeploymentProcessor` uses the Jandex annotation index to find all `BindableService` implementations (leaf classes only) and `ServerInterceptor` implementations.
3. Services are instantiated via no-arg constructor and registered with the `MutableHandlerRegistry` on the running gRPC server.
4. Interceptors are applied per-method via `InternalServerInterceptors`.

## Conventions

- Java 17 minimum.
- License header: `Copyright The WildFly Authors / SPDX-License-Identifier: Apache-2.0` — enforced by `license-maven-plugin`.
- Code formatting uses WildFly's `eclipse-code-formatter.xml` and `impsort-maven-plugin` — always run `./format.sh` before committing.
- Tests use JUnit 4 + Arquillian with WildFly managed containers.
- Uses Maven wrapper (`./mvnw`); CI runs `./mvnw install -Pexamples`.
