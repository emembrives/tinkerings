#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import calendar
import time
import datetime
import json
import sched
import subprocess
import HTMLParser

parser = argparse.ArgumentParser(description="Selects tweets")
parser.add_argument("--tweets", default="data/output.json", help="Tweets to publish")

args = parser.parse_args()
html_parser = HTMLParser.HTMLParser()

PREFIX = u"#Ilya1an "

def publish_tweet(tweet):
    tweet_id = tweet["id_str"]
    original_status = tweet["text"]
    screen_name = tweet["user"]["screen_name"]
    max_length = 140 - len(screen_name) - 2
    new_status = PREFIX + html_parser.unescape(original_status)
    if len(new_status) > max_length:
        new_status = new_status[:(max_length - 1)] + u"\u2026"
    new_status = new_status.encode("utf-8")
    cmd = ["t", "reply", "-P", "data/.trc", tweet_id, new_status]
    subprocess.call(cmd)

tweets = []
with open(args.tweets) as f:
    for raw_tweet in f.readlines():
        tweets.append(json.loads(raw_tweet.strip()))

# Have two minutes of lead time
now = datetime.datetime.utcnow() - datetime.timedelta(seconds=120)
TWITTER_DATEFORMAT = "%a %b %d %H:%M:%S +0000 %Y"

scheduler = sched.scheduler(time.time, time.sleep)

for tweet in tweets:
    tweet_datetime_utc = datetime.datetime.strptime(tweet['created_at'], TWITTER_DATEFORMAT)
    retweet_datetime = tweet_datetime_utc.replace(year=tweet_datetime_utc.date().year + 1)
    if retweet_datetime < now:
        continue
    scheduler.enterabs(calendar.timegm(retweet_datetime.timetuple()), 1, publish_tweet, [tweet])

scheduler.run()
