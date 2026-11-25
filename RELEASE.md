# Release Procedure

Releases are published to Maven Central

From a clean checkout of `main`, run the release helper with the release version and the next development version (without `.Final` or `-SNAPSHOT`):

```shell
./release.sh <release-version> <next-version>
```

For example, `./release.sh 0.2.2 0.2.3` uses Maven Release Plugin to prepare and tag `0.2.2.Final` as `v0.2.2`, updates `main` to `0.2.3-SNAPSHOT`, then deploys the tag to Maven Central. Monitor the deployment at the [Central Portal](https://central.sonatype.com/publishing/deployments).

## Prerequisites

- `<release-version>` and `<next-version>` _must_ be semantic versions following `major.minor.micro` (no `.Final` please). 
- `<next-version>` _must_ be greater than `<release-version>`
- there _must_ be no uncommitted changes
- there _must_ be no tag `v<next-version>`
- `CHANGELOG.md` must be up to date.