# Smart Banking System — Scrum & Agile Documentation

**Project:** Smart Community Digital Wallet (TT-BANK)
**Authors:** Tanke Michel & Tim Chantal
**Methodology:** Scrum (Agile)
**Sprint Duration:** 2 weeks
**Total Sprints:** 4 (Sprints 1–2 covered in detail below)

---

## 1. Scrum Team Roles

| Role | Person | Responsibilities |
|---|---|---|
| **Product Owner** | Tanke Michel | Defines and prioritises the product backlog; represents stakeholder interests; accepts or rejects deliverables at each sprint review; ensures the team builds the right product |
| **Scrum Master** | Tim Chantal | Facilitates all Scrum ceremonies; removes impediments blocking the team; coaches on Scrum practices; shields the team from external interruptions |
| **Development Team** | Tanke Michel, Tim Chantal | Designs, develops, tests, and deploys the system; self-organising; cross-functional (frontend, backend, DevOps) |

---

## 2. Definition of Done

A user story is considered Done when:
- All acceptance criteria are met
- Unit tests are written and pass (`./mvnw test`)
- Code coverage ≥ 80% on the changed module (`./mvnw verify`)
- The feature runs correctly with the Docker Compose stack
- Code is merged to the `main` branch via a reviewed pull request
- The relevant OpenAPI specification is updated

---

## 3. Product Backlog

Stories are sized using **Fibonacci story points** (1, 2, 3, 5, 8, 13).
Priority: **Critical** → **High** → **Medium** → **Low**.

| ID | User Story | Points | Priority | Status |
|---|---|---|---|---|
| US-01 | As a user, I can register with full name, email, phone and password so that I have an account | 5 | Critical | Done |
| US-02 | As a user, I receive an OTP email and can verify my account so that it becomes active | 5 | Critical | Done |
| US-03 | As a user, I can log in with email and password and receive JWT tokens so that I am authenticated | 3 | Critical | Done |
| US-04 | As a user, I can refresh my access token without re-logging in so that my session persists | 2 | Critical | Done |
| US-05 | As a user, I can log out and have my token invalidated so that my session is secure | 2 | High | Done |
| US-06 | As a user, I can request a password reset OTP and reset my password so that I can recover access | 5 | High | Done |
| US-07 | As a user, I can change my password while authenticated so that I can update my credentials | 2 | High | Done |
| US-08 | As a user, I can create a digital wallet in XAF so that I can store and transact money | 5 | Critical | Done |
| US-09 | As a user, I can deposit funds into my wallet so that my balance increases | 3 | Critical | Done |
| US-10 | As a user, I can withdraw funds from my wallet so that I can access my money | 3 | Critical | Done |
| US-11 | As a user, I can transfer money to another user by their email so that I can send payments | 8 | Critical | Done |
| US-12 | As a user, I can view my complete transaction history so that I track my spending | 3 | High | Done |
| US-13 | As a merchant, I can register my business with wallet details so that I can receive payments | 8 | High | Done |
| US-14 | As a merchant, I can generate a QR code so that customers can scan and pay me | 5 | High | Done |
| US-15 | As a customer, I can scan a merchant QR code and pay so that I can make purchases | 5 | High | Done |
| US-16 | As a merchant, I can view my revenue dashboard so that I track my earnings | 3 | Medium | Done |
| US-17 | As a user, I can create a community savings group (tontine) so that my community saves together | 13 | High | Done |
| US-18 | As a user, I can join an existing savings group with my wallet so that I participate | 3 | High | Done |
| US-19 | As a group member, I can make my fixed contribution so that the round pool grows | 5 | High | Done |
| US-20 | As a group admin, I can trigger payout to the next eligible member so that the tontine rotates | 5 | High | Done |
| US-21 | As a user, I receive email notifications for all transactions and group events | 8 | High | Done |
| US-22 | As an admin, I can view the full audit log of all banking events | 5 | High | Done |
| US-23 | As an admin, I can approve, suspend or reject merchant accounts | 3 | High | Done |
| US-24 | As an admin, I can suspend or reactivate user wallets | 2 | Medium | Done |
| US-25 | As a developer, the system is containerised with Docker so it runs identically everywhere | 8 | Critical | Done |
| US-26 | As a developer, a CI/CD pipeline auto-builds and deploys on every push to main | 8 | High | Done |
| US-27 | As a developer, all services are monitored with Prometheus and Grafana | 8 | High | Done |
| US-28 | As a developer, the infrastructure is deployable with Ansible playbooks | 5 | High | Done |
| US-29 | As a developer, Kubernetes manifests allow scalable orchestrated deployment | 8 | High | Done |
| US-30 | As a developer, test coverage meets ≥ 80% across all services | 5 | Critical | Done |

