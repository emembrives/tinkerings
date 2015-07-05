package main

import (
	"encoding/json"
	"html/template"
	"log"
	"net/http"
	"sort"
	"time"

	"github.com/emembrives/tinkerings/dispotrains.webapp/storage"

	"github.com/eknkc/dateformat"
	"github.com/gorilla/mux"
	mgo "gopkg.in/mgo.v2"
	"gopkg.in/mgo.v2/bson"
)

var (
	homeTmpl         = template.Must(template.ParseFiles("templates/lines.html", "templates/footer.html", "templates/header.html"))
	lineTmpl         = template.Must(template.ParseFiles("templates/line.html", "templates/footer.html", "templates/header.html"))
	stationTmpl      = template.Must(template.ParseFiles("templates/station.html", "templates/footer.html", "templates/header.html"))
	stationStatsTmpl = template.Must(template.ParseFiles("templates/stats.html", "templates/footer.html", "templates/header.html"))
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
	Name        string
	DisplayName string
	Elevators   []*LocElevator
}

type DataStatus struct {
	Elevator   string
	State      string
	Lastupdate time.Time
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

func StatsHandler(w http.ResponseWriter, req *http.Request) {
	session, err := mgo.Dial("localhost")
	if err != nil {
		panic(err)
	}
	defer session.Close()
	c_stations := session.DB("dispotrains").C("stations")
	c_statuses := session.DB("dispotrains").C("statuses")

	vars := mux.Vars(req)
	stationName := vars["station"]

	var station DisplayStation
	c_stations.Find(bson.M{"name": stationName}).One(&station)
	elevatorIds := make([]string, 0)
	for _, stationElevator := range station.Elevators {
		elevatorIds = append(elevatorIds, stationElevator.ID)
	}
	var dbStatuses []DataStatus

	index := mgo.Index{
		Key: []string{"elevator"},
	}
	err = c_stations.EnsureIndex(index)
	if err != nil {
		panic(err)
	}
	c_statuses.Find(bson.M{"elevator": bson.M{"$in": elevatorIds}}).
		Sort("lastupdate").
		All(&dbStatuses)

	events, reports := statusesToEvents(dbStatuses)

	templateData := struct {
		Station   DisplayStation
		Events    map[string][]string
		Reports   []string
		StartDate string
		EndDate   string
	}{station, events, reports, reports[0], reports[len(reports)-1]}
	if err = stationStatsTmpl.Execute(w, templateData); err != nil {
		log.Fatal(err)
	}
}

func statusesToEvents(dbStatuses []DataStatus) (map[string][]string, []string) {
	events := make(map[string][]string)
	reportSet := make(map[string]bool)
	for _, status := range dbStatuses {
		dateStr := status.Lastupdate.Format(time.RFC3339)
		reportSet[dateStr] = true
		if _, ok := events[status.Elevator]; !ok {
			events[status.Elevator] = make([]string, 0)
		}
		if status.State != "Disponible" {
			events[status.Elevator] = append(events[status.Elevator], dateStr)
		}
	}

	reports := make(sort.StringSlice, 0, len(reportSet))
	for key, _ := range reportSet {
		reports = append(reports, key)
	}
	reports.Sort()
	return events, reports
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
	r.HandleFunc("/gare/{station}/stats", StatsHandler)
	r.HandleFunc("/app/GetStations/", AppHandler)
	r.PathPrefix("/static/").Handler(CacheRequest(http.StripPrefix("/static/", http.FileServer(http.Dir("static")))))
	http.Handle("/", r)
	log.Fatal(http.ListenAndServe(":9000", nil))
}
