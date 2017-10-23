package main

import (
	"compress/gzip"
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"math"
	"math/rand"
	"net/http"
	"os"
	"time"
)

const (
	minLat = 48.685521
	maxLat = 49.014455
	minLng = 2.139587
	maxLng = 2.622986

	serverToken = "vvC5OPzilGbQ9jMhgIxVTSAnyjCyX0edZzvWwVoJ"
	apiEndpoint = "https://api.uber.com/v1.2/estimates/time?start_latitude=%f&start_longitude=%f"
)

var (
	latFlag = flag.Float64("lat", 0.0, "latitude")
	lngFlag = flag.Float64("lng", 0.0, "longitude")
	loop    = flag.Bool("loop", false, "Loop the requests")
)

type result struct {
	Time      time.Time
	Latitude  float64
	Longitude float64
	UberX     int64
	Access    int64
	Error     error
}

func (r result) ToCSV() []string {
	row := make([]string, 6)
	row[0] = r.Time.Format("2006-01-02T15:04:05.999999999Z-0700")
	row[1] = fmt.Sprintf("%f", r.Latitude)
	row[2] = fmt.Sprintf("%f", r.Longitude)
	row[3] = fmt.Sprintf("%d", r.UberX)
	row[4] = fmt.Sprintf("%d", r.Access)
	row[5] = fmt.Sprintf("%v", r.Error)
	return row
}

func getRandomLocation() (float64, float64) {
	lat := rand.Float64()*(maxLat-minLat) + minLat
	lng := rand.Float64()*(maxLng-minLng) + minLng
	return lat, lng
}

func makeApiCall(lat, lng float64) (int64, int64, error) {
	client := &http.Client{}

	req, err := http.NewRequest("GET", fmt.Sprintf(apiEndpoint, lat, lng), nil)
	if err != nil {
		return -1, -1, err
	}

	req.Header.Add("Authorization", "Token "+serverToken)
	req.Header.Add("Accept-Language", "en_US")
	req.Header.Add("Content-Type", "application/json")
	resp, err := client.Do(req)
	if err != nil {
		return -1, -1, err
	}
	decoder := json.NewDecoder(resp.Body)
	data := make(map[string][]map[string]interface{})
	if err = decoder.Decode(&data); err != nil {
		return -1, -1, err
	}

	var uberX, access float64 = -1, -1
	for _, entry := range data["times"] {
		rawName := entry["display_name"]
		name, ok := rawName.(string)
		if !ok {
			return -1, -1, fmt.Errorf("not a valid entry: %v, %+v", entry, rawName)
		}
		if name == "uberX" {
			rawValue := entry["estimate"]
			uberX, ok = rawValue.(float64)
			if !ok {
				return -1, -1, fmt.Errorf("not a valid value: %+v, %T", entry, rawValue)
			}
		} else if name == "ACCESS" {
			rawValue := entry["estimate"]
			access, ok = rawValue.(float64)
			if !ok {
				return -1, -1, fmt.Errorf("not a valid entry: %v", entry)
			}
		}
	}
	return int64(uberX), int64(access), err
}

func makeOneRequest() result {
	var lat, lng float64
	if math.Abs(*latFlag) < 1e-5 && math.Abs(*lngFlag) < 1e-5 {
		lat, lng = getRandomLocation()
	} else {
		lat = *latFlag
		lng = *lngFlag
	}
	uberX, access, err := makeApiCall(lat, lng)
	result := result{
		Time:      time.Now(),
		Latitude:  lat,
		Longitude: lng,
		UberX:     uberX,
		Access:    access,
		Error:     err,
	}
	return result
}

func main() {
	flag.Parse()
	rand.Seed(time.Now().UnixNano())

	if !*loop {
		result := makeOneRequest()
		json, err := json.Marshal(result)
		if err != nil {
			log.Fatalf("Error while encoding json: %v", err)
		}
		fmt.Printf("%s\n", json)
		return
	}

	file, err := os.Create(fmt.Sprintf("accesslog-%s.log.gz", time.Now()))
	if err != nil {
		log.Fatalf("Unable to open file: %s", err)
	}
	defer file.Close()
	compressed, err := gzip.NewWriterLevel(file, gzip.BestCompression)
	if err != nil {
		log.Fatalf("Unable to compress file: %s", err)
	}
	defer compressed.Close()
	csvWriter := csv.NewWriter(compressed)
	iter := 0
	for {
		iter++
		result := makeOneRequest()
		csvWriter.Write(result.ToCSV())
		csvWriter.Flush()
		if iter%100 == 0 {
			compressed.Flush()
			iter = 0
		}
		time.Sleep(2 * time.Second)
	}
}
