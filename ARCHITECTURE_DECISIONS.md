# RR Jaggery Traders — Approved Architecture Decisions

## ADR-001 — Frontend

React + TypeScript + Vite + Tailwind CSS.

## ADR-002 — Backend

Java 21 + Spring Boot.

## ADR-003 — Security

Spring Security + JWT.

## ADR-004 — Database

PostgreSQL is the application database from Day 1.

Oracle Database is not part of the approved architecture.

## ADR-005 — Cache

Redis may be used only where there is a demonstrated requirement.

Do not introduce Redis simply because it is available.

## ADR-006 — API

REST + OpenAPI/Swagger.

## ADR-007 — Containerization

Docker + Docker Compose.

## ADR-008 — Production

Initial production target:

Oracle Cloud Infrastructure Always Free Ampere A1 ARM64 VM.

Ubuntu Linux ARM64.

## ADR-009 — Production Topology

User
→ Cloudflare
→ Nginx
→ React + Spring Boot
→ PostgreSQL

All initially hosted on one OCI A1 VM using Docker Compose.

## ADR-010 — DNS and HTTPS

Cloudflare is the DNS/public edge layer.

Nginx is the reverse proxy.

HTTPS must be configured for production.

## ADR-011 — ARM64

All production containers and dependencies must support:

linux/arm64

CI should verify ARM64 compatibility.

## ADR-012 — Initial Infrastructure

Do not introduce:

* Kubernetes
* multiple production VMs
* separate VM per service
* Oracle Database
* Kafka
* RabbitMQ
* Elasticsearch
* heavy monitoring infrastructure

unless a documented requirement and explicit approval justify the change.

## ADR-013 — PostgreSQL Architecture

PostgreSQL is the source of truth for transactional application data.

The application should preserve clear domain/data ownership even if services initially share one PostgreSQL server.

## ADR-014 — Cost Philosophy

The initial infrastructure should target operation within OCI Always Free limits where possible.

Do not represent this as a guaranteed zero-cost outcome.

Domain registration, unexpected resource usage, storage, backups or other external costs may still apply.

## ADR-015 — Portability

The application should remain portable to another Linux VPS/cloud provider without redesigning the business system.

PostgreSQL remains the database abstraction target.

## ADR-016 — Backups

Backups and restore testing are mandatory because the application contains financially sensitive data.

## ADR-017 — Production Deployment Authority

No production deployment or destructive production operation should occur without the appropriate explicit approval.
