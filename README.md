Rankquest Studio is a web based tool that you can use to benchmark search relevance metrics for your search APIs. You can try the [latest version here](https://rankquest.jillesvangurp.com).

Learn how to [get started ](https://youtu.be/Nxr2UVs_n74?si=YKslAJbY7-BojcmB) with this short Youtube screen recording.

## Using Rankquest Studio is Easy

**Start optimizing your search ranking today**

- Open [Rankquest Studio](https://rankquest.jillesvangurp.com). There is no installation or signup process, all the data is stored in your browser. Of course, if you want to, you can also self host rankquest Studio. All you need is a simple web server to host its files.
- Create a search plugin configuration for your search API or play with the **demo configurations** that come with rankquest studio.
- Use the search tool to create some test cases. Enter your search and click the "Add Testcase" button to convert the results in a testcase. Tweak, tag, and edit them in the test cases screen.
- Run and explore metrics for your test cases from the metrics screen. 
- Export your configuration and testcases and use them on the command line using [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli).

## Search Relevance Metrics

Rankquest Studio allows you to fine-tune and assess the performance of your search engine, and helps you ensure 
that the most relevant results are surfaced to users. It includes several common ways to measure search relevance.

**Precision At K** and **Recall At K**,  measure the accuracy and completeness of the top search results, respectively. 
**Mean Reciprocal Rank** meausures the rank of the first relevant result and emphasizes the importance of top placements.
**Expected Reciprocal Rank** accounts for varying degrees of relevance among the results and ranks high rated results 
lower if they are ranked lower. 
**Discounted Cumulative Gain** evaluates the overall value of the search results based on their rankings and relevance.
Finally, **Normalized Discounted Cumulative Gain**, provides a normalized score of DCG between 0 and 1 to make it 
easier to compare measurements.

You can customize these metrics for your search configuration. For example, you might have 
different configurations for precision@3 and precision@10 to measure the precision for your top three 
results and the first page of results respectively. Additionally, you can configure thresholds for each of your
metrics to determine when they go from green to red. This allows you to set targets for your metrics. And of course
you can enforce these bu integrating [Rankquest CLI](https://github.com/jillesvangurp/rankquest-cli) into your continuous integration builds.

## More about Rankquest Studio

- [A brief introduction](https://youtu.be/Nxr2UVs_n74?si=YKslAJbY7-BojcmB) - Youtube screen recording.
- [What is Rankquest Studio](src/jsMain/resources/about.md) - An introduction to the features of Rankquest Studio that is also accessible from the app.
- [Rankquest Studio - Removing Barriers to Search Quality Testing](https://www.jillesvangurp.com/blog/2023-11-18-rankquest-studio.html) - A longer article I wrote about Rankquest Studio.
- [Release Notes](https://github.com/jillesvangurp/rankquest-studio/releases) - I push out new versions regularly.
- [rankquest-core](https://github.com/jillesvangurp/rankquest-core) - The search metrics implementation used by Rankquest Studio.
- [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli) - Command line tool that lets you use your exported search configurations and test cases from the command line to run metrics. You can use this for example to integrate Rankquest Studio in your CI builds.
- [MIT License](LICENSE.md) - This project is open source.

## Technical details

This is a kotlin-js project that uses the amazing [Fritz2](https://www.fritz2.dev/) framework. Fritz2 is a reactive framework in the style of react and similar frameworks. But it also builds on top of foundations laid by Kotlin with things like co-routines, flows, and internal DSLs. 

This project currently uses the new K2 compiler and Kotlin 2.0. While not yet recommended for production usage or library creation, the benfits of this are faster incremental compilation and the fact that it seems to work outweigh the downsides of dealing with its occasional flakiness and remaining bugs. If this causes you issues, you can turn it off in `gradle.properties`. For the same reason, you should use the latest version of intellij for development.

The CSS styling is done using [tailwind](https://tailwindcss.com/). This works via a hook in `webpack.config.d` that processes javascript assets that are produced by the kotlin-js transpiler to ensure that any of the tailwind classes used are backed by the correct, minimum amount of CSS necessary.

An important detail is that tailwind uses simple string manipulation and therefore you should not attempt to manipulate these strings at runtime. Tailwind works at build time and any such manipulations may cause it to fail to generate the right css classes.

## Development and running Rankquest Studio locally

If you want to add your own custom plugins to Ranquest Studio, or want to tinker with it, simply check out the source code and start editing. 

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

This compiles everything and then packs everything up with webpack. All the assets in src/jsMain/resources are copied to the root along with the compiled source code in javascript form and an `index.html` file. All the styling is done using [tailwind CSS](https://tailwindcss.com/).

Then simply copy over the files in `build/dist/js/productionExecutable/` to your web server.