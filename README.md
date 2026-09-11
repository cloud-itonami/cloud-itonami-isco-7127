# cloud-itonami-isco-7127

Open Occupation Blueprint for **ISCO-08 7127**: Air Conditioning and Refrigeration Mechanics.

This repository designs a forkable OSS business for an AC and refrigeration service scheduling and logistics coordination practice: a service scheduling and supply-coordination robot manages technician/service-call records under a governor-gated actor, so an AC and refrigeration mechanic crew keeps its own operating records instead of renting a closed field-service-management SaaS.

**Maturity: `:implemented`.** `src/hvacmech/` implements the
`HvacMechActor` as a `langgraph.graph/state-graph`
(`hvacmech.actor`) wired to an `HVAC Mechanic Advisor`
(`hvacmech.advisor`) and an independent `HvacMechGovernor`
(`hvacmech.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 22 tests / 47 assertions green (`kbb -M:test`).
HARD invariants (always hold, never overridable):
technician provenance, service-account provenance, no-actuation
(`:effect` must be `:propose`), a closed op-allowlist
(`:log-service-record`, `:schedule-service-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any
proposal that would directly finalize a
refrigerant-system-service-execution decision (e.g. deciding to
proceed with a specific refrigerant-system service) or
override/bypass a technician-certification requirement. Always-escalate
paths (human sign-off regardless of confidence, mapping this repo's
Trust Controls in [`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a service scheduling/logistics coordination robot performs technician scheduling, service-call/parts-usage/diagnostic-status record logging and refrigerant/parts supply-order coordination for an AC and refrigeration mechanic crew, under an actor that proposes actions and an independent **HVAC Mechanic Governor** that gates them. The governor never
dispatches hardware itself, never performs refrigeration-system service, and never finalizes a refrigerant-system-service-execution decision or overrides/bypasses a technician-certification requirement; `:high`/`:safety-critical` actions (such as a flagged refrigerant-leak/pressure-anomaly/electrical-hazard concern, or an above-threshold supply order) require human sign-off. **This actor coordinates service scheduling/logistics only — it never performs refrigeration-system service itself.**

## Core Contract

```text
technician roster + service-account registration + safety-reporting policy
        |
        v
HVAC Mechanic Advisor -> HvacMechGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
a refrigerant-system-service-execution decision, override or bypass a
technician-certification requirement, suppress an operating record, or
disclose sensitive data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7127`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
