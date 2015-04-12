#!/bin/sh
cd `dirname $0`/..

rm -rf data/yesterday/* data/yesteryear/*
extraction/extraction --database-path data/tweets.db --dump-path data/yesterday --last-day
jq -c . data/months/*|python extraction/date_select.py last_year > data/yesteryear/yesteryear.json
