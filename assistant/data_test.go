package main

import (
	"encoding/json"
	"testing"
)

func TestEncoding(t *testing.T) {
	response := Response{
		ConversationToken:  "sometoken",
		ExpectUserResponse: true,
		ExpectedInputs: []ExpectedInputs{
			ExpectedInputs{
				InputPrompt: &InputPrompt{
					InitialPrompts: []SpeechResponse{
						SpeechResponse{
							TextToSpeech: "Some text to speech",
							Ssml:         "<ssml></ssml>",
						},
					},
				},
			},
		},
	}
	goldenOutput := `{"conversation_token":"sometoken","expect_user_response":true,"expected_inputs":[{"input_prompt":{"initial_prompts":[{"text_to_speech":"Some text to speech","ssml":"\u003cssml\u003e\u003c/ssml\u003e"}]}}]}`
	jsonBytes, err := json.Marshal(response)
	if err != nil {
		t.Error(err)
	}
	if string(jsonBytes) != goldenOutput {
		t.Error("Bad JSON serialization: ", string(jsonBytes))
	}
}
