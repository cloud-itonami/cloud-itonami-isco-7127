# Governance

`cloud-itonami-isco-7127` is an OSS open-occupation blueprint. Governance
covers both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions or disclose records.
- HvacMechGovernor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- a refrigerant-system-service-execution decision, and any override or
  bypass of a technician-certification requirement, stay permanently
  outside this actor's op-allowlist.
- every commit, hold and approval path is auditable.
- real technician/service-account/operator data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification, license, or
the closed op-allowlist should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and data-flow
review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling technician/service-account/operator data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- attempting to route a refrigerant-system-service-execution decision,
  or a technician-certification-requirement override/bypass, through
  this actor
