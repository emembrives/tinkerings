package main

import (
	"github.com/gorilla/websocket"
)

type ServerMessage struct {
	VotedUsers int `json:"omitempty"`
	TotalUsers int `json:"omitempty"`
	Results map[int]int `json:"omitempty"`
	Reset bool `json:"omitempty"`
}

type User struct {
	conn  *websocket.Conn
	vote  int
	voted bool
}

func CreateUser(conn *websocket.Conn) *User {
	user := new(User)
	user.conn = conn
	user.vote = -1
	user.voted = false
	return user
}

func (u *User) Close() {
	u.conn.Close()
}

func (u *User) SendVoteProgressUpdate(voted, total int) error {
	msg := ServerMessage{VotedUsers: voted, TotalUsers: total}
	return u.conn.WriteJSON(msg)
}

func (u *User) SendVoteResults(voted, total int, results map[int]int) error {
	msg := ServerMessage{VotedUsers: voted, TotalUsers: total, Results: results}
	return u.conn.WriteJSON(msg)
}

func (u *User) SendNewVote() error {
	msg := ServerMessage{Reset: true}
	return u.conn.WriteJSON(msg)
}
