#!/usr/bin/env python

import platform
if platform.python_implementation() == "PyPy":
    import sys
    sys.path.insert(0, '/usr/lib/python2.7/dist-packages')

import argparse
import csv
import fileinput
import itertools
import json
import logging
import logging.config
import networkx as nx
import sys

from distances import *
from clean_text import clean_text

def load_bigrams(unigrams_path, bigrams_path):
    unigrams = {}
    bigrams = {}
    with open(unigrams_path) as f:
        first = False
        for row in csv.reader(f):
            if not first:
                first = True
                continue
            unigrams[row[0]] = (int(row[1]), float(row[3]))

    with open(bigrams_path) as f:
        first = False
        for row in csv.reader(f):
            if not first:
                first = True
                continue
            bigrams[tuple(map(int, row[0:2]))] = float(row[2])
    return unigrams, bigrams

def load_tweets(path):
    tweets = []
    tweet_ids = set()
    with open(path) as f:
        for row in f.readlines():
            tweet = json.loads(row)
            if tweet['id'] in tweet_ids:
                continue
            else:
                tweet_ids.add(tweet['id'])
            tweets.append(tweet)
    return tweets

def distance_matrix(unigrams, bigrams, tweets):
    # Prepare tweets
    prepare = lambda t: clean_text(t['text']).split()
    tweet_text = map(prepare, tweets)
    # Build the distance matrix
    # distances = [[(x, y, compute_distance(unigrams, bigrams, tweets[x], tweets[y])) for y in xrange(len(tweets)) if y > x] for x in xrange(len(tweets))]
    distances = [[(x, y, compute_shared_proba(unigrams, bigrams, tweet_text[x], tweet_text[y])) for y in xrange(len(tweets)) if x < y] for x in xrange(len(tweets))]
    return distances


def write_to_file():
    data = list(itertools.chain(*distances))
    with open("tweets-dists.csv", "w") as f:
        for x, y, dist in data:
            f.write("%d, %d, %f\n" % (x, y, dist))

    with open("tweets-dists.dot", "w") as f:
        f.write("graph G {\n")
        f.write("overlap=false;\n")
        for x, y, dist in data:
            f.write("%d -- %d [label=\"%f\"];\n" % (x, y, dist))
        f.write("}\n")


def graph_cutting(tweets, distances, skip, selector, cutoff):
    data = list(itertools.chain(*distances))
    G = nx.Graph()
    for x, y, dist in data:
        if x not in skip and y not in skip and x < y and selector(x) and selector(y) and dist > cutoff:
            G.add_edge(x, y, weight=dist)
    components = nx.connected_components(G)
    clusters = filter(lambda c: len(c) > 2, components)

    def from_many_users(cluster):
        return len(set(map(lambda c: tweets[c]['user']['id'], cluster))) > 1

    clusters = filter(from_many_users, clusters)
    skip.update(itertools.chain(*clusters))
    return clusters


def compute_clusters(old_tweets, new_tweets, distances):
    #data.sort(key=lambda x: x[2])
    N_old_tweets = len(old_tweets)
    N_new_tweets = len(new_tweets)
    all_tweets = old_tweets + new_tweets
    is_old = lambda x: x<N_old_tweets
    is_new = lambda x: x>=N_old_tweets

    # Compute old clusters
    skip = set()
    old_clusters = graph_cutting(all_tweets, distances, skip, is_old, 50)
    if len(old_clusters) < 10:
        old_clusters.extend(graph_cutting(all_tweets, distances, skip, is_old, 30))
    logger.debug("#old clusters@30: %d", len(old_clusters))
    if len(old_clusters) < 10:
        old_clusters.extend(graph_cutting(all_tweets, distances, skip, is_old, 20))
    logger.debug("#old clusters@20: %d", len(old_clusters))
    base = len(old_clusters)
    if len(old_clusters) < 5:
        for i in range(N_old_tweets):
            if i not in skip:
                old_clusters.append([i])
                skip.add(i)
    logger.info("#old clusters: %d", len(old_clusters))
    logger.debug("tweets in old clusters: %d", len(skip))

    # Compute new clusters
    new_clusters = graph_cutting(all_tweets, distances, skip, is_new, 100)
    if len(new_clusters) < 10:
        new_clusters.extend(graph_cutting(all_tweets, distances, skip, is_new, 50))
    logger.debug("#new clusters@50: %d", len(new_clusters))
    if len(new_clusters) < 10:
        new_clusters.extend(graph_cutting(all_tweets, distances, skip, is_new, 30))
    logger.debug("#new clusters@30: %d", len(new_clusters))
    if len(new_clusters) < 10:
        new_clusters.extend(graph_cutting(all_tweets, distances, skip, is_new, 20))
    logger.info("#new clusters: %d", len(new_clusters))
    logger.debug("tweets in new clusters: %d", len(skip))

    return old_clusters, new_clusters

