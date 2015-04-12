#!/usr/bin/python
# vim: set fileencoding=utf-8 :
import re
from unidecode import unidecode

RE_URL = re.compile(r'http(:?s)?://[a-zA-Z0-9_/.-]+')
RE_HTML = re.compile(r'&[a-z][a-z];')
RE_NOT_ALPHA = re.compile(r'[^\w @]')
RE_STOPWORDS = re.compile(r'\b[a-z][a-z]?\b')
RE_STOPWORDS2 = re.compile(r'\b[A-Zà]\b')
RE_SPACE = re.compile(r'[\s,\n]+')

def clean_text(entry):
    text = re.sub(RE_URL, '', unidecode(entry))
    text = re.sub(RE_HTML, ' ', text)
    text = re.sub(RE_NOT_ALPHA, ' ', text)
    text = re.sub(RE_STOPWORDS, ' ', text)
    text = re.sub(RE_STOPWORDS2, ' ', text)
    text = re.sub(RE_SPACE, ' ', text)
    return text.strip().lower()
