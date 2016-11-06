import { Component } from '@angular/core';
import { Question } from './questions/app.questions.def';
import { QuestionEngineService } from './questions/question-engine.service'

@Component({
    selector: 'my-app',
    template: `<h1>{{title}}</h1>
  <ul>
    <li *ngFor="let question of activeQuestions">
        <question-details [question]="question" [updater]="this"></question-details>
    </li>
<ul>`,
    providers: [QuestionEngineService]
})
export class AppComponent {
    title = 'Générateur de réclamation';
    activeQuestions: Question[];
    constructor(private engine: QuestionEngineService) {
        this.update();
    }

    update() {
        console.log("Updating active questions");
        this.activeQuestions = this.engine.getActiveQuestions();
    }
}
