# M-14 — Production Hardening

**Status:** Completed  
**Date:** 2026-06-17  
**Milestone:** Post-v1.0.0 maintenance

## Goal

Production readiness items not yet scheduled as SRS milestones.

## Deliverables

- [x] OIDC browser login profile (`oauth2-login`) — session login; web UI skips `X-User-Id`
- [x] Playwright streaming E2E with stub LLM (`e2e` profile + `chat-stream.spec.ts`)
- [x] Prometheus alerting rules (`observability/prometheus/alerts.yml`)
- [x] LICENSE (MIT) and `RELEASE.md` for v1.0.0
- [x] README synced with codebase

## Out of scope

- New SRS milestones unless requirements change
