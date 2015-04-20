#!/usr/bin/env python
import json
import re
import fileinput
import itertools
import bisect
import sys

from clean_text import clean_text

unigram_count = 0
bigrams = {}
unigram = set()

tweets = 0
for line in fileinput.input():
    tweet = json.loads(line.strip())
    text = clean_text(tweet["text"])
    words = set(text.split(' '))
    tweet_bigrams = [(x, y) for x in words for y in words if x <= y]
    for bigram in tweet_bigrams:
        bigrams.setdefault(bigram, 0)
        bigrams[bigram]+=1
    tweets += 1

for x, y in bigrams.keys():
    if x == y:
        unigram.add(x)

unigram = sorted(unigram)
unigram_positions = {unigram[x]: x for x in range(len(unigram))}

observation_vector = {}
for x, y in bigrams.keys():
    if x == y:
        continue
    if bigrams[(x, x)] <= 2 or bigrams[(y, y)] <= 2:
        continue
    x_index = unigram_positions[x]
    y_index = unigram_positions[y]
    observation_vector[(x_index, y_index)] = float(bigrams[(x, y)])/float(bigrams[(x, x)] + bigrams[(y, y)] - bigrams[(x, y)])

with open("data/unigrams.csv", "w") as f:
    f.write("word,id,count,probability\n")
    for word in unigram:
        f.write("%s,%d,%d,%f\n" % (word, unigram_positions[word], bigrams[(word, word)], bigrams[(word, word)]/float(tweets)))

with open("data/bigrams.csv", "w") as f:
    f.write("word1,word2,distance\n")
    for bigram, value in observation_vector.items():
        f.write("%s,%s,%f\n" % (bigram[0], bigram[1], value))

