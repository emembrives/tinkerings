import { Engine, Question, Value } from './app.questions.def';

export function createEngine(): Engine {
  let e = new Engine();
  return e;
}

class RadioQuestion implements Question {
  private variable: string;
  constructor(template: string, priority: number, variable: string) {
    this.template = template;
    this.priority = priority;
    this.variable = variable;
  }

  isSet(): boolean {
    return false;
  }

  getValues() : Value[] {
    return [];
  }

  isActive(s: State) : boolean {
    return true;
  }
}
