package main

import (
	"encoding/json"
	"html/template"
	"log"
	"net/http"

	"github.com/emembrives/tinkerings/dispotrains.webapp/storage"

	"github.com/eknkc/dateformat"
	"github.com/gorilla/mux"
	"labix.org/v2/mgo"
	"labix.org/v2/mgo/bson"
)

var (
	homeTmpl    = template.Must(template.ParseFiles("templates/lines.html", "templates/footer.html", "templates/header.html"))
	lineTmpl    = template.Must(template.ParseFiles("templates/line.html", "templates/footer.html", "templates/header.html"))
	stationTmpl = template.Must(template.ParseFiles("templates/station.html", "templates/footer.html", "templates/header.html"))
)

type Line struct {
	Network      string
	ID           string
	GoodStations []*storage.Station `bson:"goodStations"`
	BadStations  []*storage.Station `bson:"badStations"`
}

type LineHolder struct {
	Value Line
}

type LineSlice []LineHolder

type DisplayStation struct {
	storage.Station
	DisplayName string
	Elevators   []*LocElevator
}

type LocElevator storage.Elevator

func (e *LocElevator) LocalStatusDate() string {
	return dateformat.FormatLocale(e.Status.LastUpdate, "ddd D MMM à HH:MM", dateformat.French)
}

func (ls LineSlice) Lines() []Line {
	r := make([]Line, len(ls))
	for i, v := range ls {
		r[i] = v.Value
	}
	return r
}

func HomeHandler(w http.ResponseWriter, req *http.Request) {
	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()
	c := session.DB("dispotrains").C("lines")
	var lines LineSlice = make(LineSlice, 0)
	c.Find(nil).Sort("value.network", "value.id").All(&lines)
	homeTmpl.Execute(w, lines.Lines())
}

func LineHandler(w http.ResponseWriter, req *http.Request) {
	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()
	c := session.DB("dispotrains").C("lines")

	vars := mux.Vars(req)
	lineId := vars["line"]

	var line LineHolder
	c.Find(bson.M{"_id": lineId}).One(&line)
	if err = lineTmpl.Execute(w, line.Value); err != nil {
		log.Fatal(err)
	}
}

func StationHandler(w http.ResponseWriter, req *http.Request) {
	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()
	c := session.DB("dispotrains").C("stations")

	vars := mux.Vars(req)
	stationName := vars["station"]

	var station DisplayStation
	c.Find(bson.M{"name": stationName}).One(&station)
	if err = stationTmpl.Execute(w, station); err != nil {
		log.Fatal(err)
	}
}

func AppHandler(w http.ResponseWriter, req *http.Request) {
	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()
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
}

func CacheRequest(h http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Cache-control", "public, max-age=259200")
		h.ServeHTTP(w, r)
	})
}

func main() {
	r := mux.NewRouter()
	r.HandleFunc("/", HomeHandler)
	r.HandleFunc("/ligne/{line}", LineHandler)
	r.HandleFunc("/gare/{station}", StationHandler)
	r.HandleFunc("/app/GetStations/", AppHandler)
	r.PathPrefix("/static/").Handler(CacheRequest(http.StripPrefix("/static/", http.FileServer(http.Dir("static")))))
	http.Handle("/", r)
	log.Fatal(http.ListenAndServe(":9000", nil))
}
