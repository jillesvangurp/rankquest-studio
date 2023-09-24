Rankquest Studio is a web based tool that you can use to benchmark search query metrics for your search APIs. You can try the [latest version here](https://rankquest.jillesvangurp.com).

## More about Rankquest Studio

- [What is Rankquest Studio](src/jsMain/resources/about.md) - An introduction to the features of Rankquest Studio
- [Release Notes](https://github.com/jillesvangurp/rankquest-studio/releases) - I push out new versions regularly. This project is currently in Beta. Expect bugs and report them if you find them please. My goal is to stabilize the code base in the next months.
- [rankquest-core](https://github.com/jillesvangurp/rankquest-core) - The search metrics implementation used by Rankquest Studio.
- [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli) - Command line tool that lets you use your exported search configurations and test cases from the command line to run metrics. You can use this for example to integrate Rankquest Studio in your CI builds.
- [MIT License](LICENSE.md) - This project is open source.

## Technical details

This is a kotlin-js project that uses the amazing [Fritz2](https://www.fritz2.dev/) framework. Fritz2 is a reactive framework in the style of react and similar frameworks. But it also builds on top of foundations laid by Kotlin with things like co-routines, flows, and internal DSLs. 

This project currently uses the experimental K2 compiler (the new compiler for the upcoming Kotlin 2.0). While not yet recommended for production usage or library creation, the benfits of its faster incremental compilation and the fact that it seems to work outweigh the downsides of dealing with its occasional flakiness and remaining bugs. If this causes you issues, you can turn it off in `gradle.properties`. For the same reason, you should use the latest version of intellij for development.

The CSS styling is done using [tailwind](https://tailwindcss.com/). This works via a hook in `webpack.config.d` that processes javascript assets that are produced by the kotlin-js transpiler to ensure that any of the tailwind classes used are backed by the correct, minimum amount of CSS necessary.

An important detail is that tailwind uses simple string manipulation and therefore you should not attempt to manipulate these strings at runtime. Tailwind works at build time and any such manipulations may cause it to fail to generate the right css classes.

## Development and running Rankquest Studio locally

To run a local server, you will need a recent JVM (17 should be fine). The ./gradlew script (the gradle wrapper) takes
care of downloading the correct version of gradle.

To run a development server, simply run:

```bash
./gradlew jsBrowserDevelopmentRun -t
```

The `-t` option monitors the source code for changes and will trigger incremental compilation. Automatic reload is currently disabled (intentionally, I don't like the behavior) via webpack configuration. So, you need to manually reload after it recompiles.

## Deploying to a webserver

To produce production web assets that you can deploy to any webserver, run 

```bash
./gradlew jsBrowserProductionWebpack
```

This compiles everything and then packs everything up with webpack. All the assets in src/jsMain/resources are copied to the root along with the compiled source code.

Then simply copy over the files in `build/dist/js/productionExecutable/` to your web server.