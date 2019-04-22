// import "@types/webassembly-js-api";

import {ASUtil, TypedArrayConstructor} from "assemblyscript/lib/loader";


class ASModuleWrapper {
  module: ASUtil | null = null;

  init(module: ASUtil): void {
    this.module = module;
  }

  protected getString = (value: number) => {
    if (this.module == null) {
      return value;
    } else {
      return this.module.getString(value);
    }
  };

  protected getArray = (type: TypedArrayConstructor, value: number) => {
    if (this.module == null) {
      return value;
    } else {
      return this.module.getArray(type, value);
    }
  };
}

//function use array style for keep this ref.
//see https://github.com/Microsoft/TypeScript/wiki/'this'-in-TypeScript
class Console extends ASModuleWrapper {

  public log = (value: number) => {
    console.log(this.getString(value));
  };
  public logf = (msg: number, value: number) => {
    console.log(this.getString(msg), value)
  };
  public logi = (msg: number, value: number) => {
    console.log(this.getString(msg), value)
  };
  public logAction = (msg: number, player: number, state: number) => {
    console.log(this.getString(msg) + " player:", player, this.getArray(Int8Array, state))
  };
  public error = (value: number) => {
    alert(this.getString(value));
  };
}

class Listener extends ASModuleWrapper {

  public onUpdate = (player: number, state: number) => {
    console.log("listener onUpdate", player, this.getArray(Int8Array, state));
  };

  public onGameOver = (player: number) => {
    console.log("listener onGameOver", player);
    alert("Game Over Winner is:" + player);
  }
}


export function createVm(bufSource: Buffer): Vm {
  let mod = new WebAssembly.Module(bufSource);
  let engineConsole = new Console();
  let listener = new Listener();
  let env = {
    memoryBase: 0,
    tableBase: 0,
    memory: new WebAssembly.Memory({
      initial: 0
    }),
    abort(msg: number, file: number, line: number, column: number) {
      console.error("Abort called at " + file + ":" + line + ":" + column + ", msg:" + msg);
    }
  };
  const instance = new WebAssembly.Instance(mod, {
    env: env,
    console: engineConsole,
    listener: listener,
  });
  return new Vm(instance);
}

class Vm {
  readonly opcodes: Map<number, any>;
  readonly wasm: WebAssembly.Instance;

  constructor(wasm: WebAssembly.Instance) {
    this.opcodes = new Map();
    this.wasm = wasm;
    this.opcodes.set(0, this.wasm.exports.init);
    this.opcodes.set(1, this.wasm.exports.update);
  }

  execute(opcode, ...argument): any {
    let fn = this.opcodes.get(opcode)
    if (fn == null) {
      // Todo: return error
      // callback("Invalid opcode")
      return null
    }
    return fn(opcode, ...argument)
  }
}

