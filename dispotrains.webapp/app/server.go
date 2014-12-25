package main

import (
	"html/template"
    "log"
	"net/http"

    "github.com/sterops/tinkerings/dispotrains.webapp/storage"

	"github.com/gorilla/mux"
	"labix.org/v2/mgo"
	"labix.org/v2/mgo/bson"
)

var (
	homeTmpl    = template.Must(template.ParseFiles("templates/lines.html"))
	lineTmpl    = template.Must(template.ParseFiles("templates/line.html"))
	stationTmpl = template.Must(template.ParseFiles("templates/station.html"))
)

type Line struct {
	Network  string
	ID       string
    GoodStations []*storage.Station `bson:"goodStations"`
    BadStations []*storage.Station `bson:"badStations"`
}

type LineHolder struct {
	Value Line
}

type LineSlice []LineHolder

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

	var station storage.Station
    c.Find(bson.M{"name": stationName}).One(&station)
	if err = stationTmpl.Execute(w, station); err != nil {
        log.Fatal(err)
    }
}

func main() {
	r := mux.NewRouter()
	r.HandleFunc("/", HomeHandler)
	r.HandleFunc("/ligne/{line}", LineHandler)
	r.HandleFunc("/gare/{station}", StationHandler)
    r.PathPrefix("/static/").Handler(http.StripPrefix("/static/", http.FileServer(http.Dir("static"))))
	http.Handle("/", r)
    log.Fatal(http.ListenAndServe(":8080", nil))
}
