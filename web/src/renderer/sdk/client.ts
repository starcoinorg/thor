import crypto from "./crypto";
import util from "./util";

let W3CWebSocket = require('websocket').w3cwebsocket;


let client: typeof W3CWebSocket;
let wsServer: string;
let httpServer: string;
let serverPubKey: crypto.ECPair;
let me: User;

let handlers: { (msg: WsMsg): void }[] = [];

export class User {
  id: string;
  key: crypto.ECPair;

  constructor(key: crypto.ECPair) {
    this.id = crypto.toAddress(key);
    this.key = key;
  }

  toJSONObj(): any {
    return {id: this.id, key: this.key.toWIF()}
  }

  sign(hash: Buffer): Buffer {
    return this.key.sign(hash);
  }

  getPublicKey(): string {
    return "0x" + this.key.publicKey!.toString('hex');
  }

}

export class Room {
  roomId: string = "";
  gameId: string = "";
  players: any[] = [];
  cost: number = 0;
  time: number = 0;
  begin: number = 0;

  isFree(): boolean {
    return this.cost == 0;
  }
}


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
  GAME_END,
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

  static fromJSON(json: any) {
    return new SignMsg(WsMsg.fromJSON(json.msg), json.sign)
  }

  verify(pubKey: crypto.ECPair): boolean {
    let json = JSON.stringify(this.msg.toJSONObj());
    let msgData = Buffer.from(json);
    let hash = crypto.hash(msgData);
    return util.doVerify(hash, this.sign, pubKey);
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

  sign(key: crypto.ECPair): string {
    return util.doSignWithString(JSON.stringify(this.toJSONObj()), key);
  }

  static fromJSON(json: any) {
    let type: string = json.type;
    return new WsMsg(WSMsgType[type as keyof typeof WSMsgType], json.userId, json.data);
  }
}

export class WitnessData {
  userId: string = "";
  stateHash: Buffer = Buffer.from([]);
  preSign: string = "";
  data: Buffer = Buffer.from([]);
  timestamp: number = 0;
  arbiterSign: string = "";
  sign: string = "";

  initWithJSON(jsonObj: any) {
    this.userId = jsonObj.userId;
    this.stateHash = Buffer.from(jsonObj.stateHash.slice(2), 'hex');
    this.preSign = jsonObj.preSign;
    this.data = Buffer.from(jsonObj.data.slice(2), 'hex');
    this.sign = jsonObj.sign;
    this.timestamp = jsonObj.timestamp;
    this.arbiterSign = jsonObj.arbiterSign;
  }

  signData(): Buffer {
    return Buffer.concat([Buffer.from(this.userId), this.stateHash, this.data]);
  }

  arbiterSignData(): Buffer {
    return Buffer.concat([Buffer.from(this.userId), this.stateHash, this.data, util.numberToBuffer(this.timestamp)]);
  }

  doSign(key: crypto.ECPair): void {
    this.sign = util.doSign(this.signData(), key);
  }

  verifyPreSign(preUserId: string, preStatHash: Buffer, preData: Buffer, pubKey: crypto.ECPair): boolean {
    let signData = Buffer.concat([Buffer.from(preUserId), preStatHash, preData]);
    return util.doVerifyByData(signData, this.preSign, pubKey);
  }

  verifyPreSignByBeginTime(beginTime: number, pubKey: crypto.ECPair): boolean {
    let signData = util.numberToBuffer(beginTime);
    return util.doVerifyByData(signData, this.preSign, pubKey);
  }

  verifySign(pubKey: crypto.ECPair): boolean {
    return util.doVerifyByData(this.signData(), this.sign, pubKey);
  }

  verifyArbiterSign(pubKey: crypto.ECPair) {
    return util.doVerifyByData(this.arbiterSignData(), this.arbiterSign, pubKey);
  }

  toJSONObj(): any {
    return {
      "userId": this.userId,
      "stateHash": "0x" + this.stateHash.toString('hex'),
      "preSign": this.preSign,
      "data": "0x" + this.data.toString('hex'),
      "timestamp": this.timestamp,
      "arbiterSign": this.arbiterSign,
      "sign": this.sign
    }
  }
}

