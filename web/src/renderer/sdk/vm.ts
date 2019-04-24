//import "@types/webassembly-js-api";
// this is a shallow wrapper for the assemblyscript loader
import * as as2d from "as2d";
import * as loader from "assemblyscript/lib/loader";
import {GameEngine} from "./GameEngine";
import {GameGUI} from "./GameGUI";
import {ICanvasSYS} from "as2d/src/util/ICanvasSYS";

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
  module: loader.ASUtil | null = null;

  init(module: loader.ASUtil): void {
    this.module = module;
  }

  protected getString = (value: number) => {
    if (this.module == null) {
      return value;
    } else {
      return this.module.getString(value);
    }
  };

  protected getArray = (type: loader.TypedArrayConstructor, value: number) => {
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
const guiConsole = new Console();
const listener = new Listener();

let module: ICanvasSYS & loader.ASUtil & GameGUI;
let modulePromise: Promise<ICanvasSYS & loader.ASUtil & GameGUI>;

export function init(playerRole: number, onStateUpdate: (fullState: Int8Array, state: Int8Array) => void, engineBuffer: Buffer, guiBuffer: Buffer, playWithAI: boolean = false): Promise<ICanvasSYS & loader.ASUtil & GameGUI> {
  const engineBlob = new Blob([engineBuffer], {type: "application/wasm"});
  const guiBlob = new Blob([guiBuffer], {type: "application/wasm"});
  const engineURL = URL.createObjectURL(engineBlob);
  const guiURL = URL.createObjectURL(guiBlob);

  modulePromise = loader.instantiateStreaming<GameEngine>(fetch(engineURL), {
    env: env,
    console: engineConsole,
    listener: listener
  }).then(engine => {
    engineConsole.init(engine);
    listener.init(engine);
    engine.init();
    URL.revokeObjectURL(engineURL);
    return as2d.instantiateStreaming<GameGUI>(fetch(guiURL), {
      env: env, console: guiConsole, engine: {

        update(player: number, state: number) {
          let pointer = engine.newArray(module.getArray(Int8Array, state));
          return engine.update(player, pointer)
        },
        loadState(fullState: number) {
          let pointer = engine.newArray(module.getArray(Int8Array, fullState));
          engine.loadState(pointer)
        },
        getState() {
          return module.newArray(engine.getArray(Int8Array, engine.getState()))
        },
        isGameOver() {
          return engine.isGameOver()
        }
      }
    }).then(gui => {
      module = gui;
      guiConsole.init(module);
      const canvas = <HTMLCanvasElement>document.querySelector("#as2d");
      const ctx = canvas!.getContext("2d")!;

      ctx.canvas.addEventListener("click", (e: MouseEvent) => {
        let rect: ClientRect = (e.target as HTMLCanvasElement).getBoundingClientRect();
        let statePointer = module.onClick(e.clientX - rect.left, e.clientY - rect.top);
        let state: Int8Array = module.getArray(Int8Array, statePointer);
        if (state.length > 0) {
          let fullState: Int8Array = engine.getArray(Int8Array, engine.getState());
          onStateUpdate(fullState, state);
        }
      });

      module.useContext("main", ctx);
      module.init(playerRole, playWithAI);
      module.draw();
      URL.revokeObjectURL(engineURL);
      return module;
    });
  });
  return modulePromise;
}

export async function startGame() {
  let module = await modulePromise;
  module.startGame();
}

export async function rivalUpdate(state: Int8Array) {
  let module = await modulePromise;
  let pointer = module.newArray(state);
  module.rivalUpdate(pointer);
}

