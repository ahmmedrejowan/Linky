# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability in Linky, please report it responsibly:

1. **Do NOT** open a public issue
2. Email the maintainer directly or use GitHub's private vulnerability reporting
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

## Security Measures

Linky implements the following security measures:

- **Vault encryption** using AES-256-GCM via AndroidX Security Crypto
- **PIN protection** with PBKDF2 key derivation for vault access
- **Encrypted storage** for sensitive links in the vault
- **No network access** except for:
  - Fetching link previews (title, description, image)
  - Reader mode content parsing
  - Optional update checks to GitHub
- **FileProvider** for secure file sharing between apps
- **ProGuard/R8** obfuscation in release builds
- **No analytics or tracking**
- **No third-party SDKs** that collect user data

## Response Timeline

- **Acknowledgment**: Within a week
- **Initial Assessment**: Within 2 weeks
- **Fix Timeline**: Depends on severity and availability
  - Critical: As soon as possible
  - Others: Next release

## Scope

The following are in scope for security reports:

- Remote code execution
- Vault encryption bypass
- Data leakage (especially vault contents)
- Authentication/authorization bypass
- Privilege escalation
- Denial of service

Out of scope:

- Issues requiring physical device access
- Social engineering attacks
- Issues in third-party libraries (report to respective maintainers)
