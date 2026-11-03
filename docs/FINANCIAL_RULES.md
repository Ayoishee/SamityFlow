# Financial workflows and data upgrades

## Agreed terms and approval

Submitting an application captures its product name, interest strategy/rate,
penalty strategy/rate, duration, guarantee count and weekly savings in
`application_terms`. A database trigger captures the terms in the same statement
that inserts the application. Capturing at submission is deliberately stronger
than freezing at approval: even an applicant collecting guarantees keeps the
original offer. Updates and deletion of snapshots are rejected by triggers.

Guarantees must come from distinct active members in the applicant's group and
cannot come from the applicant. Zero guarantees goes directly to officer review.
Officer approval precedes manager approval; eligibility is checked again at manager
approval. Disbursement requires approval and permits only one loan per application.
The loan references that application's immutable terms for its lifetime. Schedule
principal and interest are fixed at disbursement. Product edits affect new
applications, not agreements or overdue scans. Rejection and existing State/Chain
approval behavior are otherwise preserved.

Normal/agricultural/business interest is simple annual interest, prorated by
weeks / 52. Emergency interest is one flat percentage of principal. The persisted
charge is calculated with BigDecimal and rounded once to cents, HALF_UP. The
schedule factory distributes rounding residuals so its totals match the loan.

## Collection, allocation and penalties

A collection verifies installment/loan/member ownership, active/defaulted loan
status, positive payment, nonnegative savings, date, unique reference, and no
overpayment. Payment, balances, optional linked savings, and audit are one SQLite
transaction. A zero outstanding balance completes the loan.

The allocation policy is **interest first, principal second, penalties last**.
`payments.interest_paid`, `principal_paid`, and `penalty_paid` record the allocation
at posting time. Reversals record the negative allocation of their original payment.
The effective installment allocation is base-first against cumulative net paid;
a reversal of an earlier payment may therefore make base debt outstanding again.
Historical posting allocations remain history rather than being rewritten.

An overdue installment starts with week 1 the day after its due date; each seven
additional days increments the week. NORMAL charges every overdue week; GRACE
waives week 1. Each scan charges:

```
remainingBase = max(totalPrincipalAndInterest - cumulativeNetPaid, 0)
newWeeks = max(chargeableWeeks(asOfDate) - penaltyWeeksAlreadyAssessed, 0)
additionalPenalty = round(remainingBase * penaltyPercent / 100 * newWeeks, 2)
```

Additional penalties are nonnegative and add to the previously assessed amount;
repayment never cancels an assessed charge. Paying penalties cannot create a
negative base. No penalty is charged on penalties. Repeated or earlier scans add
no charge. At three overdue weeks the existing default/eligibility review rule
applies. Alerts are sent after the database commit.

This is a **scan-based policy**, not reconstruction of daily historical arrears:
if several weeks were not scanned, they are assessed against the remaining base
at the next scan. It does not backdate hypothetical charges that were never
assessed. This explicit assumption should be confirmed before production use.

## Savings and reversal

Savings balances use exact signed integer cents. A withdrawal acquires SQLite's
writer lock with a no-op member update **before** reading available funds, then
checks and records withdrawal plus audit on that same connection. Competing app
instances serialize; a second insufficient withdrawal fails. Connections have a
10-second busy timeout; lock exhaustion is an operation failure, never permission
to skip validation. The existing prototype's withdrawal policy remains available
funds only; no new active-loan exposure restriction is introduced.

Collection savings carry a unique nullable `payment_id` foreign key. Reversal
looks up only that key, never a matching reference. Standalone deposits remain
unlinked even if the member and reference match a payment. A reversing savings
withdrawal links to the original deposit through `reversal_of_transaction_id`.
Original records and audits remain intact. If linked savings were already spent,
the entire payment reversal fails for reconciliation rather than overdrawing
savings. Duplicate payment and savings reversals are rejected. Failure in any
ledger or audit step rolls back the entire operation.

## Transaction ownership

Services that open a connection own its transaction and close it. Disbursement also
accepts a supplied connection: an existing transaction gets a savepoint. Success
releases only that savepoint; failure rolls back only work since that savepoint.
Neither path commits unrelated caller work or closes the caller's connection.
An initially auto-commit connection receives a service transaction and has its
original setting restored on success or failure. Repositories using supplied
connections do not manage transactions.

