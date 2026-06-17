# M-10 — M9 Docker + Polish

**Status:** Active  
**Date:** 2026-06-17  
**Milestone:** M9 per [docs/01-requirements.md §14](../../docs/01-requirements.md)

## Goal

Production-ready packaging and smoke validation for the full stack.

## Deliverables

- [ ] `Dockerfile` multi-stage build for ai-chat
- [ ] `docker-compose.yml` — PostgreSQL + ai-chat (+ optional ai-architect-6-mcp profile)
- [ ] GitHub Actions CI workflow (`mvn test`, `mvn verify -Pintegration`)
- [ ] MCP health actuator indicator in `system/` module
- [ ] Manual smoke checklist documented and verified locally
- [ ] README quick-start with `docker compose up`

## Out of scope

- Kubernetes / cloud deployment
- Production secrets management
