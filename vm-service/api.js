const express = require('express');
const asm = require("./vm");
const vms = new Map();
const fs = require("fs");

const router = express.Router();
router.post("/vm", async function (req, res) {
  let vmid = req.body.id;
  if (vmid == null) {
    return res.status(400).send("vm id not set");
  }

  let vm = vms.get(vmid);
  let codeSrc = "";
  res.id = vmid;
  if (vm != null) {
    return res.send("Vm created before");
  }
  if (req.body.source != null && req.body.source != 0) {
    codeSrc = Buffer.from(req.body.source, "base64");

  } else if (req.srcpath != null) {
    try {
      codeSrc = fs.readFileSync(req.srcpath);
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
  let vmid = req.body.id;
  let vm = vms.get(vmid);
  if (vm == null) {
    return res.status(400).send("Vm not created:" + vmid);
  }
  if (req.body.cmd == null || req.body.cmd.length == 0) {
    return res.status(400).send("Cmd invalid");
  }
  let cmd = req.body.cmd.split(",");
  console.log("cmd:",cmd)
  let opcode = parseInt(cmd.shift());
  let vmout = vm.execute(opcode, ...cmd)
  console.log("result:",vmout)
  return res.status(200).send("" + vmout)
});

module.exports = router;