def get_distance(x, y, distances):
    if x > y:
        return get_distance(y, x, distances)
    return distances[x][y-x-1][2]
    
def mean_cluster_distance(cluster1, old_tweet, distances):
    distance = 0
    for n1 in cluster1:
        distance += get_distance(n1, old_tweet, distances)
    return distance / float(len(cluster1))

def reverse_clusters(clusters):
    reverse = {}
    for i in range(len(clusters)):
        for t in clusters[i]:
            reverse[t] = i
    return reverse

class MatchingStats(object):
    def __init__(self):
        self.cluster_distances = []
        self.new_cluster_old_cluster_match = 0
        self.new_cluster_no_old_match = 0
        self.old_cluster_no_new_match = 0

    def add_distance(self, d):
        self.cluster_distances.append(d)

    def add_match(self, new_index, old_index):
        if new_index >= 0 and old_index >= 0:
            self.new_cluster_old_cluster_match += 1
        elif new_index < 0:
            self.old_cluster_no_new_match += 1
        elif old_index < 0:
            self.new_cluster_no_old_match += 1

    def log(self, logger):
        logger.info("Clusters matched: new-old %d, new-0 %d, 0-old %d",
                self.new_cluster_old_cluster_match,
                self.new_cluster_no_old_match,
                self.old_cluster_no_new_match)
        self.cluster_distances.sort()
        logger.debug("Distance distribution: min %f, 1st quartile %f, "
                "median %f, mean %f, 3rd quartile %f, max %f",
                min(self.cluster_distances),
                self.cluster_distances[len(self.cluster_distances)/4],
                self.cluster_distances[len(self.cluster_distances)/2],
                sum(self.cluster_distances)/float(len(self.cluster_distances)),
                self.cluster_distances[(3*len(self.cluster_distances))/4],
                max(self.cluster_distances))


def matching_clusters(new_clusters, old_tweets, old_clusters, distances):
    reversed_old = reverse_clusters(old_clusters)
    matches = {}
    skip = set()
    stats = MatchingStats()
    for new_index in range(len(new_clusters)):
        cluster_matches = []
        for old_tweet in range(len(old_tweets)):
            d = mean_cluster_distance(
                    new_clusters[new_index],
                    old_tweet,
                    distances)
            stats.add_distance(d)
            if d > 10:
                old_cluster = -1
                if old_tweet in reversed_old:
                    old_cluster = reversed_old[old_tweet]
                    skip.add(old_cluster)
                cluster_matches.append((old_tweet, d, new_index, old_cluster))
                stats.add_match(new_index, old_cluster)
        matches[new_index] = cluster_matches
    for old_index in range(len(old_clusters)):
        if old_index in skip:
            continue
        matches[-old_index] = map(lambda t: (t, 1, -1, old_index), old_clusters[old_index])
        stats.add_match(-1, old_index)
    stats.log(logger)
    return matches

def print_matched_tweets(output_path, old_tweets, matches):
    output = []
    for matched_old_tweets in matches.values():
        for tweet_index, val, new_index, old_index in matched_old_tweets:
            output.append((old_tweets[tweet_index], val, new_index, old_index))

    with open(output_path, "w") as f:
        json.dump(output, f)


parser = argparse.ArgumentParser(description="Selects tweets")
parser.add_argument("--unigrams", default="data/unigrams.csv", help="Unigram input file")
parser.add_argument("--bigrams", default="data/bigrams.csv", help="Bigrams input file")
parser.add_argument("--input_old", default="data/tweets.json", help="Old tweets input file")
parser.add_argument("--input_new", default="data/tweets.json", help="New tweets input file")
parser.add_argument("--output", default="data/output.json", help="Tweets output file")
parser.add_argument("--debug", action='store_true', default=False, help="Log debug information")

args = parser.parse_args()

logging.config.fileConfig('data/logging.conf')
logger = logging.getLogger('selection')

unigrams, bigrams = load_bigrams(args.unigrams, args.bigrams)
old_tweets = load_tweets(args.input_old)
new_tweets = load_tweets(args.input_new)

distances = distance_matrix(unigrams, bigrams, old_tweets+new_tweets)

#write_to_file()
#hierarchical_clusters()
old_clusters, new_clusters = compute_clusters(old_tweets, new_tweets, distances)
matches = matching_clusters(new_clusters, old_tweets, old_clusters, distances)
print_matched_tweets(args.output, old_tweets, matches)
