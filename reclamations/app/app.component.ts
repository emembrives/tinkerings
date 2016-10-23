import { Component } from '@angular/core';
import { createEngine } from './questions/app.questions'

@Component({
  selector: 'my-app',
  template: '<h1>{{title}}</h1><h2>{{hero}} details!</h2>'
})

export class AppComponent {
  title = 'Générateur de réclamation';
  hero = 'Windstorm';
  config = createEngine();
}
