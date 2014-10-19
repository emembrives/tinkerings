package main

import (
	"encoding/json"
	"fmt"
	"labix.org/v2/mgo"
	"labix.org/v2/mgo/bson"
	"log"
	"net"
	"net/http"
	"net/http/fcgi"
	"strings"
)

type FastCGIServer struct{}

func (s *FastCGIServer) ServeHTTP(w http.ResponseWriter, req *http.Request) {
	ServeHTTP(w, req)
}

func ServeHTTP(w http.ResponseWriter, req *http.Request) {
	url := req.URL
	urlComponents := strings.Split(url.Path, "/")
	log.Println(urlComponents)
	if urlComponents[1] != "app" || len(urlComponents) < 3 {
		log.Println("Not in app:", urlComponents)
		return
	}
	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()

	// Optional. Switch the session to a monotonic behavior
	session.SetMode(mgo.Monotonic, true)

	if urlComponents[2] == "GetLines" {
		c := session.DB("dispotrains").C("lines")
		var lines []bson.M = make([]bson.M, 0)
		c.Find(nil).Sort("value.network", "value.id").All(&lines)
		jsonLines := make([]bson.M, 0)
		for _, line := range lines {
			jsonLines = append(jsonLines, line["value"].(bson.M))
		}
		json.NewEncoder(w).Encode(&jsonLines)
	} else if urlComponents[2] == "GetStation" && len(urlComponents) >= 4 {
		var stationName string = urlComponents[3]
		c := session.DB("dispotrains").C("stations")
		var station bson.M
		c.Find(bson.M{"name": stationName}).One(&station)
		json.NewEncoder(w).Encode(&station)
	} else if urlComponents[2] == "GetStations" {
		c := session.DB("dispotrains").C("stations")
		var stations []bson.M = make([]bson.M, 0)
		if err := c.Find(nil).All(&stations); err != nil {
			log.Println(err)
		}
		jsonStations := make([]bson.M, 0)
		for _, station := range stations {
			delete(station, "_id")
			jsonStations = append(jsonStations, station)
		}
		json.NewEncoder(w).Encode(&jsonStations)
	} else {
		log.Println("Not a recognized action")
	}
}

/*func main() {
    http.HandleFunc("/", ServeHTTP)
    http.ListenAndServe(":8080", nil)
}*/

func main() {
	fmt.Printf("Starting server")
	l, _ := net.Listen("tcp", "127.0.0.1:9000")
	b := new(FastCGIServer)
	fcgi.Serve(l, b)
}
