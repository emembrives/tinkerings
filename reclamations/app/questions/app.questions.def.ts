export interface IValue {
    isSet: boolean
    variable: string
    value: any
}

export class Value implements IValue {
    isSet: boolean
    value: any

    constructor(readonly variable: string) {
        this.isSet = false;
    }

    setValue(value: any) {
        this.value = value;
        this.isSet = true;
    }
}

export class State {
    values: Value[] = new Array<Value>();

    hasValue(v: Value): boolean {
        for (let value of this.values) {
            if (value.isSet && v.variable === value.variable && v.value === value.value) {
                return true;
            }
        }
        return false;
    }
}

export interface QuestionJSON {
    name: string
    introduction: string
    type: string
    choices: string[]
    prerequisites: Value[]
}

export class Question {
    value: Value;

    constructor(readonly data: QuestionJSON) {
        this.value = new Value(data.name);
    }

    isActive(state: State): boolean {
        for (let prerequisite of this.data.prerequisites) {
            if (!state.hasValue(prerequisite)) {
                console.log("Question " + this.data.name + " not active");
                return false;
            }
        }
        return true;
    }

    isMultiChoice(): boolean {
        return this.data.type == "choices";
    }

    setChoice(choice: string) {
        this.value.setValue(choice);
    }
}
