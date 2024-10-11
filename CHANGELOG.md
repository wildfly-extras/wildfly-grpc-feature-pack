# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.9] - 2024-10-11

## [0.1.8] - 2024-10-11

## [0.1.7] - 2024-10-11

## [0.1.6] - 2024-10-11

## [0.1.5] - 2024-10-11

## [0.1.4] - 2024-10-11

### Fixes

- Update build to detect if running less than Java 17 and report an error

### Added

- Adaptation of feature pack so it can be pulled into WildFly Preview
- Move the stability level of the extension and schema to Preview 
- Add support for socket bindings
- All attributes should have defaults if these are implied from the underlying API defaults.

### Upgrades

- Bump io.grpc:grpc-bom from 1.65.1 to 1.68.0


## [0.1.3] - 2024-01-26

### Fixed

- #193: Add missing dependency and module to `com.squareup.okhttp3` after upgrading to WildFly 31

## [0.1.2] - 2023-07-21

### Added

- Galleon layers metadata
- Enhanced documentation

### Upgrades

- Bump WildFly Core to 21.1.0.Final
- Bump WildFly to 29.0.0.Final
- Bump Galleon to 5.2.0.Final
- Bump gRPC to 1.56.1
- Bump JBoss Logging to 3.5.2.Final
- Bump WildFly Maven plugin to 4.2.0.Alpha1
- Bump Jandex to 3.1.2
- Bump Arquillian to 1.7.0.Final

## [0.1.1] - 2023-05-08

### Added

- Add a maven profile `quick-build` that skips (IT) test, formatting, license check, checkstyle and enforcer. It runs the goals `clean install` by default and can be activated by specifying the system property `quickly`: `mvn -Dquickly` 

### Upgrades

- Bump arquillian-junit-core from 1.6.0.Final to 1.7.0.Final

### Fixed

- Fix deprecated license-maven-plugin configuration
- Exclude test suite from deployment
- Fix script to bump the version number

## [0.1.0] - 2023-04-28

This is our first feature complete release of the WildFly gRPC feature pack 🍾🎉🍻. 
Lots of new features, bug fixes and dependency upgrades went into this release. 

### Added

- Add the io.grpc module as it will be removed from WildFly (#78)
- Add to documentation
- Extended treatment of TLS
- Support for one-way and two-way SSL/TLS connections
- Implemented two attributes: "wildfly-grpc-server-port" and "wildfly-grpc-server-host"
- Add formatting of POM files.
- Add version for galleon-maven-plugin
- Add configuration parameters
- Add examples/helloworld/client/src/main/resources/client.keystore.pem
- Add attribute: shutdown-timeout
- Add SSL options to chat example

### Changed

- Migrate to using the `org.wildfly.tools:wildfly-parent` for the parent module (#87)
- Enable provisioning for the examples instead of relying on a server built in the test suite. Remove the standalone.xml.* pre-configured files in favor of using CLI to configure servers. (#91)
- Update documentation

### Fixed

- Adjust release script

### Removed

- Remove unneeded configuration options. Use values in the subsystem tests. Rename the attributes as the wildfly-grpc prefix should be implied by being in WildFly in the gRPC subsystem.
- Removed references to grpc-api; 0.0.7-0.0.8
- Do not import the WildFly component matrix as in WildFly 28 it's gone. Clean up dependencies no longer required.

### Upgrades

- Bump grpc-bom from 1.49.1 to 1.49.2
- Bump wildfly-maven-plugin from 4.0.0.Beta3 to 4.0.0.Final
- Bump wildfly-galleon-maven-plugin from 6.1.0.Final to 6.4.1.Final
- Bump WildFly from 26.1.0.Final to 27.0.1.Final
- Bump WildFly Core from 19.0.1.Final to 20.0.1.Final
- Bump grpc-bom from 1.49.1 to 1.54.1
- Bump galleon-maven-plugin from 5.0.8.Final to 5.0.9.Final

## [0.0.3] - 2022-09-21

### Added

- Enhance [community standards](https://github.com/wildfly-extras/wildfly-grpc-feature-pack/community)
    - Add [CODEOWNERS](CODEOWNERS)
    - Add [SECURITY.md](SECURITY.md)

### Changed

- Update documentation

### Upgrades

- Bump grpc-bom from 1.49.0 to 1.49.1
- Bump wildfly-galleon-maven-plugin from 6.0.0.Final to 6.1.0.Final

## [0.0.2] - 2022-09-14

### Added

- Deploy to Maven Central

## 0.0.1 - 2022-09-14

### Added

- Setup release workflow
- Add change log
- Add code of conduct
- Add contribution guide

<!--
## Template

### Added

- for new features

### Changed

- for changes in existing functionality

### Fixed

- for any bug fixes

### Security

- in case of vulnerabilities

### Deprecated

- for soon-to-be removed features

### Removed

- for now removed features

### Upgrades

- for dependency upgrades
-->

[Unreleased]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.1.9...HEAD
[0.1.9]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.1.8...v0.1.9
[0.1.8]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.1.7...v0.1.8
[0.1.7]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.1.6...v0.1.7
[0.1.6]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.1.5...v0.1.6
[0.1.5]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.0.3...v0.1.0
[0.0.3]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/wildfly-extras/wildfly-grpc-feature-pack/compare/vTemplate...v0.0.1
