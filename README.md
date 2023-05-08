# SimpleCI
A Gradle Plugin used for simple (Minecraft mod) CI configuration

## Configuration
Using the SimpleCI-generated version is as simple as adding the following line to your buildscript:
```gradle
version = versioning.calculatedVersion
```

## Versioning rules
This plugin is tag-based. It will query the latest tag as a `major.minor.patch-(alpha/beta.number)` with only the major being required. Examples of valid tags:
- `46`, `46.0`, `46.0.0`
- `45-alpha`, `45-beta.2`

Then, the commit tree will be walked until it hits the latest commit. For every encountered commit, ignoring commits with `[noci]` in the message, a version number will be modified, as follows, in the following order:
- if the commit message contains `[minor]`, the major will be bumped;
- if the commit message contains `[beta]`, the beta number will be increased, resetting the alpha version;
- if the commit message contains `[alpha]`, the alpha number will be increased, resetting the beta version;
- otherwise, the patch version will be bumped.

Examples:
- given the following commits, from latest to oldest, starting from the `5.0` tag, the version will be `5.0.3-alpha.1`
 * `[alpha] Add this other experimental feature`
 * `[alpha] Add a new experimental feature`
 * `Fix the third bug`
 * `Fix the second bug`
 * `Fix the first bug`
 
 ## The `configureTeamCity` task
 The plugin registers a `configureTeamCity` task which should be configured to be ran by TeamCity in order to set the build number to the project version.
