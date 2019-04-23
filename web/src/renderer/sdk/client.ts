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
  DEF, PUB_KEY, CREATE_GAME, GAME_LIST, GAME_INFO, CREATE_ROOM, ROOM_LIST, ALL_ROOM_LIST, ROOM, ERR
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

  toJSONObj(): any {
    return {"msg": this.msg.toJSONObj(), "sign": this.sign};
  }

  // toJSON(): string {
  //   return JSON.stringify(this.toJSONObj());
  // }

  static fromJSON(json: any) {
    return new SignMsg(WsMsg.fromJSON(json.msg), json.sign)
  }

  verify(pubKey: crypto.ECPair): boolean {
    let json = JSON.stringify(this.msg.toJSONObj());
    let signature = Buffer.from(this.sign, 'base64');
    //server signature length is 65, but bitcoinjs is 64.
    if (signature.length == 65) {
      signature = signature.slice(1);
    }
    let msgData = Buffer.from(json);
    let hash = crypto.hash(msgData);
    //bitcoinjs bug, result should be boolean.
    let result = pubKey.verify(hash, signature);
    console.log("verify server signature result:", result);
    return <any>result;
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

  toJSONObj(): any {
    return {"type": WSMsgType[this.type], "userId": this.userId, "data": this.data}
  }

  // toJSON(): string {
  //   return JSON.stringify(this.toJSONObj());
  // }

  sign(key: crypto.ECPair): string {
    let buffer = Buffer.from(JSON.stringify(this.toJSONObj()));
    console.log("buffer:", buffer.toString('hex'));
    let hash = crypto.hash(buffer);
    console.log("msg hash:", hash.toString('hex'));
    return key.sign(hash).toString('base64')
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
    return {"preSign": this.preSign, "data": this.data.toString('hex'), "sign": this.sign}
  }
}

function check(condition: boolean, msg?: string): void {
  if (!condition) {
    if (msg) {
      throw msg;
    } else {
      console.error("check error");
      throw "check error.";
    }
  }
}

export function init(_keyPair: crypto.ECPair, ws = 'ws://localhost:8082/ws', http = 'http://localhost:8082') {
  myKeyPair = _keyPair;
  myAddress = crypto.toAddress(myKeyPair);
  console.log("my address", myAddress);
  console.log("my pubKey", myKeyPair.publicKey);
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
        let buffer = Buffer.from(pubKey.slice(2), 'hex');
        console.log("pubKey buffer:", buffer);
        serverPubKey = crypto.fromPublicKey(buffer);
        check(signMsg.verify(serverPubKey));
        //TODO why TS2722
        send(WSMsgType.NONCE, {nonce: nonce, pubKey: "0x" + myKeyPair.publicKey!.toString('hex')});
      } else {
        check(signMsg.verify(serverPubKey));
        fire(msg);
      }
    }
  };
}

function post(type: HttpMsgType, data: any) {
  let typeName = HttpMsgType[type]
  let body = {
    "type": typeName,
    "data": data
  };

  return fetch(httpServer + "/p", {
    method: "POST",
    body: JSON.stringify(body),
    mode: "cors",
    headers: {
      "Content-Type": "application/json;charset=UTF-8"
    }
  }).then(response => {
    return response.json();
  }).then(json => {
    console.log("httpServer:", httpServer, "type:", typeName, "resp:", json);
    return json;
  })
}

export function gameList() {
  return post(HttpMsgType.GAME_LIST, {page: 0})
}

export function gameInfo(gameId: string) {
  return post(HttpMsgType.GAME_INFO, {gameId: gameId})
}

export function roomList() {
  return post(HttpMsgType.ALL_ROOM_LIST, {page: 0})
}

export function getRoom(roomId: string): Promise<Room> {
  return post(HttpMsgType.ROOM, {roomId: roomId}).then(json => {
    return Unmarshaler.unmarshal(new Room(), json)
  })
}

function send(type: WSMsgType, data: any) {
  let msg = new WsMsg(type, myAddress, data);
  let signMsg = new SignMsg(msg, msg.sign(myKeyPair));
  let json = JSON.stringify(signMsg.toJSONObj());
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
  return myKeyPair.sign(buffer).toString('hex');
}

export class Room {
  roomId: string = "";
  gameId: string = "";
  players: string[] = [];
  payment: boolean = false;
  cost: number = 0;
  time: number = 0;
  begin: number = 0;
}

class Unmarshaler {
  static unmarshal<T>(obj: T, jsonObj: any): T {
    try {
      // @ts-ignore
      if (typeof obj["initWithJSON"] === "function") {
        // @ts-ignore
        obj["initWithJSON"](jsonObj);
      } else {
        for (var propName in jsonObj) {
          // @ts-ignore
          obj[propName] = jsonObj[propName]
        }
      }
    } catch (e) {
      console.error("unmarshal error.", e);
    }
    return obj;
  }
}
