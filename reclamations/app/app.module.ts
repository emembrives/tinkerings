import { NgModule }      from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppComponent }  from './app.component';
import { QuestionComponent } from './questions/question.component';
import { FormsModule }   from '@angular/forms';

@NgModule({
    imports: [BrowserModule, FormsModule],
    declarations: [AppComponent, QuestionComponent],
    bootstrap: [AppComponent]
})

export class AppModule { }
