# Release Management

The bulk of work of releasing a new version is done by the [`./release.sh`](release.sh) script. This script takes care of bumping the versions, creating a tag and deploying to JBoss Nexus.

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

Example:

```shell
./release.sh 1.2.3 1.2.4
```

## Prerequisites

- `<release-version>` and `<next-version>` _must_ be semantic versions following `major.minor.micro`. 
- `<next-version>` _must_ be greater than `<release-version>`
- there _must_ be no uncommitted changes
- there _must_ be no tag `v<next-version>`
- there _should_ be some entries in the [Unreleased](CHANGELOG.md#unreleased) section of the changelog. 

## What it does

1. Bump the version to `<release-version>.Final`
2. Update the changelog (there should already be entries in the [Unreleased](CHANGELOG.md#unreleased) section)
3. Create a tag for `v<release-version>`
4. Commit and push to origin and upstream (which will trigger the [release workflow](.github/workflows/release.yml) at GitHub):
   1. Deploy to JBoss Nexus `wildfly-extras-staging`
   2. Create a GitHub release with the relevant entries from the changelog
5. Bump the version to `<next-version>-SNAPSHOT`
6. Commit and push to origin and upstream

## What it doesn't do

The release script deploys to JBoss Nexus `wildfly-extras-staging`. It doesn't deploy to Maven Central. You have to make sure that there are no validation errors for `wildfly-extras-staging`. **Only then** you can move the artifacts to `wildfly-extras` (which will trigger the deployment to Maven Central): 

```shell
mvn nxrm3:staging-move
```

Otherwise use 

```shell
mvn nxrm3:staging-delete
```

to delete the artifacts from `wildfly-extras-staging`.

See also https://docs.google.com/document/d/1-6_vmFWzRVuMRwSuv_nOgG530qSDe6Ipc_eCERv6lrE/edit?usp=sharing
