package main

import (
	"log"

	"github.com/gorilla/websocket"
)

type ServerMessage struct {
	VotedUsers int
	TotalUsers int
	Results    map[string]int `json:"Results,omitempty"`
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
	msg := ServerMessage{VotedUsers: voted, TotalUsers: total, Results: results}
	log.Println("Sending", msg)
	return u.conn.WriteJSON(msg)
}

func (u *User) SendNewVote() error {
	msg := ServerMessage{Reset: true}
	log.Println("Sending", msg)
	return u.conn.WriteJSON(msg)
}
