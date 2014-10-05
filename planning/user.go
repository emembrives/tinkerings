package main

import (
	"log"
	"strconv"
	"sort"

	"github.com/gorilla/websocket"
)

type Result struct {
	Vote string
	Value int
}

type ByVote []Result

func (a ByVote) Len() int {return len(a)}
func (a ByVote) Swap(i, j int) { a[i], a[j] = a[j], a[i] }
func (a ByVote) Less(i, j int) bool {
	floatI, errI := strconv.ParseFloat(a[i].Vote, 32)
	floatJ, errJ := strconv.ParseFloat(a[j].Vote, 32)
	if errI != nil && errJ != nil {
		return a[i].Vote < a[j].Vote
	} else if errI != nil {
		return true
	} else if errJ != nil {
		return false
	} else {
		return floatI < floatJ
	}
}

type ServerMessage struct {
	VotedUsers int
	TotalUsers int
	Results    []Result `json:"Results,omitempty"`
	Reset      bool
}

type User struct {
	conn  *websocket.Conn
	vote  string
	voted bool
}

func CreateUser(conn *websocket.Conn) *User {
	user := new(User)
	user.conn = conn
	user.voted = false
	return user
}

func (u *User) Close() {
	u.conn.Close()
}

func (u *User) SendVoteProgressUpdate(voted, total int) error {
	msg := ServerMessage{VotedUsers: voted, TotalUsers: total}
	log.Println("Sending", msg)
	return u.conn.WriteJSON(msg)
}

func (u *User) SendVoteResults(voted, total int, results map[string]int) error {
	resultItems := make([]Result, 0)
	for k, v := range results {
		resultItems = append(resultItems, Result{k, v})
	}
	sort.Sort(ByVote(resultItems))
	msg := ServerMessage{VotedUsers: voted, TotalUsers: total, Results: resultItems}
	log.Println("Sending", msg)
	return u.conn.WriteJSON(msg)
}

func (u *User) SendNewVote() error {
	msg := ServerMessage{Reset: true}
	log.Println("Sending", msg)
	return u.conn.WriteJSON(msg)
}
