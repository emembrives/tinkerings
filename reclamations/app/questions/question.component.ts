import { Input, Component } from '@angular/core';
import { Question } from './app.questions.def'

@Component({
    selector: 'question-details',
    template: `
  <p>{{ question.data.introduction }}</p>
  <input *ngIf="isInput()" type="{{ question.data.type }}"/>
  <ul *ngIf="!isInput()">
    <li *ngFor="let choice of question.data.choices">
      <a (click)="select(choice)">{{ choice }}</a>
    </li>
  </ul>`
})
export class QuestionComponent {
    @Input()
    question: Question;
    @Input()
    updater: any;
    selected: string;

    isInput(): boolean {
        return !this.question.isMultiChoice();
    }

    select(choice: string) {
        console.log("Selecting choice " + choice);
        this.selected = choice;
        this.question.setChoice(choice);
        this.updater.update();
    }
}
