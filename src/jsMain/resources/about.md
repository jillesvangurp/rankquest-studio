# About Rankquest Studio

Rankquest Studio is a tool that helps you assess how good your search service or API is. You do this by
querying your service and by providing rated search results. These rated search results are then used to 
calculate **metrics**.

## Getting started

Start by creating a **search plugin configuration**. You can also choose to play with one of the built in demo configurations. A search plugin configuration tells Rankquest studio how to fetch results from your search service. Results must be assigned an id and you can optionally extract a label from your results that is displayed. Additionally, you need to specify what parameters are needed to use your search service. Finally, you can specify a list of metric configurations that you want to measure. 

Then you can start creating **test cases**. Simply use the search tool to run a search. You can convert the results in a test case. Each new test case is initialized with a copy of the parameters you used for the search (the search context) and initialized with the results that it found. 

You can **fine tune** your test cases in the Test Cases screen. You can review your test cases, and add/remove more results, modify the rating of your results, and document your test cases with some comments. 

Once you have test cases, you can go to the **metrics** screen to get metrics.

You should **export and backup** your configuration, test cases, and metrics. A good suggestion is to store them in a git repository or some other safe place.

You can also produce metrics from your builds by using the **command line** version of Rankquest (*coming soon*).

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
it uses internally. This allows you to radically change your implementation and compare 
the before and after metrics. 

Rankquest studio aims to address all these issues by providing a responsive and very usable UI 
that allows people to simply get started by simply opening the app. There is **no installation
process**. Rankquest Studio is a web application.

There is no server either (other than the simple web server that hosts the application). 
It does not require a server or a database. Everything happens
in the browser. All the data it manipulates is stored in the browser. You can easily import and export
ratings, configurations, and metrics reports in json format. The only network 
traffic that rank quest studio makes is to your own search service.

It makes no assumptions about how your search API works. It uses an extensible plugin model to talk to 
your search service. The only assumption it makes is that whatever you have can return a list of results with ids for a given search context (your search parameters). 

## Support & Getting help with improving your ranking further

Please provide feedback via the [Github Issue Tracker](https://github.com/jillesvangurp/rankquest-studio/issues)

This tool was created by me to help me evaluate our search quality in a few of my own projects and after years of observing my clients mostly ignoring search quality. With some notable exceptions, most companies don't employ search relevance experts, and have no good way to benchmark their search quality beyond manual testing.

I've been working with search technology for over twenty years and with Elasticsearch and Opensearch 
for the last decade. I've worked with small and big companies in various roles and still do 
some consulting next to my main job as CTO of [FORMATION Gmbh](https://tryformation.com).

If you need help with your search, want an outside opinion about your current setup, or are struggling with your search quality, I might be able to help you.

As a **search consultant**, I have advised many clients over the years on how to architect and use search effectively. This usually involves both advising on query and mapping strategies, refining their product strategy, coming up with good ETL strategies, coming up with solutions for difficult problems, and educating people about all this. Unfortunately, I am not able to join your team for extended amounts of time. However, I can add a lot of value quickly and help set your team up for success. My preferred way of working is doing short, result driven products and coaching people how to do what they need to get done.

**Jilles van Gurp**

jilles AT jillesvangurp.com

[@jillesvangurp](https://twitter.com/jillesvangurp) [@jillesvangurp@mastodon.world](https://mastodon.world/@jillesvangurp) 
