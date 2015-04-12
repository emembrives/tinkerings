import math

from clean_text import clean_text

def compute_distance(unigrams, bigrams, tweet1, tweet2, debug=False):
    total_distance = []
    if debug:
        print tweet1['text'], tweet2['text']
    for word1 in clean_text(tweet1['text']).split(' '):
        word1_index = -1
        if word1 in unigrams:
            word1_index = int(unigrams[word1])
        if debug:
            print "Word1: %s, %d" % (word1, word1_index)
        min_distance = 1e10
        found = False
        for word2 in clean_text(tweet2['text']).split(' '):
            if debug:
                print "Word2: %s" % word2
            if word2 == word1:
                if debug:
                    print "Same word, break"
                found = True
                min_distance = 0
                break
            if word2 not in unigrams or word1 not in unigrams:
                if debug:
                    print "Not in unigrams"
                continue
            word2_index = int(unigrams[word2])
            if word1_index < word2_index:
                pair = (word1_index, word2_index)
            else:
                pair = (word2_index, word1_index)
            if pair in bigrams:
                found = True
                local_distance = -math.log(bigrams[pair])
                min_distance = min(local_distance, min_distance)
                if debug:
                    print "Pair %s, dist %f, min %f" % (str(pair), local_distance, min_distance)
            else:
                if debug:
                    print "Pair %s not found" % (pair)

        if found:
            if debug:
                print "Pair found, distances %s, min %s" % (str(total_distance), str(min_distance))
            total_distance.append(min_distance)
    if len(total_distance) == 0:
        return 1e10
    else:
        if debug:
            print "Sum %s, %f" % (str(total_distance), sum(total_distance))
        return sum(total_distance)

def compute_shared_proba(unigrams, bigrams, tweet1, tweet2, debug=False):
    score = 0.0
    for word1 in tweet1:
        word1_index = -1
        if word1 in unigrams:
            word1_index = int(unigrams[word1][0])
        for word2 in tweet2:
            if word1 == word2:
                score += -math.log(2*unigrams[word1][1])
            if word2 not in unigrams or word1 not in unigrams:
                if debug:
                    print "Not in unigrams"
                continue
            word2_index = int(unigrams[word2][0])
            if word1_index < word2_index:
                pair = (word1_index, word2_index)
            else:
                pair = (word2_index, word1_index)
            if pair in bigrams:
                score += -bigrams[pair]*(math.log(unigrams[word1][1])+math.log(unigrams[word2][1]))
    return score
