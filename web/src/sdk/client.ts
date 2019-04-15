let W3CWebSocket = require('websocket').w3cwebsocket;


let client;
let wsServer: string;
let httpServer: string;
let myId: string = randomString();

let handlers: { (msg: WSMessage): void }[] = [];

enum HttpMsgType {
  DEF = 0,
  CREATE_GAME = 1,
  GAME_LIST = 2,
  CREATE_ROOM = 3,
  ROOM_LIST = 4,
  ROOM = 5,
  ERR = 100
}

export enum WSMsgType {
  CONN = 1,
  START_INVITE_REQ = 4,
  START_INVITE_RESP = 5,
  INVITE_PAYMENT_REQ = 6,
  INVITE_PAYMENT_RESP = 7,
  PAYMENT_START_REQ = 8,
  PAYMENT_START_RESP = 9,
  GAME_BEGIN = 10,
  SURRENDER_REQ = 11,
  SURRENDER_RESP = 12,
  CHALLENGE_REQ = 13,
  JOIN_ROOM = 14,
  ROOM_DATA_MSG = 99,
  UNKNOWN = 100
}

export class WSMessage {
  type: WSMsgType;
  data: any;

  constructor(type: WSMsgType, data: any) {
    this.type = type;
    this.data = data;
  }
}

function randomString() {
  return Math.random().toString(36);
}


export function init(ws = 'ws://localhost:8082/ws', http = 'http://localhost:8082') {

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
        let msg = {"type": 1, "from": "abc", "to": "server", "data": ""}
        //TODO connection message
        //client.send(JSON.stringify(msg));
      }
    }

    conn();
  };

  client.onclose = function () {
    console.log('WebSocket Client Closed');
  };

  client.onmessage = function (e) {
    if (typeof e.data === 'string') {
      console.log("Received: '" + e.data + "'");
      let obj = JSON.parse(e.data)
      let msg = new WSMessage(obj.type, JSON.parse(obj.data));
      if (msg.type == WSMsgType.CONN) {
        myId = msg.data.id;
        console.log("id", myId);
      } else {
        fire(msg);
      }
    }
  };
}

function post(type: HttpMsgType, data: any) {
  let body = {
    "type": type,
    "data": JSON.stringify(data)
  }
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

export function getRoom(roomId) {
  return post(HttpMsgType.ROOM, {roomId: roomId})
}

function send(type: WSMsgType, roomId: string, data: any) {
  let msg = {from: myId, to: roomId, type: type, data: JSON.stringify(data)}
  client.send(JSON.stringify(msg))
}

export function sendRoomData(roomId: string, data: any) {
  send(WSMsgType.ROOM_DATA_MSG, roomId, data)
}

export function createGame() {
  return post(HttpMsgType.CREATE_GAME, {gameHash: myId})
}

export function createRoom(gameHash: String) {
  return post(HttpMsgType.CREATE_ROOM, {gameHash: gameHash})
}

export function joinRoom(roomId: string) {
  send(WSMsgType.JOIN_ROOM, roomId, {roomId: roomId})
}

export function subscribe(fn: (msg: WSMessage) => void) {
  handlers.push(fn);
}

export function unsubscribe(fn: (msg: WSMessage) => void) {
  handlers = handlers.filter(
    function (item) {
      if (item !== fn) {
        return item;
      }
    }
  );
}

function fire(msg: WSMessage) {
  console.log("fire message", msg, "handlers:", handlers.length);
  handlers.forEach(function (item) {
    item(msg)
  });
}

export function getMyId() {
  return myId;
}
