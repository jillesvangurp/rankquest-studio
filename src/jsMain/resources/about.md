# About Rankquest Studio

Rankquest Studio is a web based tool that you can use to benchmark search relevance metrics for your search APIs.

## Quick overview

Watch this [quick overview](https://youtu.be/Nxr2UVs_n74?si=YKslAJbY7-BojcmB) on Youtube.

## Why search relevance matters

Most websites or apps have some kind of search functionality. There are all sorts of ways to implement this. There are managed solutions such as Algolia that are easy to integrate. You can use off the shelf packages like Opensearch, Solr, or Elasticsearch. And there are various databases that integrate search functionality. Additionally, there is a growing amount of products that support vector search and other AI based approaches. But how do you decide which is best for your use case? Where do you start?  

Whichever solution you use, search relevancy is about delivering the best results possible to your users. And of course, **you are competing against others** that are trying to do their best as well. Your competitiveness and profit depends on how relevant your search results are. 

In order to optimize search relevance, you need to be able to **test and measure** search relevance. This allows you to compare different solutions and enables you to evaluate improvements to your seerch in an objective way. Doing so helps you drive your product roadmap, identify points of improvement, and take informed decisions about where to focus your efforts, and drive the quality of your search in a data driven way.

## How does Rank Quest Studio work?

Rankquest Studio helps you build collections of test cases of **rated searches**. A test case is a search query and a list of results with ratings that indicate how relevant each result is for that search. Rankquest Studio uses a simple five star rating. A high rating indicates the result would be highly relevant for that search, a 0 star rating would indicate the result is not relevant for the search at all.

An effective suite of rated searches may include examples of searches that are representative of what users would be looking for, things that are known to work poorly, or things that are known to work well. The more test cases you add, the easier it gets to measure improvements or regressions in how good your search is. 

To measure search quality, Rankquest Studio runs the test cases against your search API and calculates various search relevance metrics using the results that come back and by comparing those to your rated results. If a particular result with a high rating is missing or ranked low, that negatively affects the metrics and if they are where they should be the metrics go up.

## Search Plugin Configurations

To use Rankquest Studio, you need to  configure it to communicate with your search service using one of the plugins. You can do this in the Configuration screen. 

You can add multiple configurations here and easily switch between them. Doing so is useful for comparing different versions of your search with the same test cases.

If you are new to Rankquest Studio and just want to play around a bit with the UI, you can enable the demo plugins in this screen. These are intended for quickly exploring how Rankquest works with a simple movie quote search service. Three versions of this search are provided. Two of them use an in memory search engine and the third one enables you to use Elasticsearch or Opensearch. 

A plugin configuration needs to include the following details:

- an id for the configuration
- A way to extract lists of results from the search API response. Each result should have a unique id and you may also extract an optional label (e.g. a title or name field)
- A list of search parameters for your search API. This typically includes a text field, a parameter to restrict the number of results, etc. You can of course specify defaults for your search parameters.
- A list of search relevance metrics configurations. You can go with the defaults here or tune this.

Rankquest Studio comes with four plugins. Three
of those are provided by rankquest-core and are written in Kotlin:

- JsonGetAPIPlugin - allows you to do an HTTP GET against any API that returns Json
- JsonPostAPIPlugin - allows you to do an HTTP POST against any API that returns Json
- ElasticsearchPlugin - lets you query Elasticsearch
- Javascript Plugin -  allows you to **run javascript in your browser**. Note. while this works great inside the browser, this plugin does not work outside the browser and cannot be used with rankquest-cli. Using Javascript, you can use the browser fetch API to access your server.

Note. because the plugins run in a browser, your search API must set the appropriate CORS headers.

You can of course create your own plugin in Kotlin. However, this is a bit more involved and you will have to modify
rankquest studio in order to be able to edit your configuration. Additionally, you may need to modify rankquest-cli as well so that it can use your plugin. For this, refer to the source code and the existing plugins.

An **alternative to writing your own plugin may also be proxying** your search API such that you can use one of the builtin plugins to talk to the proxy. This may be easier than customizing the UI for your plugin and is something you might be able to do with a few lines of code in your preferred programming language.

## Creating  and fine tuning Test Cases

Once you have created and activated a search configuration, you can start creating **test cases**. Simply use the search screen to query your search service using the plugin. In the search screen, you can customize the search parameters and fetch results. 

You can then convert the parameter configuration + results in a test case. The new test case is initialized with a copy of the parameters you used for the search (the search context) and initialized with the results that it found. 

You can **fine tune** your test case in the Test Cases screen. There you can review your test cases, modify the rating of your results, add/remove more results, and document & annotate your test cases with some comments and tags.

## Running Metrics

Once you have test cases, you can go to the **metrics** screen to calculate the metrics that you defined in your search plugin configuration. 

The metrics are broken down by test case and by result. If there are any unrated results, you can add them to your test case from this screen as well.

## Exporting and Importing

Instead of using a database, Rankquest Studio uses simple files to store test cases, configurations, etc. This makes it easy to share test cases and also enables you to use e.g. a git repository to keep track of your test cases, configurations, etc.

You can import/export:

- plugin configurations
- metric configurations (so you can reuse them with different plugin configurations)
- test cases
- metrics runs

## Commandline

You can use the exported configuration and testcases on the **command line** using [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli). Rankquest-cli is available via [docker hub](https://hub.docker.com/repository/docker/jillesvangurp/rankquest-cli/general) and you can invoke it like this:

```bash
docker run -it --network host -v $(pwd):/rankquest jillesvangurp/rankquest-cli -c my-config.json -t my-testcases.json -v -f
```

For more information on this, see the [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli) project.

## Why another tool?

I wanted to have an easy to use tool that removes all possible excuses for people to 
start evaluating and iterating on their search quality. Over my career as a search 
specialist and consultant, I've worked with many clients that did not have any form 
of structured testing of their search quality. 

There are of course several excellent tools 
for this already. However, most of these tools are aimed at search relevance experts, 
which makes these tools difficult to use for non-experts. Additionally, a lot of these
tools require some setup. Including, usually, running some kind of database somewhere. 
This further complicates trying out and taking into use these tools. 

Finally, a lot of these tools are tightly coupled to specific search products such as Solr 
or Elasticsearch. You use these tools to test specific queries. While this is of course useful, 
this amounts to a form of whitebox testing where you make various assumptions about the
internals of your product. In my view, search quality testing should be 
a blackbox test that runs directly against your API without making any assumptions about what 
it uses internally or how it is implemented. This allows you to radically change your implementation and compare 
the before and after metrics. 

Rankquest studio aims to address all these issues by providing a simple but usable UI 
that allows people to simply get started by simply opening the app. There is **no installation
process**. Rankquest Studio is a web site that you open and use.

There is no server either (other than the simple web server that hosts the application). 
It does not require an application server or a database. Everything happens
in the browser. All the data it manipulates is stored in the browser. You can easily import and export
ratings, configurations, and metrics reports in json format. The only network 
traffic that rank quest studio makes is to your own search service.

It makes no assumptions about how your search API works. It uses an extensible plugin model to talk to 
your search service. The only assumption it makes is that whatever you have can return a list of results with ids for a given search context (your search parameters).

## Links

- [rankquest-studio](https://github.com/jillesvangurp/rankquest-studio) The github project for this web application. It's all written in kotlin-js and uses the wonderful [Fritz2](https://www.fritz2.dev/) framework. 
- [Release Notes](https://github.com/jillesvangurp/rankquest-studio/releases) Find out about the latest features and fixes here.
- [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli) A command line tool that you can use to use exported search configurations and test cases to run metrics from the command line or in your CI build.
- [rankquest-core](https://github.com/jillesvangurp/rankquest-core) A Kotlin multiplatform kotlin library that implements the metrics and search plugins used in Rankquest Studio.
- [ktsearch](https://github.com/jillesvangurp/kt-search) A kotlin multiplatform library for using Elasticsearch and Opensearch. This library is used to implement the Elasticsearch searchplugin and also used for the movie quotes demo.
- [querylight](https://github.com/jillesvangurp/querylight) An in memory search library that is used for the movie quotes demos.

## Support & Getting help taking your search to the next level

Please provide feedback via the [Github Issue Tracker](https://github.com/jillesvangurp/rankquest-studio/issues).

I created Rankquest to help me evaluate our search quality in a few of my own projects and after years of observing my clients mostly ignoring search quality. With some notable exceptions, most companies don't employ search relevance experts, and have no good way to benchmark their search quality beyond manual testing. After looking at existing tools, I chose the more difficult but satisfying path of building yet another tool to address some of their limitations.

I've been working with search technology for over twenty years and with Elasticsearch and Opensearch 
for the last decade. I've worked with small and big companies in various roles and still occasionally do 
some consulting next to my main job as CTO of [FORMATION Gmbh](https://tryformation.com).

If you need help with your search, want an outside opinion about your current setup, or are struggling with your search quality, **I might be able to help you**. And if not, I can connect you with people that can.

As a **search consultant**, I have advised many clients over the years on how to architect and use search effectively. This usually involves both advising on query and mapping strategies, refining their product strategy, coming up with good ETL strategies, coming up with solutions for ranking challenges, and educating people about all this. Unfortunately, my main job as a CTO prevents me from joining your team for extended periods of time or on a full time basis. However, I can add a lot of value quickly and help set your company up for success. My preferred way of working is doing short, result driven products and coaching people how to do what they need to get done. The ideal outcome of a project for me is a happy customer that no longer needs my services because they are fully equipped to take the next steps.

## Showing your appreciation

This project is free and open source. If you like it, let me know & give me some feedback, tell others, star the project on [Github](https://github.com/jillesvangurp/rankquest-studio), show it to others, etc. And consider engaging my services as a search consultant.

## Jilles van Gurp

<img src="jilles.jpg" style="width: 100px;height: 100px">

**email**: jilles AT jillesvangurp.com

**address**: Bergstrasse 16, 10115 Berlin, Germany

[www.jillesvangurp.com](https://www.jillesvangurp.com) [@jillesvangurp](https://twitter.com/jillesvangurp) [@jillesvangurp@mastodon.world](https://mastodon.world/@jillesvangurp) 

