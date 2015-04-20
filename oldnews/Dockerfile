FROM ubuntu:latest
MAINTAINER Etienne J. Membrives <etienne@membrives.fr>
ENV TZ Europe/Paris
RUN cp /usr/share/zoneinfo/Europe/Paris /etc/localtime
RUN apt-get update
RUN apt-get install -y python-dateutil python-networkx python-virtualenv python-setuptools python-unidecode
RUN apt-get install -y jq libleveldb-dev
RUN apt-get install -y ruby ruby-dev
RUN gem install t
COPY scripts /oldnews/scripts/
COPY logger /oldnews/logger/
COPY extraction /oldnews/extraction/
COPY selection /oldnews/selection/
COPY publish /oldnews/publish/
ADD or/Google.OrTools.python.examples.3549.tar.gz /or/
RUN cd /or/ortools_examples && python setup.py install
CMD /oldnews/oldnews.sh
VOLUME /oldnews/data
