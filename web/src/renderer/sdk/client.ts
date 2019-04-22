import crypto from "./crypto";

let W3CWebSocket = require('websocket').w3cwebsocket;


let client: typeof W3CWebSocket;
let wsServer: string;
let httpServer: string;
let myKeyPair: crypto.ECPair;
let serverPubKey: crypto.ECPair;
let myAddress: string;

let handlers: { (msg: WsMsg): void }[] = [];

enum HttpMsgType {
  DEF,
  CREATE_GAME,
  GAME_LIST,
  CREATE_ROOM,
  ROOM_LIST,
  ROOM,
  ERR
}

export enum WSMsgType {
  NONCE,
  CREATE_ROOM_REQ,
  CREATE_ROOM_RESP,
  JOIN_ROOM_REQ,
  JOIN_ROOM_RESP,
  HASH_REQ,
  HASH_RESP,
  INVOICE_REQ,
  INVOICE_RESP,
  READY_REQ,
  READY_RESP,
  GAME_BEGIN,
  SURRENDER_REQ,
  SURRENDER_RESP,
  CHALLENGE_REQ,
  ROOM_GAME_DATA_MSG,
  ROOM_COMMON_DATA_MSG,
  UNKNOWN
}

class SignMsg {
  msg: WsMsg;
  sign: string;

  constructor(msg: WsMsg, sign: string) {
    this.msg = msg;
    this.sign = sign;
  }

  toJSON(): string {
    return JSON.stringify(this);
  }

  static fromJSON(json: any) {
    return new SignMsg(WsMsg.fromJSON(json.msg), json.sign)
  }

  verify(pubKey: crypto.ECPair): boolean {
    let json = JSON.stringify(this.msg);
    console.log("verify result:", pubKey.verify(new Buffer(json), new Buffer(this.sign)));
    return true
  }
}

export class WsMsg {
  type: WSMsgType;
  userId: string;
  data: any;

  constructor(type: WSMsgType, userId: string, data: any) {
    this.type = type;
    this.userId = userId;
    this.data = data;
  }

  toJSON(): string {
    return JSON.stringify(this);
  }

  sign(key: crypto.ECPair): string {
    return key.sign(new Buffer(this.toJSON())).toString('base64')
  }

  static fromJSON(json: any) {
    let type: string = json.type;
    return new WsMsg(WSMsgType[type as keyof typeof WSMsgType], json.userId, json.data);
  }
}

export class WitnessData {
  preSign: string;
  data: Buffer;
  sign: string;


  constructor(preSign: string, data: Buffer) {
    this.preSign = preSign;
    this.data = data;
    this.sign = "";
  }

  doSign(key: crypto.ECPair): void {
    this.sign = key.sign(this.data).toString('base64');
  }

  toJSONObj(): any {
    return {"preSign": this.preSign, "data": this.data.toString('base64'), "sign": this.sign}
  }
}

function check(condition: boolean, msg?: string): void {
  if (!condition) {
    if (msg) {
      throw msg;
    } else {
      throw "check fail."
    }
  }
}

export function init(_keyPair: crypto.ECPair, ws = 'ws://localhost:8082/ws', http = 'http://localhost:8082') {
  myKeyPair = _keyPair;
  myAddress = crypto.toAddress(myKeyPair);
  console.log("my address", myAddress);
  wsServer = ws;
  httpServer = http;
  client = new W3CWebSocket(wsServer);

  client.onerror = function () {
    console.log('Connection Error');
  };

  client.onopen = function () {
    console.log('WebSocket Client Connected');

    function conn() {
      if (client.readyState === client.OPEN) {
        //let msg = {"type": 1, "from": "abc", "to": "server", "data": ""}
        //TODO connection message
        //client.send(JSON.stringify(msg));
      }
    }

    conn();
  };

  client.onclose = function () {
    console.log('WebSocket Client Closed');
  };

  client.onmessage = function (e: any) {
    if (typeof e.data === 'string') {
      console.log("Received: '" + e.data + "'");
      let obj = JSON.parse(e.data);
      let signMsg = SignMsg.fromJSON(obj);
      let msg = signMsg.msg;
      if (msg.type == WSMsgType.NONCE) {
        let nonce: string = msg.data.nonce;
        let pubKey: string = msg.data.pubKey;
        serverPubKey = crypto.fromPublicKey(new Buffer(pubKey, 'base64'));
        check(signMsg.verify(serverPubKey));
        //TODO why TS2722
        send(WSMsgType.NONCE, {nonce: nonce, pubKey: (<any>myKeyPair).getPublicKey().toString('base64')});
      } else {
        check(signMsg.verify(serverPubKey));
        fire(msg);
      }
    }
  };
}

function post(type: HttpMsgType, data: any) {
  let body = {
    "type": HttpMsgType[type],
    "data": data
  };
  console.log("httpServer:", httpServer);
  return fetch(httpServer + "/p", {
    method: "POST",
    body: JSON.stringify(body),
    mode: "cors",
    headers: {
      "Content-Type": "application/json;charset=UTF-8"
    }
  }).then(response => {
    console.log("request resp:", response);
    return response.json()
  })
}

export function gameList() {
  return post(HttpMsgType.GAME_LIST, {page: 1})
}

export function roomList() {
  return post(HttpMsgType.ROOM_LIST, {page: 1})
}

export function getRoom(roomId: string) {
  return post(HttpMsgType.ROOM, {roomId: roomId})
}

function send(type: WSMsgType, data: any) {
  let msg = new WsMsg(type, myAddress, data);
  let signMsg = new SignMsg(msg, msg.sign(myKeyPair));
  let json = signMsg.toJSON();
  console.log("send msg:", json);
  client.send(json)
}

export function sendRoomGameData(roomId: string, data: WitnessData) {
  data.doSign(myKeyPair);
  send(WSMsgType.ROOM_GAME_DATA_MSG, data.toJSONObj());
}

export function createRoom(gameHash: String) {
  return post(HttpMsgType.CREATE_ROOM, {gameHash: gameHash})
}

export function joinRoom(roomId: string) {
  send(WSMsgType.JOIN_ROOM_REQ, {roomId: roomId})
}

export function subscribe(fn: (msg: WsMsg) => void) {
  handlers.push(fn);
}

export function unsubscribe(fn: (msg: WsMsg) => void) {
  handlers = handlers.filter(
    item => item !== fn
  );
}

function fire(msg: WsMsg) {
  console.log("fire message", msg, "handlers:", handlers.length);
  handlers.forEach(function (item) {
    item(msg)
  });
}

export function getMyAddress() {
  return myAddress;
}

export function sign(buffer: Buffer): string {
  return myKeyPair.sign(buffer).toString('base64');
}
