#!/bin/sh
cd `dirname $0`/..

extraction/extraction --database-path data/tweets.db --dump-path data/months --last-month
