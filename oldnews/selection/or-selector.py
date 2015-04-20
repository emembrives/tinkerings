import argparse
import datetime
import json
import logging
import logging.config
from dateutil import tz

from ortools.linear_solver import pywraplp

class MultiDict(object):
    def __init__(self):
        self.__data = {}

    def put(self, key, value):
        if not key in self.__data:
            self.__data[key] = []
        self.__data[key].append(value)
        return self.get(key)
    
    def __contains__(self, key):
        return key in self.__data

    def get(self, key):
        return self.__data[key]

    def keys(self):
        return self.__data.keys()

TWITTER_DATEFORMAT = "%a %b %d %H:%M:%S +0000 %Y"
from_zone = tz.gettz('UTC')
to_zone = tz.gettz('Europe/Paris')

def time_of_tweet(tweet):
    tweet_date_utc = datetime.datetime.strptime(tweet['created_at'], TWITTER_DATEFORMAT)
    tweet_date_utc = tweet_date_utc.replace(tzinfo=from_zone)
    tweet_date = tweet_date_utc.astimezone(to_zone)
    return tweet_date


def process_tweets(tweets):
    solver = pywraplp.Solver('SolveTweetSelection',
            pywraplp.Solver.GLOP_LINEAR_PROGRAMMING)

    old_clusters = MultiDict()
    new_clusters = MultiDict()
    for tweet_id in range(len(tweets)):
        old_clusters.put(tweets[tweet_id][3], tweet_id)
        new_clusters.put(tweets[tweet_id][2], tweet_id)

    # Creating variables
    tweet_variables = []
    for tweet_id in range(len(tweets)):
        tweet_variables.append(solver.IntVar(0, 1, "t%d" % tweet_id))

    old_cluster_count_variables = []
    old_cluster_bool_variables = []
    for cluster in old_clusters.keys():
        variable = solver.IntVar(0, len(old_clusters.get(cluster)), "oc%d" % cluster)
        solver.Add(variable == solver.Sum([tweet_variables[i] for i in old_clusters.get(cluster)]))
        old_cluster_count_variables.append(variable)

        variable = solver.IntVar(0, 1, "ocb%d" % cluster)
        solver.Add(variable == solver.Sum(
            [tweet_variables[i] for i in old_clusters.get(cluster)]))
        old_cluster_bool_variables.append(variable)

    new_cluster_count_variables = []
    for cluster in new_clusters.keys():
        variable = solver.IntVar(0, len(new_clusters.get(cluster)), "nc%d" % cluster)
        solver.Add(variable == solver.Sum([tweet_variables[i] for i in new_clusters.get(cluster)]))
        new_cluster_count_variables.append(variable)
  
    # We want between 5 and 10 tweets
    constraint1 = solver.Constraint(5, 10)
    for variable in tweet_variables:
      constraint1.SetCoefficient(variable, 1)

    # We want at least 5 old topics
    constraint2 = solver.Constraint(5, solver.infinity())
    for var in old_cluster_bool_variables:
        constraint2.SetCoefficient(var, 1)
        
    # We don't want more than 3 tweet per topic
    for var in old_cluster_count_variables:
        constraint = solver.Constraint(0, 3)
        constraint.SetCoefficient(var, 1)

    # We don't want more than 3 tweet per new topic
    for var in new_cluster_count_variables:
        constraint = solver.Constraint(0, 3)
        constraint.SetCoefficient(var, 1)

    DEBUG_L = [5, 8, 9, 10, 11, 12, 93, 98]
    # No two tweets with less than one hour interval
    for tweet_id in range(len(tweets)):
        t1 = time_of_tweet(tweets[tweet_id][0])
        for tweet_id2 in range(len(tweets)):
            if tweet_id >= tweet_id2:
                continue
            t2 = time_of_tweet(tweets[tweet_id2][0])
            if abs((t1 - t2).total_seconds()) > 600:
                continue
            solver.Add(tweet_variables[tweet_id] == 0 or tweet_variables[tweet_id2] == 0)

    objective = solver.Objective()
    coefficients = []
    for tweet_id in range(len(tweets)):
        coefficient = float(tweets[tweet_id][0]["retweet_count"])
        if tweets[tweet_id][2] < 0:
            coefficient /= 2.0
        if tweets[tweet_id][3] < 0:
            coefficient /= 2.0
        else:
            coefficient *= len(old_clusters.get(tweets[tweet_id][3]))
        coefficients.append(coefficient)
        objective.SetCoefficient(tweet_variables[tweet_id], coefficient)
    objective.SetMaximization()

    status = solver.Solve()

    for i in range(len(tweets)):
        logger.debug("%s@%s new %d, old %d, selection %d, coeff %f",
                tweets[i][0]['text'],
                tweets[i][0]['created_at'],
                tweets[i][2],
                tweets[i][3],
                int(tweet_variables[i].solution_value()),
                coefficients[i])

        if tweet_variables[i].solution_value() > 0.5:
            yield tweets[i][0]
    
logging.config.fileConfig('data/logging.conf')
logger = logging.getLogger('selection')

parser = argparse.ArgumentParser(description="Selects tweets")
parser.add_argument("--input", default="data/clusters.json", help="JSON clusters")
parser.add_argument("--output", default="data/output.json", help="Selected tweets")

args = parser.parse_args()
with open(args.input) as f:
    tweets = json.load(f)

selected_tweets = process_tweets(tweets)

with open(args.output, "w") as f:
    for tweet in selected_tweets:
        json.dump(tweet, f)
        f.write('\n')
