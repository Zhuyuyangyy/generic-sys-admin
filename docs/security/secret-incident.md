# Secret Incident — MiniMax API Key

Status: **CODE REMEDIATED / CREDENTIAL ROTATION REQUIRES OWNER ACTION**

The key below is referred to in masked form only. It is never printed in full in
this repository, in the terminal transcript used to prepare this document, or in
any issue or pull request.

## Summary

| Field | Value |
|---|---|
| Secret type | MiniMax API key (third-party LLM/TTS provider) |
| Masked value | `sk-cp****oFK3yI` |
| Provider | MiniMax (api.minimax.chat) |
| Affected path | `backend/src/main/resources/application-dev.yml` |
| First affected commit | `a6d09be` — "Phase 1-2: JWT secret mandatory, getCurrentUserId() fixed" |
| Last affected commit | `686f894` — "security: 移除application-dev.yml中的硬编码MiniMax API Key" |
| Removed in | `686f894` |
| Reachable from `origin/master` | yes, both commits are ancestors of `11139eb` |
| Present in current HEAD (`stabilize-from-master`) | **no** |
| Present in `git log -S` on `origin/master` | **yes** — two commits |
| Credential revoked | **UNKNOWN — requires owner action** |
| History rewritten | **NO** (deliberately not done this round) |

## Scope

`git log -S '<key>' origin/master --oneline` returns exactly two commits:

```
a6d09be  Phase 1-2: JWT secret mandatory, getCurrentUserId() fixed
686f894  security: 移除application-dev.yml中的硬编码MiniMax API Key
```

The second is the removal commit. Several files were touched by that pair of
commits; only `application-dev.yml` contained the key. `backend/run_dev.bat` and
`frontend/.env.development` were touched in the same range but verified clean.

The repository has been public, so this key must be treated as **COMPROMISED**.
Anyone could have cloned it at any point since `a6d09be`.

## Current code state (remediated)

- `application.yml` and `application-dev.yml` both read `${MINIMAX_API_KEY:}`.
  There is no hard-coded key, no real-secret fallback and no default production
  key anywhere in `backend/src/main`.
- An unset key resolves to an empty string, so the application context still
  starts. TTS/AI features that need the key fail at call time rather than
  preventing startup.
- `.gitleaks.toml` on this branch does **not** allowlist this key. The one place
  it was referenced in an earlier branch's config was removed: writing even a
  prefix of a live key into the repo would itself be a leak.

## Required action — credential rotation

Deleting code does not un-leak a secret. Revocation is the only thing that makes
the exposure stop mattering, and it cannot be done from here because this
environment has no MiniMax console access.

**Do not assume this has been done.** The owner must:

1. Sign in to the MiniMax console.
2. Revoke the key introduced in `a6d09be`.
3. Generate a replacement key.
4. Distribute it only through the deployment environment's `MINIMAX_API_KEY`.
5. Rotate anything else that shared the key.

Until step 2 is confirmed, treat the old key as live and usable by third parties.

## Recommended history cleanup (NOT executed)

Rewriting history is deferred because it rewrites commit SHAs, breaks every
existing clone and every branch that has not been rebased, and requires a force
push to a shared branch. That is an owner decision, not a side effect of a
fix branch.

Once the key is confirmed revoked, either option below is acceptable. Both start
from a clean clone so the old objects are gone locally too.

### Option A — full history rewrite (preferred once approved)

```bash
# 1. Fresh clone so the only refs are the ones you keep
git clone --no-local /path/to/repo erms-clean && cd erms-clean

# 2. Create a replacement file (one match per line)
cat > replacements.txt <<'EOF'
sk-cp****oFK3yI==>***REMOVED-MINIMAX-API-KEY***
EOF

# 3. Rewrite every ref, then drop the old objects
git filter-repo --replace-text replacements.txt --force

# 4. Force-push every branch and tag
git push --force --all origin
git push --force --tags origin

# 5. Nothing useful survives in reflogs after this
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

Then every collaborator must re-clone, and any open branch must be rebased onto
the rewritten base.

### Option B — leave history, keep scanning new commits

Leave the two historical commits alone, document the exposure here (this file),
and restrict the CI secret scan to commits introduced by a pull request. The
history keeps a revoked key in it forever, but nothing new is added and every
consumer knows the key is dead.

This branch takes neither option; it only records the incident.

## Notes for the secret-scan job

`gitleaks-action@v2` with `fetch-depth: 0` scans full history, so it will keep
reporting this key on any branch that can reach `a6d09be`. That is the scanner
being correct, not a CI defect. Do not silence it with an allowlist for this
key; if it must be temporarily scoped, narrow the scan to the PR's commits and
say so explicitly.