**Total backlog points:** 167

---

## 4. Sprint 1 — Authentication & Wallet Core

**Sprint Goal:** Deliver a working authentication system and basic wallet operations so users can register, verify, log in, and manage funds.

**Dates:** Week 1–2 of development

**Sprint Backlog:**

| ID | Story | Points | Assignee | Status |
|---|---|---|---|---|
| US-01 | User registration | 5 | Tanke Michel | Done |
| US-02 | OTP email verification | 5 | Tim Chantal | Done |
| US-03 | User login + JWT | 3 | Tanke Michel | Done |
| US-04 | Token refresh | 2 | Tim Chantal | Done |
| US-05 | Logout + blacklist | 2 | Tanke Michel | Done |
| US-08 | Wallet creation | 5 | Tanke Michel | Done |
| US-09 | Deposit funds | 3 | Tim Chantal | Done |
| US-10 | Withdraw funds | 3 | Tim Chantal | Done |
| US-25 | Docker Compose (infra) | 8 | Tim Chantal | Done |

**Sprint 1 Committed Points:** 36
**Sprint 1 Velocity (completed):** 36

**Sprint 1 Daily Burndown Data:**

| Day | Remaining Points |
|---|---|
| Day 0 (start) | 36 |
| Day 1 | 34 |
| Day 2 | 31 |
| Day 3 | 28 |
| Day 4 | 24 |
| Day 5 | 21 |
| Day 6 | 18 |
| Day 7 | 14 |
| Day 8 | 10 |
| Day 9 | 6 |
| Day 10 (end) | 0 |

**Sprint 1 Review — What was demonstrated:**
- User registration with immediate OTP dispatch
- Email verification enabling the account
- Login returning access + refresh JWT pair
- Wallet creation and balance query
- Deposit and withdrawal with business rule validation
- Full Docker Compose stack running all infrastructure

**Sprint 1 Retrospective:**

*What went well:*
- JWT authentication design was solid from the start; no rework needed
- The GatewayAuthenticationFilter approach (gateway injects headers, services trust them) simplified security across all services significantly
- Docker Compose health checks prevented race conditions during startup

*What could be improved:*
- Redis was initially misconfigured for password-protected mode; cost 0.5 days
- Test coverage was written after the code rather than alongside it; will fix in Sprint 2
- The docker-compose init-db.sql path was wrong relative to the compose file; fixed

*Action items for Sprint 2:*
- Write tests alongside feature code, not after
- Set up JaCoCo from the start of each new service
- Ensure `application-test.properties` is created before the first test is written

---

## 5. Sprint 2 — Transfers, Merchants & Savings

**Sprint Goal:** Deliver P2P transfers, QR merchant payments, community savings groups, and async notifications so the core fintech features are complete.

**Dates:** Week 3–4 of development

**Sprint Backlog:**

