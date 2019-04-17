let W3CWebSocket = require('websocket').w3cwebsocket;


let client: typeof W3CWebSocket;
let wsServer: string;
let httpServer: string;
let myId: string = randomString();

let handlers: { (msg: WSMessage): void }[] = [];

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
  CONN,
  START_INVITE_REQ,
  START_INVITE_RESP,
  INVITE_PAYMENT_REQ,
  INVITE_PAYMENT_RESP,
  PAYMENT_START_REQ,
  PAYMENT_START_RESP,
  GAME_BEGIN,
  SURRENDER_REQ,
  SURRENDER_RESP,
  CHALLENGE_REQ,
  JOIN_ROOM,
  ROOM_DATA_MSG,
  UNKNOWN
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

  client.onmessage = function (e: any) {
    if (typeof e.data === 'string') {
      console.log("Received: '" + e.data + "'");
      let obj = JSON.parse(e.data);
      let type: string = obj.type;
      let msg = new WSMessage(WSMsgType[type as keyof typeof WSMsgType], JSON.parse(obj.data));
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
    "type": HttpMsgType[type],
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

export function getRoom(roomId: string) {
  return post(HttpMsgType.ROOM, {roomId: roomId})
}

function send(type: WSMsgType, roomId: string, data: any) {
  let msg = {from: myId, to: roomId, type: WSMsgType[type], data: JSON.stringify(data)}
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
