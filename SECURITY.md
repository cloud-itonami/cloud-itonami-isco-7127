# Security Policy

This project handles AC and refrigeration mechanics operating workflows.
Treat vulnerabilities as potentially high impact even when the demo data is
synthetic — this domain's failure modes include refrigerant exposure,
pressurized-system hazards and electrical hazards.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real technician, service-account or operator data exposure
- authorization bypass
- HvacMechGovernor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any path that lets a proposal reach a
  refrigerant-system-service-execution decision, or a
  technician-certification-requirement override/bypass decision

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on technician/service-account data, policy enforcement or audit
  logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real technician/service-account/operator data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
