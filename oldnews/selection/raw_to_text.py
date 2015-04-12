#!/usr/bin/env python3
import json
import sys
import time
import calendar
import re
import unicodedata

RE_URL = re.compile(r'http(:?s)?://[a-zA-Z0-9_/.-]+')
RE_HTML = re.compile(r'&[a-z][a-z];')
RE_NOT_ALPHA = re.compile(r'[^\w @]')
RE_STOPWORDS = re.compile(r'\b[a-z][a-z]?\b')
RE_STOPWORDS2 = re.compile(r'\b[A-Zà]\b')
RE_SPACE = re.compile(r'\s+')

data = {}
for line in sys.stdin.readlines():
    tweet = json.loads(line)
    date = calendar.timegm(time.strptime(tweet['date'], "%a %b %d %H:%M:%S +0000 %Y"))
    data.setdefault(date, set()).add(tweet['text'])

data = sorted(data.items(), key=lambda x:x[0])
for item in data:
    for entry in item[1]:
        text = re.sub(RE_URL, '', entry)
        text = re.sub(RE_HTML, ' ', text)
        text = re.sub(RE_NOT_ALPHA, ' ', text)
        text = re.sub(RE_STOPWORDS, ' ', text)
        text = re.sub(RE_STOPWORDS2, ' ', text)
        text = re.sub(RE_SPACE, ' ', text)
        print(text.strip())

