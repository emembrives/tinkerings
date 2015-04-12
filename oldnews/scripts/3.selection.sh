#!/bin/sh
cd `dirname $0`/..
jq -c . data/months/* data/yesterday/*|python selection/bigrams.py
python selection/clusters.py --unigrams data/unigrams.csv --bigrams data/bigrams.csv --input_old data/yesteryear/* --input_new data/yesterday/* --output data/clusters.json
python selection/or-selector.py --input data/clusters.json --output data/output.json
