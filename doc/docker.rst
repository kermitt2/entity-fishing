.. topic:: entity-fishing with Docker


entity-fishing with Docker
==========================

Docker automates the deployment of applications inside software containers. The documentation on how to install it and start using it can be found `here <https://docs.docker.com/engine/understanding-docker/>`_.

*entity-fishing* can be instantiated and run using Docker. 

Running entity-fishing with Docker
**********************************

An image for *entity-fishing* is available on Docker Hub:

Pull the image from Docker Hub (check the `latest version number <https://hub.docker.com/r/grobid/entity-fishing/tags>`_:
::
    $ docker pull grobid/entity-fishing:${latest_entity_fishing_version}

The current latest version should be (image size: 1.6GB):
::
    $ docker pull grobid/entity-fishing:0.0.6

Prepare the knowledge data volumes on your host machine: *entity-fishing* uses LMDB to store compiled Wikidata and Wikipedia resources for every supported languages. Despite compression and indexing, these resources are pretty big, because they cover most of Wikidata and language-specific Wikipedia content. 

Download on your host machine (where the docker container will run) the data resources, as explained in the `installation instructions <build.rst>`_.

Run the container (if necessary, adapt the port mapping according to your requirements) and mount the data volumes for the languages to be supported indicating the path where you have unzipped them. The minimal requirement is to mount at least the db-kb (Wikidata) and db-en (English Wikipedia) volumes: 
::
    $ docker run --rm -p 8090:8090 -p 8091:8091 \
      -v /home/lopez/entity-fishing/data/db/db-kb:/opt/entity-fishing/data/db/db-kb \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-en \
      grobid/entity-fishing:0.0.6 

More volumes can be mounted to support more languages. Be sure to have installed the data resources files on the host machine at the previous steps. For example, here for the 15 supported languages:
::
    $ docker run --rm -p 8090:8090 -p 8091:8091 \
      -v /home/lopez/entity-fishing/data/db/db-kb:/opt/entity-fishing/data/db/db-kb \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-en \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-fr \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-de \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-es \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-it \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-ar \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-zh \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-ru \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-ja \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-pt \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-fa \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-uk \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-sv \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-bn \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-hi \
      grobid/entity-fishing:0.0.6 

Similarly to run the container with a modified config file, mount the modified version at launch of the container: 
::
    $ docker run --rm -p 8090:8090 -p 8091:8091 \
      -v /home/lopez/entity-fishing/data/db/db-kb:/opt/entity-fishing/data/db/db-kb \
      -v /home/lopez/entity-fishing/data/db/db-en:/opt/entity-fishing/data/db/db-en \
      -v /home/lopez/entity-fishing/data/config/wikipedia-en.yaml:/opt/entity-fishing/data/config/wikipedia-en.yaml \
      grobid/entity-fishing:0.0.6

Access the service (with default port):

  - web demo/console: open the browser at the address http://localhost:8090

  - the health check will be accessible at the address http://localhost:8091

  - metrics and monitoring are available at http://localhost:8091/metrics?pretty=true (Dropwizard metrics) and Prometheus metrics (e.g. for Graphana monitoring) are available at http://localhost:8091/metrics/prometheus

*entity-fishing* web services are then available as described in the `service documentation <https://grobid.readthedocs.io/en/latest/Grobid-service/>`_

**Important:** It is not recommended to launch several containers using the same data resources volumes, access to the volumes might be quickly a bottleneck for the concurrent container processes. 

Building entity-fishing image
*****************************

For building a new image corresponding to the current entity-fishing master (e.g. ``0.0.7-SNAPSHOT``):
::
    $ docker build -t grobid/entity-fishing:0.0.7-SNAPSHOT --build-arg BUILD_VERSION=0.0.7-SNAPSHOT --file Dockerfile .


