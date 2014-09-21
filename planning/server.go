package main

import (
	"log"
	"net/http"
	"time"

	"github.com/gorilla/mux"
	"github.com/gorilla/websocket"
)

type UserMessage struct {
	Vote             string
	ChangedVote      bool
	RequestReset     bool
	RequestVoteClose bool
}

type PokerServer struct {
	users map[*User]bool
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

var server = new(PokerServer)

func main() {
	server.users = make(map[*User]bool)
	websocketListen()
}

func websocketListen() {
	r := mux.NewRouter()
	h := &http.Server{
		Addr:           ":6001",
		Handler:        r,
		ReadTimeout:    30 * time.Second,
		WriteTimeout:   30 * time.Second,
		MaxHeaderBytes: 1 << 20,
	}
	r.HandleFunc("/sock", websocketHandler)
	r.HandleFunc("/", homeHandler)
	h.ListenAndServe()
}

func homeHandler(w http.ResponseWriter, r *http.Request) {

}

func websocketHandler(w http.ResponseWriter, r *http.Request) {
	websocketConn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println(err)
		return
	} else {
		log.Println("Upgrading connection to websocket")
	}

	user := CreateUser(websocketConn)

	server.HandleUser(user)
}

func (ps *PokerServer) HandleUser(user *User) {
	ps.users[user] = true
	ps.DispatchVoteStatus()
	for {
		message := UserMessage{}
		err := user.conn.ReadJSON(&message)
		if err != nil {
			log.Println("Error: ", err)
			user.Close()
			break
		}
		log.Println("Receiving:", message)
		if message.ChangedVote {
			user.vote = message.Vote
            user.voted = len(user.vote) != 0
			ps.DispatchVoteStatus()
		}
		if message.RequestVoteClose {
			ps.DispatchVoteResults()
		}
		if message.RequestReset {
			ps.PrepareNewVote()
		}
	}
	delete(ps.users, user)
	log.Println("Disconnecting user")
}

func (ps *PokerServer) DispatchVoteStatus() {
	number_users := len(ps.users)
	number_voted := 0
	for user := range ps.users {
		if user.voted {
			number_voted++
		}
	}

	for user := range ps.users {
		user.SendVoteProgressUpdate(number_voted, number_users)
	}
}

func (ps *PokerServer) DispatchVoteResults() {
	results := make(map[string]int)
	number_users := len(ps.users)
	number_voted := 0
	for user := range ps.users {
		if user.voted {
			results[user.vote]++
			number_voted++
		}
	}

	for user := range ps.users {
		user.SendVoteResults(number_voted, number_users, results)
	}
}

func (ps *PokerServer) PrepareNewVote() {
	for user := range ps.users {
		user.voted = false
		user.SendNewVote()
	}
}
