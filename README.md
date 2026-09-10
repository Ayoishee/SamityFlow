# SamityFlow - Group-Based Microfinance Approval, Collection & Savings System

A desktop **group microfinance workflow application** built with **JavaFX**, **Maven**, and **SQLite** for the Design Patterns final project.

The application manages samities, small member groups, loan applications, group guarantees, multi-stage approval, repayment schedules, weekly collections, compulsory savings, overdue monitoring, and auditable payment corrections. It is designed around configurable organizational policies rather than being limited to basic loan CRUD.

---

## Problem Statement

Small microfinance organizations and community lending programs may manage member groups, weekly collections, savings contributions, and loan approvals through paper registers or disconnected spreadsheets. This can make it difficult to verify guarantees, apply loan-product rules consistently, identify overdue installments, calculate savings balances, and preserve a reliable history of payment corrections.

SamityFlow addresses this problem by providing a single offline desktop application for the complete group-based lending workflow. A loan application passes through configurable guarantee and approval stages before disbursement. The application generates a repayment schedule according to the selected loan product, records weekly installment and savings collections, detects overdue accounts, and preserves every important financial action in an audit history.

Group size, required guarantees, repayment term, savings contribution, interest calculation, penalty calculation, and group-eligibility consequences are configurable because these policies can differ between organizations and loan products.

---

## Tech Stack

| Layer / Technology | Selection |
| --- | --- |
| Programming language | Java 17+ |
| User interface | JavaFX |
| Build and dependency management | Maven |
| Database | SQLite |
| Database access | JDBC |
| Testing | JUnit 5 |

---

## Core Features

- **Samity and group-unit management** - organize members into samities and smaller mutual-support groups
- **Member management** - maintain member profiles, status, group membership, savings, and loan history
- **Configurable loan products** - define repayment term, interest strategy, penalty strategy, savings contribution, and guarantee requirements
- **Group guarantee workflow** - record member guarantees before an application proceeds to formal approval
- **Multi-stage loan approval** - process applications through group verification, field-officer review, and manager approval
- **Loan disbursement** - generate and persist a product-specific repayment schedule after approval
- **Weekly collection** - collect installment payments and savings contributions from a single workspace
- **Savings management** - record deposits and controlled withdrawal requests
- **Overdue monitoring** - detect missed installments, apply the configured penalty rule, and place affected eligibility under review
- **Audited payment reversal** - correct an incorrect collection using a reversing transaction instead of deleting financial history
- **Reports and analytics** - analyze collection performance, overdue exposure, member savings, group risk, and officer activity

---

## Database Schema (Entities)

| Table | Purpose |
| --- | --- |
| `Users` | Field officers, managers, and administrators with application roles |
| `Samities` | Samity information, meeting schedule, assigned officer, and operational status |
| `GroupUnits` | Smaller member groups belonging to a samity |
| `Members` | Member profile, group membership, join date, and eligibility status |
| `LoanProducts` | Configurable interest, penalty, term, savings, and guarantee policies |
| `LoanApplications` | Requested amount, purpose, selected product, state, and approval trail |
| `Guarantees` | Group-member guarantee decisions for loan applications |
| `Loans` | Approved and disbursed loan accounts with principal and outstanding balance |
| `Installments` | Generated weekly dues, due dates, paid amounts, and installment states |
| `Payments` | Posted collections, allocations, reversals, references, and timestamps |
| `SavingsTransactions` | Compulsory deposits, voluntary deposits, withdrawals, and reversals |
| `AuditLogs` | Important state changes and financial actions with user and timestamp |

Primary keys, foreign keys, unique constraints, amount checks, state checks, and database transactions will preserve data consistency. Seeder scripts will create the schema and populate sample users, samities, members, loan products, applications, schedules, payments, and savings records.

---

## Multi-Step Workflows

