import { Component } from '@angular/core';
import { QuestionEngineService } from './questions/question-engine.service'

@Component({
  selector: 'my-app',
  template: '<h1>{{title}}</h1><h2>{{hero}} details!</h2>',
  providers: [QuestionEngineService]
})
export class AppComponent {
  title = 'Générateur de réclamation';
  hero = 'Windstorm';
  constructor(private engine: QuestionEngineService) { }
}
