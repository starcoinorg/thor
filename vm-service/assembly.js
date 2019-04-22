"use strict";
// import "@types/webassembly-js-api";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
exports.__esModule = true;
var ASModuleWrapper = /** @class */ (function () {
    function ASModuleWrapper() {
        var _this = this;
        this.module = null;
        this.getString = function (value) {
            if (_this.module == null) {
                return value;
            }
            else {
                return _this.module.getString(value);
            }
        };
        this.getArray = function (type, value) {
            if (_this.module == null) {
                return value;
            }
            else {
                return _this.module.getArray(type, value);
            }
        };
    }
    ASModuleWrapper.prototype.init = function (module) {
        this.module = module;
    };
    return ASModuleWrapper;
}());
//function use array style for keep this ref.
//see https://github.com/Microsoft/TypeScript/wiki/'this'-in-TypeScript
var Console = /** @class */ (function (_super) {
    __extends(Console, _super);
    function Console() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.log = function (value) {
            console.log(_this.getString(value));
        };
        _this.logf = function (msg, value) {
            console.log(_this.getString(msg), value);
        };
        _this.logi = function (msg, value) {
            console.log(_this.getString(msg), value);
        };
        _this.logAction = function (msg, player, state) {
            console.log(_this.getString(msg) + " player:", player, _this.getArray(Int8Array, state));
        };
        _this.error = function (value) {
            alert(_this.getString(value));
        };
        return _this;
    }
    return Console;
}(ASModuleWrapper));
var Listener = /** @class */ (function (_super) {
    __extends(Listener, _super);
    function Listener() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.onUpdate = function (player, state) {
            console.log("listener onUpdate", player, _this.getArray(Int8Array, state));
        };
        _this.onGameOver = function (player) {
            console.log("listener onGameOver", player);
            alert("Game Over Winner is:" + player);
        };
        return _this;
    }
    return Listener;
}(ASModuleWrapper));
function createVm(bufSource) {
    var mod = new WebAssembly.Module(bufSource);
    var engineConsole = new Console();
    var listener = new Listener();
    var env = {
        memoryBase: 0,
        tableBase: 0,
        memory: new WebAssembly.Memory({
            initial: 0
        }),
        abort: function (msg, file, line, column) {
            console.error("Abort called at " + file + ":" + line + ":" + column + ", msg:" + msg);
        }
    };
    var instance = new WebAssembly.Instance(mod, {
        env: env,
        console: engineConsole,
        listener: listener
    });
    return new Vm(instance);
}
exports.createVm = createVm;
var Vm = /** @class */ (function () {
    function Vm(wasm) {
        this.opcodes = new Map();
        this.wasm = wasm;
        this.opcodes.set(0, this.wasm.exports.init);
        this.opcodes.set(1, this.wasm.exports.update);
    }
    Vm.prototype.execute = function (opcode) {
        var argument = [];
        for (var _i = 1; _i < arguments.length; _i++) {
            argument[_i - 1] = arguments[_i];
        }
        var fn = this.opcodes.get(opcode);
        if (fn == null) {
            //callback("Invalid opcode")
            return null;
        }
        return fn.apply(void 0, [opcode].concat(argument));
    };
    return Vm;
}());