1. **Loan Approval** - Create application -> validate member eligibility -> collect required group guarantees -> field-officer review -> manager approval or rejection.
2. **Disbursement** - Select approved application -> apply the loan-product calculation strategy -> generate the repayment schedule -> create the active loan -> record disbursement.
3. **Weekly Collection** - Open samity collection sheet -> select member -> validate due installment -> post repayment -> record savings contribution -> update balances -> produce audit entry.
4. **Payment Correction** - Locate incorrect payment -> enter reversal reason -> create reversing payment and savings records -> restore affected balances -> post corrected collection.
5. **Overdue Escalation** - Detect missed installments -> apply penalty strategy -> count consecutive overdue weeks -> generate alert -> place member or group eligibility under review according to policy.
6. **Savings Withdrawal** - Submit withdrawal request -> verify available balance and active-loan exposure -> approve or reject -> record transaction -> update savings balance.

---

## Important Business Rules

- A member must belong to an active samity and group unit before applying for a loan.
- A member cannot guarantee their own application.
- Duplicate guarantees from the same group member are not permitted.
- An application cannot advance until its configured guarantee requirement is satisfied.
- Only an approved application can be disbursed.
- A repayment schedule is fixed when the loan is disbursed; later product changes do not rewrite an existing schedule.
- Installment, payment, and savings amounts cannot be negative.
- A payment cannot be posted twice using the same collection reference.
- Financial corrections create reversal records; original payment records are never silently deleted.
- Savings withdrawals cannot exceed the available balance.
- A completed loan cannot accept additional installment payments.
- Eligibility restrictions caused by overdue loans are reviewable and determined by configurable organizational policy.

---

## Probable Design Patterns

These patterns address concrete workflow and maintainability problems. The final selection may be reduced if a pattern does not remain necessary during implementation.

| Pattern | Where it is used | Why it is appropriate |
| --- | --- | --- |
| **Strategy** | Interest and penalty calculation per loan product | Swappable calculation rules behind one interface; new products do not require changes to existing calculation code |
| **State** | Loan and installment lifecycle (Applied -> Active -> Defaulted -> Closed) | Status-dependent transitions become structurally impossible to violate instead of relying on scattered conditional checks |
| **Chain of Responsibility** | Loan approval pipeline (group -> officer -> manager) | Each approver can pass or halt the request without hardcoding the complete approval hierarchy |
| **Factory Method** | Installment schedule generation | Each loan product can create a structurally different repayment-schedule shape |
| **Observer** | Overdue, default, and low-savings alerts | One event can notify several independent listeners without coupling those listeners together |
| **Command**  | Payment collection at weekly meetings | Wraps each payment as an auditable action that can be safely corrected through a reversal command when a field-entry mistake occurs |

---

## Reports and Analytics

- Weekly collection sheet by samity
- Expected amount versus collected amount
- Due, paid, partial, and overdue installments
- Collection rate by field officer or samity
- Member loan and savings statement
- Consecutive-overdue and default-risk report
- Group units under eligibility review
- Loan-product performance comparison
- Savings deposits and withdrawals by period
- Payment reversal and financial audit report

---

## Major Screens

1. **Dashboard** - portfolio summary, weekly collection status, overdue alerts, and savings totals
2. **Samity & Member Management** - samities, group units, members, and guarantees
3. **Loan Workspace** - product selection, application, approval trail, and disbursement
4. **Weekly Collection** - installment collection, savings contribution, receipts, and reversals
5. **Savings Management** - member statements, deposits, and withdrawal requests
6. **Reports & Audit History** - operational reports, risk analysis, and financial action history

---

## Academic Scope

SamityFlow is an academic prototype and not a production banking platform. It will use one simulated branch, seeded loan products, local users, and manually recorded cash collections. It will not include mobile banking, credit-bureau integration, biometric authentication, SMS delivery, multi-currency accounting, or regulatory submission. Organizational policies will be configurable rather than presented as universal microfinance rules.

---

## Team

- **Member 1:** Asshifa Sultana - Roll: 1632
- **Member 2:** Prottasha Saha Ayoishee - Roll: 1656

---

*A group-based microfinance application where design patterns represent real approval, repayment, savings, correction, and eligibility rules rather than being added only for pattern count.*
