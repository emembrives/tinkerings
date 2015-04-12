package main

import (
	"encoding/json"
	"flag"
	"log"
	"os"
	"time"

	"github.com/ChimeraCoder/anaconda"
	"github.com/jmhodges/levigo"
)

var (
	databasePath     = flag.String("database-path", "tweets.db", "Path to database directory")
	dumpPath         = flag.String("dump-path", "data", "Path to data")
	dayDump          = flag.Bool("last-day", false, "Dump the last day of data")
	monthDump        = flag.Bool("last-month", false, "Dump the last month of data")
	parisLocation, _ = time.LoadLocation("Europe/Paris")
)

func fatalOnError(err error) {
	if err != nil {
		log.Fatal(err)
	}
}
func getDates() (time.Time, time.Time) {
	year, month, day := time.Now().Date()
	if *dayDump {
		start := time.Date(year, month, day-1, 0, 0, 0, 0, parisLocation)
		end := time.Date(year, month, day, 0, 0, 0, 0, parisLocation)
		return start, end
	} else {
		start := time.Date(year, month-1, 1, 0, 0, 0, 0, parisLocation)
		end := time.Date(year, month, 1, 0, 0, 0, 0, parisLocation)
		return start, end
	}
}

func getFilename() string {
	date, _ := getDates()
	if *dayDump {
		return *dumpPath + "/" + date.Format("2006-01-02.json")
	} else {
		return *dumpPath + "/" + date.Format("2006-01.json")
	}
}

func extractFromDatabase() {
	opts := levigo.NewOptions()
	opts.SetCache(levigo.NewLRUCache(10000))
	opts.SetCreateIfMissing(true)
	db, err := levigo.Open(*databasePath, opts)
	fatalOnError(err)

	readOps := levigo.NewReadOptions()
	readOps.SetFillCache(false)
	it := db.NewIterator(readOps)
	defer it.Close()

	file, err := os.Create(getFilename())
	fatalOnError(err)
	defer file.Close()

	toDelete := make([][]byte, 0)
	for it.SeekToFirst(); it.Valid(); it.Next() {
		var tweet anaconda.Tweet
		err = json.Unmarshal(it.Value(), &tweet)
		fatalOnError(err)

		tweetTime, err := tweet.CreatedAtTime()
		fatalOnError(err)

		start, end := getDates()
		if !tweetTime.After(start) || !tweetTime.Before(end) {
			continue
		}

		file.Write(it.Value())
		file.WriteString("\n")

		if *monthDump {
			toDelete = append(toDelete, it.Key())
		}
	}
	fatalOnError(it.GetError())

	if *monthDump {
		wo := levigo.NewWriteOptions()
		for _, key := range toDelete {
			db.Delete(wo, key)
		}
	}
}

func main() {
	flag.Parse()
	if !*dayDump && !*monthDump {
		log.Fatal("Set --last-day or --last-month")
	}
	extractFromDatabase()
}
