import { Injectable } from '@angular/core';
import { Question, Value, State } from './app.questions.def';

@Injectable()
export class QuestionEngineService {
  private configuration: Question[];
  private state: State;

  getActiveQuestions() : Question[] {
    let active_questions : Question[];
    for (let question of this.configuration) {
      if (question.isActive(this.state)) {
        active_questions.push(question);
      }
    }
    return active_questions;
  }
}
