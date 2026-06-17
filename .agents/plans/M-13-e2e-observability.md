# M-13 — E2E & Observability

**Status:** Active  
**Date:** 2026-06-17  
**Milestone:** Post-v1.0.0 maintenance

## Goal

Browser-level smoke automation and observability dashboards.

## Backlog

- [ ] Playwright E2E for browser smoke items (agent panel, MCP toggles, streaming UI)
- [ ] Grafana dashboard JSON for Prometheus metrics (`/actuator/prometheus` with `prod` profile)
- [ ] CI job for Playwright against `docker compose up` stack

## Out of scope

- New SRS milestones unless requirements change