export class Witnesses {
  roomId: string = "";
  witnesses: WitnessData[] = [];

  initWithJson(jsonObj: any) {
    this.roomId = jsonObj.roomId;
    jsonObj.witnesses.forEach((jsonObj: any) => {
      let witnessData = new WitnessData();
      witnessData.initWithJSON(jsonObj);
      this.witnesses.push(witnessData);
    })
  }

  toJSONObj(): any {
    let jsonObj = {roomId: this.roomId, witnesses: []};
    this.witnesses.forEach((data: WitnessData) => {
      // @ts-ignore
      jsonObj.witnesses.push(data.toJSONObj());
    });
    return jsonObj;
  }
}

function connect() {
  client = new W3CWebSocket(wsServer);

  client.onerror = function () {
    console.debug('Connection Error');
  };

  client.onopen = function () {
    console.debug('WebSocket Client Connected');
  };

  client.onclose = function (e: any) {
    console.debug('Socket is closed. Reconnect will be attempted in 1 second.', e.reason);
    setTimeout(function () {
      connect();
    }, 1000);
  };

  client.onmessage = function (e: any) {
    if (typeof e.data === 'string') {
      console.debug("Received: '" + e.data + "'");
      let obj = JSON.parse(e.data);
      let signMsg = SignMsg.fromJSON(obj);
      let msg = signMsg.msg;
      if (msg.type == WSMsgType.NONCE) {
        let nonce: string = msg.data.nonce;
        let pubKey: string = msg.data.pubKey;
        let buffer = util.decodeHex(pubKey);
        console.debug("pubKey buffer:", buffer);
        serverPubKey = crypto.fromPublicKey(buffer);
        util.check(signMsg.verify(serverPubKey));
        //TODO why TS2722
        send(WSMsgType.NONCE, {nonce: nonce, pubKey: me.getPublicKey()});
      } else {
        util.check(signMsg.verify(serverPubKey));
        fire(msg);
      }
    }
  };

  client.onerror = function (err: any) {
    console.error('Socket encountered error: ', err.message, 'Closing socket');
    client.close();
  };
}

export function init(keyPair: crypto.ECPair, ws = 'ws://localhost:8082/ws', http = 'http://localhost:8082') {
  me = new User(keyPair);
  console.debug("me", JSON.stringify(me.toJSONObj()));
  wsServer = ws;
  httpServer = http;
  connect();
}

function post(type: HttpMsgType, data: any) {
  let typeName = HttpMsgType[type];
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
    console.debug("httpServer:", httpServer, "type:", typeName, "resp:", json);
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
    return util.unmarshal(new Room(), json)
  })
}

function send(type: WSMsgType, data: any) {
  let msg = new WsMsg(type, me.id, data);
  let signMsg = new SignMsg(msg, msg.sign(me.key));
  let json = JSON.stringify(signMsg.toJSONObj());
  console.debug("send msg:", json);
  client.send(json)
}

export function sendRoomGameData(roomId: string, data: WitnessData) {
  data.doSign(me.key);
  return send(WSMsgType.ROOM_GAME_DATA_MSG, {"to": roomId, "witness": data.toJSONObj()});
}

export function doReady(roomId: string) {
  return send(WSMsgType.READY_REQ, {roomId: roomId});
}

export function doSurrender(roomId: string) {
  return send(WSMsgType.SURRENDER_REQ, {roomId: roomId});
}

export function createRoom(gameHash: string, cost: number) {
  return post(HttpMsgType.CREATE_ROOM, {gameHash: gameHash, cost: cost})
}

export function joinRoom(roomId: string) {
  return send(WSMsgType.JOIN_ROOM_REQ, {roomId: roomId})
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
  console.debug("fire message", msg, "handlers:", handlers.length);
  handlers.forEach(function (item) {
    item(msg)
  });
}

export function getMe() {
  return me;
}

export function getMyAddress() {
  return me.id;
}

export function getMyKeyPair(): crypto.ECPair {
  return me.key;
}

export function sign(buffer: Buffer): string {
  return me.sign(buffer).toString('hex');
}

export function getServerPubKey(): crypto.ECPair {
  return serverPubKey;
}
