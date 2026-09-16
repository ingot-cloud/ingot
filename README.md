# Ingot

[English](./README.md) | [简体中文](./README.zh-CN.md)

Ingot is an enterprise microservices platform for SaaS and multi-tenant applications. It provides reusable services and framework modules for identity and access management, tenant isolation, security policies, data access, caching, object storage, task scheduling, and other common platform concerns.

The project is designed for teams building business systems on the Spring ecosystem who want a consistent security model and a production-oriented foundation instead of recreating the same infrastructure for every service.

## Highlights

- **Identity and access management** — OAuth 2.0/OIDC authorization, JWT and JWK management, application authorization, roles, permissions, and data scopes.
- **Multi-tenancy** — tenant context propagation and MyBatis-Plus-based data isolation for SaaS workloads.
- **Security foundations** — credential policies, transport encryption, replay protection, account protection, gateway access policies, and security event recording.
- **Reusable infrastructure** — layered caching, dictionaries, event-driven invalidation, object storage, distributed IDs, verification codes, social login, and task scheduling.
- **Cloud-native services** — API gateway, authorization server, BFF, permission management, member management, and security center services.
- **Production operations** — Docker Compose and Docker Swarm deployment assets, environment-specific Nacos configuration, database migrations, performance guidance, and troubleshooting documentation.

## Architecture

| Area | Modules | Purpose |
| --- | --- | --- |
| Cloud services | `ingot-service` | Gateway, authorization, BFF, permission, member, security, and test services |
| Framework | `ingot-framework` | Shared security, data, cache, tenant, OSS, OpenAPI, scheduling, and integration components |
| Gradle plugins | `ingot-plugin` | Build conventions for MyBatis-Plus and service assembly |
| Configuration | `nacos` | Environment-specific service and shared configuration |
| Database | `databases` | Base schemas, seed data, migrations, and rollback scripts |
| Deployment | `deploy` | Middleware and application deployment assets |
| Documentation | `docs` | Product requirements, module guides, operations guides, and engineering standards |
| Specifications | `specs` | Active changes and the accepted behavioral baseline used by the SDD workflow |

### Main services

| Service | Responsibility |
| --- | --- |
| `ingot-gateway` | External API gateway and gateway-level security enforcement |
| `ingot-auth` | OAuth 2.0/OIDC authorization and token services |
| `ingot-bff` | Backend-for-frontend authentication and session flows |
| `ingot-iam-provider` | Permission, role, application, organization, and tenant management |
| `ingot-member-provider` | Member and user-domain capabilities |
| `ingot-security-provider` | Central security policy and security event capabilities |

## Technology stack

| Component | Version or choice |
| --- | --- |
| Java | 21 |
| Gradle | 8.12.1 (Wrapper included) |
| Spring Boot | 3.5.7 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| Spring Authorization Server | 1.5.3 |
| MyBatis-Plus | 3.5.10.1 |
| Service discovery and configuration | Nacos |
| Persistence and caching | MySQL, Redis, Caffeine |

The authoritative dependency versions are maintained in [`versions.gradle`](./versions.gradle).

## Getting started

### Prerequisites

- JDK 21
- Git
- Docker with Docker Compose, if you want to run the provided infrastructure or deployment stack

### Clone and build

```bash
git clone https://github.com/ingot-cloud/ingot.git
cd ingot
./gradlew clean build
```

To inspect all available modules and tasks:

```bash
./gradlew projects
./gradlew tasks
```

### Run locally

Running a service requires MySQL, Redis, and Nacos, together with the corresponding database and configuration data:

- Middleware definitions: [`deploy/middleware/standalone`](./deploy/middleware/standalone/)
- Database schemas and migrations: [`databases`](./databases/)
- Nacos configuration sets: [`nacos`](./nacos/)

After preparing those dependencies, run an individual service through Gradle. For example:

```bash
./gradlew :ingot-service:ingot-gateway:bootRun
```

For container-based application deployment, see the [deployment guide](./deploy/services/README.md). Review all sample credentials, secrets, network settings, and storage paths before using the supplied configuration outside a local environment.

## Documentation

- [Documentation center](./docs/README.md) — product direction, module documentation, deployment, performance, and troubleshooting
- [Product vision](./docs/requirements/VISION.md) — goals, users, core capabilities, and non-goals
- [Module documentation](./docs/modules/README.md) — implementation and usage guides by module
- [Operations guides](./docs/guides/README.md) — deployment, configuration, performance, upgrades, and troubleshooting
- [Current specifications](./specs/current/README.md) — accepted system behavior
- [Spec-Driven Development workflow](./specs/README.md) — contribution and change-management process

## Development workflow

This repository uses Spec-Driven Development for changes that affect business behavior, data models, public interfaces, or deployment compatibility. Before implementing such a change, read the [SDD workflow](./specs/README.md), inspect the current specifications and active changes, and obtain approval for the relevant change package.

Useful verification commands include:

```bash
./gradlew test
./gradlew clean build
```

## License

Ingot is licensed under the [Apache License 2.0](./LICENSE).
