Rankquest Studio is a web based tool that helps you evaluate search query metrics
in your search APIs.

[rankquest.jillesvangurp.com](https://rankquest.jillesvangurp.com)

## More about Rankquest Studio

- [What is Rankquest Studio](src/jsMain/resources/about.md)
- [Release Notes](https://github.com/jillesvangurp/rankquest-studio/releases)
- [rankquest-core](https://github.com/jillesvangurp/rankquest-core) - the search metrics implementation used by Rankquest Studio.
- [MIT License](LICENSE)

## Development and running Rankquest Studio locally

This is a kotlin-js project that uses the amazing [Fritz2](https://www.fritz2.dev/) framework. 
You will need the latest version of Intellij for development.

To run a local server, you will need a recent JVM (17 should be fine). Simply run:

```bash
./gradlew jsBrowserDevelopmentRun -t
```


