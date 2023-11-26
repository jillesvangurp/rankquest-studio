# About Rankquest Studio

Rankquest Studio is a tool that helps you assess how good your search service or API is at ranking your results. You do this by
querying your service and by providing rated search results. These rated search results are then used to 
calculate search relevance **metrics**.

Doing so is useful because it helps you assess the impact of changes you make to the way your search service works. 
Do the metrics go up or down? This can be a more objective way to evaluate what changes do to your system than peopple's
opinions, gut feelings, intuition, etc. Optimizing search engines can be a complicated process. You fix one query and 
that fix may then have unexpected consequences on other queries. Testing all this manually is a lot of work.

Gathering test cases for the queries you care about and measuring the impact of your changes makes this process easier. 
Rankquest Studio helps you do that.

## Quick overview

Watch this [quick overview](https://youtu.be/Nxr2UVs_n74?si=YKslAJbY7-BojcmB) on Youtube.

## Getting started

- Start by creating a **search plugin configuration**. You can also choose to play with one of the built in demo configurations. A search plugin configuration tells Rankquest studio how to fetch results from your search service. Results must be assigned an id and you can optionally extract a label from your results that is displayed. Additionally, you need to specify what parameters are needed to use your search service. Finally, you can specify a list of metric configurations that you want to measure. 

- Then you can start creating **test cases**. Simply use the search tool to run a search. You can convert the results in a test case. Each new test case is initialized with a copy of the parameters you used for the search (the search context) and initialized with the results that it found. 

- You can **fine tune** your test cases in the Test Cases screen. You can review your test cases, and add/remove more results, modify the rating of your results, and document your test cases with some comments. 

- Once you have test cases, you can go to the **metrics** screen to get metrics.

You should **export and backup** your configuration, test cases, and metrics. A good suggestion is to store them in a git repository or some other safe place.

You can use the exported configuration and testcases on the **command line** using [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli). A build of this is available via docker hub and you can invoke it like this:

```bash
docker run -it --network host -v $(pwd):/rankquest jillesvangurp/rankquest-cli -c my-config.json -t my-testcases.json -v -f
```

For more information on this, see the rankquest-cli project.

## Search Plugins

To get results from your search API, you need to use a search plugin. Rankquest Studio comes with four plugins. Three
of those are provided by rankquest-core and are written in Kotlin:

- JsonGetAPIPlugin - allows you to do an HTTP GET against any API that returns Json
- JsonPostAPIPlugin - allows you to do an HTTP POST against any API that returns Json
- ElasticsearchPlugin - lets you query Elasticsearch

A fourth plugin is bundled with Rankquest Studio and allows you to run javascript in your browser. Note. while this works great inside the browser, this plugin does not work outside the browser and cannot be used with rankquest-cli. Using Javascript,
you can use the browser fetch API to access your server.

Finally, you can of course create your own plugin in Kotlin. However, this is a bit more involved and you will have to modify
rankquest studio in order to be able to edit your configuration. Additinoally, you may need to modify rankquest-cli as well
so that it can use your plugin. For this, refer to the source code and the existing plugins.

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

