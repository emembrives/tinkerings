import { Component } from '@angular/core';
import { Question } from './app.questions.def'

@Component({
  selector: 'question',
  template: `
  <p>{{ introduction }}</p>
  <input *ngIf="isInput()" type="{{ type }}"></input>
  <div *ngIf="!isInput()">
    <li *ngFor="let choice of choices">
      <a (click)="selected(choice)">{{ choice }}</a>
    </li>
  </div>`
})
export class QuestionComponent {
  constructor(private question: Question) {};

  introduction() : string {
    return this.question.introduction;
  }

  isInput() : boolean {
    return !this.question.isMultiChoice();
  }

  choices() : string[] {
    return this.question.choices();
  }
}
