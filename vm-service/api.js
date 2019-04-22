const express = require('express');
const asm = require("./vm");
const vms = new Map();
const fs = require("fs");

const router = express.Router();
router.post("/vm", async function (req, res) {
  let vmid = parseInt(req.body.id);
  if (isNaN(vmid)) {
    return res.status(400).send("Vm id invalid")
  }
  let vm = vms.get(vmid);
  let codeSrc = "";
  res.id = vmid;
  if (vm != null) {
    return res.send("Vm created before");
  }
  if (req.body.source != null && req.body.source.length != 0) {
    codeSrc = Buffer.from(req.body.source);
  } else if (req.body.srcpath != null) {
    try {
      codeSrc = fs.readFileSync(req.body.srcpath);
    } catch (e) {
      return res.status(400).send("Read source of vm from srcpath failed");
    }

  } else {
    return res.status(400).send("Invalid vm source code");
  }
  try {
    let vm = asm.createVm(codeSrc);
    vms.set(vmid, vm);
  } catch (e) {
    console.log(e);
    return res.status(400).send("Init vm failed");
  }
  return res.send("Vm created with id:" + vmid);
});

router.post('/execute', async function (req, res) {
  let vmid = parseInt(req.body.id);
  let vm = vms.get(vmid);
  if (vm == null) {
    return res.status(400).send("Vm not created:" + vmid);
  }
  if (req.body.cmd == null || req.body.cmd.length == 0) {
    return res.status(400).send("Cmd invalid");
  }
  let cmd = req.body.cmd.split(",");
  opcode = parseInt(cmd.shift());
  return res.send({
    "opcode": opcode,
    "result": vm.execute(opcode, ...cmd)
  });

});

module.exports = router;