| ID | Story | Points | Assignee | Status |
|---|---|---|---|---|
| US-06 | Password reset via OTP | 5 | Tim Chantal | Done |
| US-07 | Change password | 2 | Tim Chantal | Done |
| US-11 | P2P transfer | 8 | Tanke Michel | Done |
| US-12 | Transaction history | 3 | Tanke Michel | Done |
| US-13 | Merchant registration | 8 | Tim Chantal | Done |
| US-14 | QR code generation | 5 | Tim Chantal | Done |
| US-15 | QR merchant payment | 5 | Tanke Michel | Done |
| US-17 | Savings group creation | 13 | Tanke Michel | Done |
| US-18 | Join savings group | 3 | Tim Chantal | Done |
| US-19 | Make contribution | 5 | Tanke Michel | Done |
| US-21 | Email notifications (all events) | 8 | Tim Chantal | Done |

**Sprint 2 Committed Points:** 65
**Sprint 2 Velocity (completed):** 65

**Sprint 2 Daily Burndown Data:**

| Day | Remaining Points | Ideal |
|---|---|---|
| Day 0 (start) | 65 | 65 |
| Day 1 | 63 | 58.5 |
| Day 2 | 58 | 52.0 |
| Day 3 | 51 | 45.5 |
| Day 4 | 45 | 39.0 |
| Day 5 | 38 | 32.5 |
| Day 6 | 32 | 26.0 |
| Day 7 | 24 | 19.5 |
| Day 8 | 16 | 13.0 |
| Day 9 | 8 | 6.5 |
| Day 10 (end) | 0 | 0 |

**Sprint 2 Review — What was demonstrated:**
- P2P transfer between two users with balance validation and daily limit enforcement
- Merchant registration, QR code generation (ZXing PNG), and QR payment flow
- Community savings group creation and member join
- Contribution debit and payout credit cycle
- End-to-end email notifications for all 14 event types via RabbitMQ
- Admin audit log capturing every event across all domains

**Sprint 2 Retrospective:**

*What went well:*
- The RabbitMQ event-driven design scaled elegantly — adding the Audit Service required zero changes to existing services
- Community savings group (tontine) logic was the most complex feature; breaking it into entity→repository→service→controller layers made it manageable
- ZXing QR code generation worked first attempt; no integration issues

*What could be improved:*
- The savings payout logic initially had a race condition when multiple members contributed simultaneously; resolved with database-level constraints
- Notification service emails were not templated; plain-text emails sent. HTML templates are a future enhancement
- Sprint 2 started slow (Days 1–3 behind ideal) due to RabbitMQ topology design discussions

*Action items for Sprint 3:*
- Add full DevOps layer: Dockerfiles for all services, Kubernetes manifests, Ansible playbooks
- Set up Jenkins CI/CD pipeline
- Configure Prometheus + Grafana monitoring
- Add JaCoCo to all pom.xml files and verify coverage meets 80%

---

## 6. Sprint 3 — DevOps & Infrastructure (Summary)

**Sprint Goal:** Complete containerisation, orchestration, CI/CD pipeline, monitoring, and infrastructure as code.

**Key stories completed:** US-25 (full Docker), US-26 (Jenkins), US-27 (Prometheus/Grafana), US-28 (Ansible), US-29 (Kubernetes), US-30 (JaCoCo coverage)

**Points:** 42 | **Velocity:** 42

---

## 7. Sprint 4 — Documentation & Polish (Summary)

**Sprint Goal:** Complete API documentation, README, user manual, architecture document, and Scrum artefacts.

**Key stories completed:** US-22, US-23, US-24 (admin features), API documentation, README, OpenAPI spec, user manual.

**Points:** 24 | **Velocity:** 24

---

## 8. Project Velocity Summary

| Sprint | Committed | Completed | Velocity |
|---|---|---|---|
| Sprint 1 | 36 | 36 | 36 |
| Sprint 2 | 65 | 65 | 65 |
| Sprint 3 | 42 | 42 | 42 |
| Sprint 4 | 24 | 24 | 24 |
| **Total** | **167** | **167** | **Avg: 41.75/sprint** |

---

*Documentation prepared in accordance with the Scrum Guide (Schwaber & Sutherland, 2020).*
