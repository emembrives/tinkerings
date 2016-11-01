export class Value {
    isSet: boolean;
    variable: string;
    value: any;
}

export class State {
  values: Value[];
}

const QUESTIONS : string = `
  [
    {
      id: 1,
      introduction: "Sur quel réseau vous trouviez-vous ?",
      type: "choices",
      choices: [
        "SNCF Transilien",
        "RER RATP",
        "Métro",
        "Bus RATP",
        "Tramway",
      ]
    }
  ]`;
export class Question {
  constructor(readonly introduction: string, readonly type: string, readonly choices: string[]);

  isActive(s: State) : boolean {
    return true;
  }
}
