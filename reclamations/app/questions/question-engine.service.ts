import { Injectable } from '@angular/core';
import { Question, QuestionJSON, Value, State } from './app.questions.def';

const QUESTIONS: string = `
  [
    {
      "name": "reseau",
      "introduction": "Sur quel réseau vous trouviez-vous ?",
      "type": "choices",
      "choices": [
        "SNCF Transilien",
        "RER RATP",
        "Métro",
        "Bus RATP",
        "Tramway"
      ],
      "prerequisites": []
    },
    {
      "name": "ligne_RATP",
      "introduction": "Quelle ligne de la RATP",
      "type": "choices",
      "choices": [
        "RER A",
        "RER B"
      ],
      "prerequisites": [
        {
          "variable": "reseau",
          "value": "RER RATP"
        }
      ]
    }
  ]`;

@Injectable()
export class QuestionEngineService {
    private configuration: Question[];
    private state: State = new State();

    QuestionEngineService() { }

    private populateConfiguration() {
        this.configuration = new Array<Question>();
        let questions_json: QuestionJSON[] = JSON.parse(QUESTIONS);
        for (let question_json of questions_json) {
            let question = new Question(question_json);
            this.configuration.push(question);
            this.state.values.push(question.value);
        }
    }

    getActiveQuestions(): Question[] {
        if (this.configuration === undefined) {
            this.populateConfiguration();
        }

        let active_questions = new Array<Question>();
        for (let question of this.configuration) {
            if (question.isActive(this.state)) {
                active_questions.push(question);
            }
        }
        return active_questions;
    }
}
