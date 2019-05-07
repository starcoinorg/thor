//import "@types/webassembly-js-api";

import {ASUtil, TypedArrayConstructor} from "assemblyscript/lib/loader";
import * as fs from 'fs';

const env = {
  memoryBase: 0,
  tableBase: 0,
  memory: new WebAssembly.Memory({
    initial: 0
  }),
  abort(msg: number, file: number, line: number, column: number) {
    console.error("abort called at " + file + ":" + line + ":" + column + ", msg:" + msg);
  }
};

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


const engineConsole = new Console();
const listener = new Listener();

let mod = new WebAssembly.Module(fs.readFileSync("../../../wasm/engine_optimized.wasm"));

const instance=new WebAssembly.Instance(mod, {
  env: env,
  console: engineConsole,
  listener: listener,
});
module.exports=instance.exports