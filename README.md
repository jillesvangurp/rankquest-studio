Rankquest Studio is a web based tool that you can use to benchmark search relevance metrics for your search APIs. You can try the [latest version here](https://rankquest.jillesvangurp.com).

Learn how to [get started ](https://youtu.be/Nxr2UVs_n74?si=YKslAJbY7-BojcmB) with this short Youtube screen recording.

## Why search relevance matters

Most websites and apps have search functionality. There are all sorts of ways to implement this. There are managed solutions such as Algolia that are easy to integrate. You can use off the shelf packages like Opensearch, Solr, or Elasticsearch. And there are various databases that integrate search functionality. Additionally, there is a growing amount of products that support vector search and other AI based approaches. But how do you decide which is best for your use case? Where do you start?  
Whichever solution you use, search relevancy is about delivering the best results possible to your users. And of course, you are competing against others that are trying to do that as well. Your competitiveness depends on how relevant your search results are. And in order to optimize that, you need to be able to test and measure search relevance. Doing so helps you drive your product roadmap, take informed decisions about where to focus your efforts, and drive the quality of your search in a data driven way.

## How does Rank Quest Studio work?

Rankquest Studio helps you build a test suite of rated searches. A rated search is a search query and a list of results with ratings that determine how relevant each result is. To measure search quality, Rankquest Studio runs the queries against your search API and calculates various search relevance metrics using the results that come back and comparing those to your rated results. If a particular result with a high rating is missing or ranked low, that negatively affects the metrics and if they are where they should be the metrics go up.

## Using Rankquest Studio is Easy

**Start optimizing your search ranking today**

- Open [Rankquest Studio](https://rankquest.jillesvangurp.com). There is no installation or signup process. Of course, if you want to, you can also self host Rankquest Studio. All you need is a simple web server to host its files. There is no database, no server, etc. Everything happens in your browser. This makes Rankquest both very easy to use and very safe to use.
- Create a search plugin configuration for your search API or play with the **demo configurations** that come with Rankquest Studio.
- Use the search tool to create some test cases. Enter your search and click the "Add Testcase" button to convert the results in a testcase. Tweak, tag, and edit them in the test cases screen.
- Run and explore metrics for your test cases from the metrics screen. 
- Export your configuration, testcases, and test runs and use them on the command line using [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli). All data is stored in simple json format that is easy to work with and process. 

## Search Relevance Metrics

Rankquest Studio allows you to fine-tune and assess the performance of your search engine, and helps you ensure 
that the most relevant results are surfaced to users. It includes several common ways to measure search relevance.

**Precision At K** and **Recall At K**,  measure the accuracy and completeness of the top search results, respectively. 
**Mean Reciprocal Rank** measures the rank of the first relevant result and emphasizes the importance of top placements.
**Expected Reciprocal Rank** accounts for varying degrees of relevance among the results and ranks high rated results 
lower if they are ranked lower. 
**Discounted Cumulative Gain** evaluates the overall value of the search results based on their rankings and relevance.
Finally, **Normalized Discounted Cumulative Gain**, provides a normalized score of DCG between 0 and 1 to make it 
easier to compare measurements.

You can customize these metrics for your search configuration. For example, you might have 
different configurations for precision@3 and precision@10 to measure the precision for your top three 
results (important on mobile screens) and the first full page of results respectively. Additionally, you can configure thresholds for each of your
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

## Related tools

Rankquest Studio is a relatively new tool and I've obviously taken a lot of inspiration from existing tools out there. This is not an exhaustive list and I don't have a lot of hands on experience with most of these tools.

- [Quepid](https://quepid.com/) - Developed by some people I know at Opensource Connections, this is probably the most mainstream option and something a lot of search relevance engineers would be familiar with. It's a bit older but well supported; the people at OSC are awesome.
- [Rated Ranking Evaluator](https://github.com/SeaseLtd/rated-ranking-evaluator) - A bit older project that is interoperable with Quepid. Mainly focuses on Elasticsearch and Solr.
- [Querite](https://github.com/tballison/quaerite) - Like RRE a bit older and mainly focuses on Elasticsearch and Solr.
- [Rank Evaluation API in ES](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html) - Only works for Elasticsearch and currently lacks a UI. The lack of usability of this is what prompted me to develop Rankquest Studio. 

My main critique of these tools, and the reason that I created Rankquest Studio is that these tools are a bit too low level and mostly focus on testing queries rather than search APIs. Products like Elasticsearch and Solr are fantastic but there are more options in the market. And regardless of what you use - or how you use that - what actually matters is the API you create using those tools and how that benefits your product and use case. 

This is true regardless of whether you use Solr, Opensearch, Elasticsearch, one of the new vector search products, an in house solution, or querying support in e.g. postgres, mongodb, or other products. Additionally, in order to make an informed decision as to which of these is best for you you would have to test them in the same way. With Ranquest Studio you can test pretty much anything that returns a ranked list of stuff. It's implementation neutral by design.

## Data safety

Since Rankquest Studio runs in your browser without any server components, you are fully in control of your data. It never shares any of your data, metrics, etc. with any outside servers. All the json is stored in your browser's local storage. You can delete that at your discretion and you can export and import that as files from the application.

## Support and Contributing

If you wish to contribute to Rankquest Studio or have feature suggestions or change requests, that's great. Please use the issue tracker and we'll coordinate any changes there.

I provide Rankquest Studio for free as open source mainly to promote my services as an independent search consultant. My main job is being the CTO for [FORMATION](https://tryformation.com). But I do occasionally still consult companies with their search challenges as a freelance consultant. If you need my help or support, please reach out via email (jilles AT jillesvangurp.com) and we can discuss how to improve search in your company.

## Implementation details

This section is for people that wish to work on the source code to make modifications. Rankquest Studio is a [kotlin-js](https://kotlinlang.org/docs/js-overview.html) project that uses the amazing [Fritz2](https://www.fritz2.dev/) framework. Fritz2 is a reactive framework in the style of react and similar frameworks. But it also builds on top of foundations laid by Kotlin with things like co-routines, flows, and internal DSLs. 

The project uses various kotlin multiplatform dependencies that can work both on the JVM and in a browser (and elsewhere), including rankquest-core, our kt-search library for talking to Elasticsearch, and various kotlinx libraries.

Additionally, we use the new [K2 compiler and Kotlin 2.0](https://kotlinlang.org/docs/whatsnew-eap.html). While not yet ready for production usage, the benefits of this are faster incremental compilation and the fact that it seems to work outweighs the downsides of dealing with its occasional flakiness and remaining bugs. If this causes you issues, you can turn it off in `gradle.properties`.

The CSS styling is done using the popular [tailwind](https://tailwindcss.com/) framework. Tailwind works via a hook in `webpack.config.d` that processes the javascript assets that are produced by the kotlin-js transpiler to ensure that any of the tailwind classes used are backed by the correct, minimum amount of CSS necessary. An important limitation with this approach is that tailwind uses simple string manipulation and therefore you should not attempt to manipulate these strings at runtime (e.g. concatenating them with some if .. else .. logic). Tailwind works at build time and any such manipulations may cause it to fail to generate and include the right css classes.

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