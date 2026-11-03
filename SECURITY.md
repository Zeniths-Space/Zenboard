# Security

## Reporting a vulnerability

Please report security issues privately through GitHub Security Advisories
("Report a vulnerability" on the repository's Security tab). Do not open a public issue.

## Design

- The input method service is protected by `BIND_INPUT_METHOD`, so only the system can bind to it.
- The app requests no permissions.
- Release signing keys and passwords are never committed. They live in protected CI secrets.
