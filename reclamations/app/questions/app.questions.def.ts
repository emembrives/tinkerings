export class Value {
    isSet: boolean;
    variable: string;
    value: any;
}

export class State {
  values: Value[];
}

export interface Question {
  template: string;
  priority: number;

  isSet(): boolean;
  getValues() : Value[];
  isActive(s: State) : boolean;
}

export class Engine {
  configuration: Question[];
  state: State;
}
