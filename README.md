Rankquest Studio is a web based tool that you can use to benchmark search query metrics for your search APIs. You can try the [latest version here](https://rankquest.jillesvangurp.com).

## More about Rankquest Studio

- [What is Rankquest Studio](src/jsMain/resources/about.md) - An introduction to the features of Rankquest Studio
- [Release Notes](https://github.com/jillesvangurp/rankquest-studio/releases) - I push out new versions regularly. This project is currently in Beta. Expect bugs and report them if you find them please. My goal is to stabilize the code base in the next months.
- [rankquest-core](https://github.com/jillesvangurp/rankquest-core) - The search metrics implementation used by Rankquest Studio.
- [MIT License](LICENSE.md) - This project is open source.

## Technical details

This is a kotlin-js project that uses the amazing [Fritz2](https://www.fritz2.dev/) framework. Fritz2 is a reactive framework in the style of react and other frameworks. But it also builds on top of foundations laid by Kotlin with things like co-routines, flows, and internal DSLs.

This project currently uses the experimental K2 compiler (the new compiler for the upcoming Kotlin 2.0). While not yet recommended for production usage or library creation, the benfits of its faster incremental compilation and the fact that it seems to work outweigh the downsides of dealing with occasional flakiness. If this causes you issues, you can turn it off in `gradle.properties`. For the same reason, you should use the latest version of intellij. For development.

The styling is done using [tailwind](https://tailwindcss.com/). It works via a hook in `webpack.config.js` that processes javascript assets that are produced by the kotlin-js transpiler to ensure that any of the tailwind classes used are backed by the correct, minimum amount of CSS necessary. As of now there is no auto complete support for tailwind with kotlin-js.

An important detail is that tailwind uses simple string manipulation and therefore you should not attempt to manipulate these strings at runtime. Tailwind works at build time and any such manipulations may cause it to fail to generate the right css classes.

## Development and running Rankquest Studio locally

To run a local server, you will need a recent JVM (17 should be fine). Simply run:

```bash
./gradlew jsBrowserDevelopmentRun -t
```

The `-t` option monitors the source code for changes and will trigger incremental compilation. Automatic reload is currently disabled (intentionally, I don't like the behavior). So, you need to manually reload.

## Deploying to a webserver

To produce production web assets that you can deploy to any webserver, run 

```bash
./gradlew jsBrowserProductionWebpack
```

This compiles everything and then packs everything up with webpack. All the assets in src/jsMain/resources are copied to the root along with the compiled source code.

Then simply copy over the files in `build/dist/js/productionExecutable/` to your web server.

