FROM ubuntu:latest
MAINTAINER Etienne J. Membrives <etienne@membrives.fr>
RUN apt-get update && apt-get install -y jq python-pip python-dev build-essential python-dateutil python-networkx python-virtualenv python-setuptools python-unidecode libleveldb-dev
COPY scripts /oldnews/scripts/
COPY logger /oldnews/logger/
COPY extraction /oldnews/extraction/
COPY selection /oldnews/selection/
ADD or/Google.OrTools.python.examples.3549.tar.gz /or/
RUN cd /or/ortools_examples && python setup.py install
CMD /oldnews/oldnews.sh
VOLUME /oldnews/data
