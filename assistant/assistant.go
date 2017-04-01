package main

import (
	"encoding/json"
	"net/http"
)

func handler(w http.ResponseWriter, r *http.Request) {
	decoder := json.NewDecoder(r.Body)
	var request Request
	decoder.Decode(&request)
}

func main() {
	http.HandleFunc("/", handler)
	http.ListenAndServe(":8080", nil)
}
