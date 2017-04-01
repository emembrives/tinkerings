package main

type Request struct {
	User         User         `json:"user"`
	Device       Device       `json:"device"`
	Conversation Conversation `json:"conversation"`
	Inputs       []Inputs     `json:"inputs"`
}

type User struct {
	UserId      string      `json:"user_id"`
	Profile     UserProfile `json:"profile"`
	AccessToken string      `json:"access_token"`
}

type UserProfile struct {
	GivenName   string `json:"given_name"`
	FamilyName  string `json:"family_name"`
	DisplayName string `json:"display_name"`
}

type Device struct {
	Location Location `json:"location"`
}

type Location struct {
	Coordinates      Coordinates `json:"coordinates"`
	FormattedAddress string      `json:"formatted_address"`
	City             string      `json:"city"`
	Zipcode          string      `json:"zipcode"`
}

type Coordinates struct {
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

type Conversation struct {
	ConversationId    string `json:"conversation_id"`
	Type              string `json:"type"`
	ConversationToken string `json:"conversation_token"`
}

type Inputs struct {
	Intent    string      `json:"intent"`
	RawInputs []RawInputs `json:"raw_inputs"`
	Arguments []Arguments `json:"arguments"`
}

type RawInputs struct {
	CreateTime Time   `json:"create_time"`
	InputType  string `json:"input_type"`
	Query      string `json:"query"`
}

type Time struct {
	Seconds int64 `json:"seconds"`
	Nanos   int64 `json:"nanos"`
}

type Arguments struct {
	Name    string `json:"name"`
	RawText string `json:"raw_text"`
}

type Response struct {
	ConversationToken  string           `json:"conversation_token"`
	ExpectUserResponse bool             `json:"expect_user_response"`
	ExpectedInputs     []ExpectedInputs `json:"expected_inputs,omitempty"`
	FinalResponse      *SpeechResponse  `json:"final_response,omitempty"`
}

type ExpectedInputs struct {
	InputPrompt     *InputPrompt     `json:"input_prompt,omitempty"`
	PossibleIntents []ExpectedIntent `json:"possible_intents,omitempty"`
}

type InputPrompt struct {
	InitialPrompts []SpeechResponse `json:"initial_prompts,omitempty"`
	NoInputPrompts []SpeechResponse `json:"no_input_prompts,omitempty"`
}

type ExpectedIntent struct {
	Intent string `json:"intent"`
}

type SpeechResponse struct {
	TextToSpeech string `json:"text_to_speech,omitempty"`
	Ssml         string `json:"ssml,omitempty"`
}