## Money and SQLite precision

New savings entries persist both a compatibility `REAL amount` and canonical
`INTEGER amount_cents`. Balance authorization sums cents, not SQLite floating
point. Conversion uses BigDecimal HALF_UP and checked long conversion. SQLite
integer sum overflow fails rather than silently allowing a withdrawal.

Existing installment, loan, payment and rate columns remain SQLite REAL for
compatibility and preservation of historical data. JDBC BigDecimal does **not**
make REAL exact. Core accounting reads normalize those values to two decimals;
loan penalty SQL updates explicitly ROUND to cents. Calculations and schedules
use BigDecimal, and payment inputs round before posting. Double getters remain
for existing UI/API compatibility. Portfolio/report SUM queries over legacy REAL
can still have binary floating point residuals; reports are informational, not
withdrawal authorization. This prototype is not an arbitrary-precision banking
ledger; amounts beyond double's cent-level precision require a separate full
integer-ledger conversion before use. No historical monetary values are rewritten
as part of this patch.

## Upgrade and reconciliation

1. Stop all app instances. Back up `samityflow.db` (including WAL/SHM if present),
   preferably with SQLite's backup API, and retain the original backup.
2. Build/test with `mvn verify`. Start the application normally. Initialization adds
   schema, columns, indexes and triggers. Financial migration version 1 executes
   transactionally and is repeatable; existing original history and audits remain.
3. Inspect `SELECT * FROM migration_reviews ORDER BY category, record_id;` using
   a SQLite client. This is a persistent review queue, not a list automatically
   resolved by restarting the app.
4. Reconcile flagged records against source paperwork before resuming affected
   operations. Preserve evidence and add an audit record describing each approved
   reconciliation. Work on a backed-up copy first.

Legacy cases:

- **TERMS:** original agreed product terms cannot be recovered from today's product
  row. No snapshot is guessed. Disbursement/penalty scanning fails explicitly for
  loans lacking terms. Insert the verified complete row into `application_terms`
  for the application; it becomes immutable and is shared by the existing loan.
  UI application details show unknown terms until reconciled. Other data remains
  readable. Do not copy the current product merely to clear the blocker.
- **SAVINGS_LINK:** old entries remain unlinked. A reversal will leave these savings
  unchanged. A reviewer may set `payment_id` only with independent evidence of the
  original collection, same-member ownership, and no existing reversal/link. Text
  equality alone is not evidence, even for a single same-member match.
- **PENALTY:** existing nonzero penalties get `penalty_weeks_assessed=-1`; scanning
  fails rather than double-charging an unknown period. Verify the charged period
  and set the correct count of already-chargeable weeks before scanning again.
- **ALLOCATION:** old payment allocation columns remain NULL; historical allocation
  cannot be proven. Cumulative base-first allocation governs future scans; the
  old payment amount and history remain unchanged.
- **SAVINGS_ROUNDING:** existing REAL amounts retain their exact stored value.
  Canonical cents are initialized with SQLite ROUND (positive HALF_UP); entries
  with fractional cents are flagged for explicit reconciliation.
- **PAYMENT_REFERENCE / SAVINGS_REFERENCE:** duplicate historical references are
  preserved and flagged. A unique index is created only when existing references
  allow it; service-level duplicate checks still reject new duplicates. No suffix
  is silently appended to historical references.

Review flags persist after reconciliation for traceability. Removing a flag alone
is not reconciliation. A legacy database should not be reopened by older app
versions after migration: those versions do not write the canonical savings cents
or explicit links. Restore the entire pre-upgrade backup if a downgrade is needed.

## Verification

`mvn verify` runs the existing suite plus `FinancialIntegrityTest` and
`LegacyMigrationTest`. These exercise independent SQLite connections, reference
collisions, genuine linked reversals and rollback, partial penalty repayment,
repeated scans, concurrent insufficient withdrawals, immutable terms,
zero-guarantee approval, caller-owned transaction success/failure, and migration
against the pre-change schema including repeated execution.
