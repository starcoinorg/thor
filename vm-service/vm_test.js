const vm = require("./vm.js")
fs = require("fs")
const vmins = vm.createVm(fs.readFileSync("./wasm/engine_optimized.wasm"))
vmins.execute(1, 2, 8)
vmins.execute(3,)
vmins.wasm.exports.update(2, 5)