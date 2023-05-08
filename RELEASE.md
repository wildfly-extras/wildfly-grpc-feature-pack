# Release Management

The bulk of work of releasing a new version is done by the [`./release.sh`](release.sh) script. This script takes care of bumping the versions, creating a tag and deploying to Maven Central.

```
USAGE:
    release.sh [FLAGS] <release-version> <next-version>

FLAGS:
    -h, --help          Prints help information
    -v, --version       Prints version information
    --no-color          Uses plain text output

ARGS:
    <release-version>   The release version (as semver)
    <next-snapshot>     The next snapshot version  (as semver)
```

## Prerequisites

- `<release-version>` and `<next-version>` _must_ be semantic versions following `major.minor.micro`. 
- `<next-version>` _must_ be greater than `<release-version>`
- there _must_ be no uncommitted changes
- there _must_ be no tag `v<next-version>`
- there _should_ be some entries in the [Unreleased](CHANGELOG.md#unreleased) section of the changelog. 

## What it does

1. Bump the version to `<release-version>.Final`
2. Update the changelog headings and internal links (there should already be entries in the [Unreleased](CHANGELOG.md#unreleased) section)
3. Create a tag for `v<release-version>`
4. Commit and push to upstream (which will trigger the [release workflow](.github/workflows/release.yml) at GitHub):
   1. Deploy to Maven Central
   2. Create a GitHub release with the relevant entries from the changelog
   3. [Announce](https://github.com/wildfly-extras/wildfly-grpc-feature-pack/discussions/categories/announcements) the release in the discussions
5. Bump the version to `<next-version>-SNAPSHOT`
6. Commit and push to upstream
