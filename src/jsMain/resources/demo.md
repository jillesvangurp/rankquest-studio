# Rankquest Studio Demos

The configuration screen in Rankquest Studio has a "Show Demo Content" link
that enables a few buttons. Using these buttons, you can add some
test configurations to explore the Rankquest Studio UI.

## The demos

For this we use a small data set of 732 quotes from famous movies and three
different configurations that allow you to explore this:

- Movie quotes search: uses the querylight library with a simple tf/idf index.
- Movie quotes with ngrams: similar but it uses ngrams instead of terms. The 
metrics for this are a bit less iteresting.
- ES based movie search. Relies on an external elasticsearch that you have 
to run locally

## Loading the test case

1. Add & use one of the demo configurations so it is active
1. Make sure you still have the *Show Demo Content* enabled. 
1. Go to the Test Cases screen and click the *Load Demo Movies Test Cases* button

Now you can go to the search screen and search for movies and the metrics screen to 
explore some metrics.

## Using Elasticsearch

The ES based movie search of course requires elasticsearch. Two buttons are provided,
that create the index and remove it again.

Unlike the two querylight demos, the elasticsearch one uses a plugin configuration that you can modify and tweak.

This is great for getting started!

Make sure elasticsearch is running on localhost and port 9200. If you use docker,
you can use this [docker-compose](docker-compose.yml) file to start elasticsearch
and kibana by downloading it and running `docker compose up -d`
                                        
Important: **make sure your elasticsearch server is configured to send CORS headers**. Without
 this your browser will not allow requests to Elasticsearch.

If you use docker-compose, you can add these settings: 

```yaml
  http.cors.enabled: "true"
  http.cors.allow-origin: |-
  "*"
  http.cors.allow-methods: "OPTIONS, HEAD, GET, POST, PUT, DELETE"
  http.cors.allow-headers: "X-Requested-With, X-Auth-Token, Content-Type, Content-Length, Authorization, Access-Control-Allow-Headers, Accept"                    
```
