import argparse
import datetime
import fileinput
import json
import logging
import logging.config
import sys

import platform
if platform.python_implementation():
    sys.path.insert(0, '/usr/lib/python2.7/dist-packages')

from dateutil import tz

logging.config.fileConfig('data/logging.conf')
logger = logging.getLogger('extraction')

parser = argparse.ArgumentParser(description='Filter tweets by date.')
parser.add_argument('date', nargs=1, help="Date of the tweets")

args = parser.parse_args()

if args.date[0] == "last_year":
    today = datetime.date.today()
    year = today.year - 1
    month = today.month
    day = today.day
else:
    year, month, day = map(int, args.date[0].split('-'))

logger.info("Extracting tweets for %s", datetime.date(year, month, day).strftime("%A %d %B %Y"))

TWITTER_DATEFORMAT = "%a %b %d %H:%M:%S +0000 %Y"

from_zone = tz.gettz('UTC')
to_zone = tz.gettz('Europe/Paris')

for line in fileinput.input('-'):
    tweet = json.loads(line.strip())
    tweet_date_utc = datetime.datetime.strptime(tweet['created_at'], TWITTER_DATEFORMAT)
    tweet_date_utc = tweet_date_utc.replace(tzinfo=from_zone)
    tweet_date = tweet_date_utc.astimezone(to_zone)
    if tweet_date.year == year and tweet_date.month == month and tweet_date.day == day:
        print json.dumps(tweet)
