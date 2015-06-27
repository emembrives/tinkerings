package main

import (
	"encoding/json"
	"flag"
	"log"
	"net/url"

	"github.com/ChimeraCoder/anaconda"
	"github.com/jmhodges/levigo"
)

var (
	CONSUMER_KEY        string
	CONSUMER_SECRET     string
	ACCESS_TOKEN        string
	ACCESS_TOKEN_SECRET string

	databasePath                = flag.String("database-path", "tweets.db", "Path to database directory")
	newsTwitterHandles []string = []string{
		"lemondefr",
		"libe",
		"Le_Figaro",
		"RTLFrance",
		"Europe1",
		"franceinter",
		"RFI",
		"franceinfo",
		"20Minutes",
		"le_Parisien",
		"leJDD",
		"LEXPRESS",
		"LePoint",
		"LeNouvelObs",
		"metronews",
		"RMCInfo",
	}
)

func addToDatabase(tweets []anaconda.Tweet) {
	opts := levigo.NewOptions()
	opts.SetCache(levigo.NewLRUCache(10000))
	opts.SetCreateIfMissing(true)
	db, err := levigo.Open(*databasePath, opts)

	if err != nil {
		log.Fatal(err)
	}

	writeOps := levigo.NewWriteOptions()
	for _, tweet := range tweets {
		j, err := json.Marshal(tweet)
		if err != nil {
			log.Fatal(err)
		}
		db.Put(writeOps, []byte(tweet.IdStr), j)
	}
}

func getTimelineForUser(api *anaconda.TwitterApi, username string) ([]anaconda.Tweet, error) {
	v := url.Values{}
	v.Set("screen_name", username)
	v.Set("count", "4000")
	v.Set("include_rts", "1")
	tweets, err := api.GetUserTimeline(v)
	return tweets, err
}

func main() {
	flag.Parse()
	anaconda.SetConsumerKey(CONSUMER_KEY)
	anaconda.SetConsumerSecret(CONSUMER_SECRET)
	api := anaconda.NewTwitterApi(ACCESS_TOKEN, ACCESS_TOKEN_SECRET)
	var allTweets []anaconda.Tweet
	for _, handle := range newsTwitterHandles {
		tweets, err := getTimelineForUser(api, handle)
		if err != nil {
			log.Fatal(err)
		}
		allTweets = append(allTweets, tweets...)
	}
	addToDatabase(allTweets)
}
