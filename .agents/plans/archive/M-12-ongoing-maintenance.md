# M-12 — Ongoing Maintenance

**Status:** Completed  
**Date:** 2026-06-17  
**Milestone:** Post-v1.0.0

## Goal

Track optional improvements after initial release.

## Deliverables

- [x] OAuth2/JWT optional profile (`oauth2`); default remains open dev identity (`X-User-Id`)
- [x] User guide (`docs/user-guide.md`)
- [x] Prometheus metrics endpoint in `prod` profile (`/actuator/prometheus`)
- [x] Align README model names with `application.yml` (`OLLAMA_*` / `gemma4:31b-cloud`)

## Deferred to M-13

- Playwright E2E for browser smoke items
- Grafana dashboard artifacts

## Out of scope

- New SRS milestones unless requirements change
