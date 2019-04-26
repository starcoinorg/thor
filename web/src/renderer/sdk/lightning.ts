// Setup lnd rpc

let config: Config

class Config {
  lndUrl: string;
  lndMacaroon: string;


  constructor(lndUrl: string, lndMacaroon: string) {
    this.lndUrl = lndUrl;
    this.lndMacaroon = lndMacaroon;
  }
}

export function hasInit(): boolean {
  return config != undefined;
}

export function init(cfg: any) {
  if (cfg.lndUrl && cfg.lndMacaroon) {
    config = new Config(cfg.lndUrl, cfg.lndMacaroon);
    console.log("lnd init", config)
  } else {
    console.log("can not find lnd config from cfg:", cfg);
  }
}

function get(api: string) {
  console.log("config:", config);
  let url = config.lndUrl + "/v1/" + api;
  return fetch(url, {
    method: "GET",
    mode: "cors",
    credentials: "omit",
    headers: {
      "Content-Type": "application/json;charset=UTF-8",
      "Grpc-Metadata-macaroon": config.lndMacaroon
    }
  }).then(response => {
    return response.json();
  }).then(json => {
    console.log("resp json:", json);
    return json;
  })
}

function post(api: string,body :string) {
  console.log("config:", config);
  let url = config.lndUrl + "/v1/" + api;
  return fetch(url, {
    method: "POST",
    mode: "cors",
    credentials: "omit",
    headers: {
      "Content-Type": "application/json;charset=UTF-8",
      "Grpc-Metadata-macaroon": config.lndMacaroon
    },
    body:body
  }).then(response => {
    console.log("request resp:", response);
    return response.json()
  })
}

export function invoice() {
  return get("invoices")
}

export function addInvoice(rHash: Buffer, value: number) {
  var requestBody = {
    r_hash: rHash.toString('base64'),
    value:value,
  }
  return post("invoices",JSON.stringify(requestBody))
}

export function sendPayment(requestString:string){
  var requestBody = {
    payment_hash_string:requestString,
  }
  return post("channels/transactions",JSON.stringify(requestBody))
}

export function decodePayReq(requestString:string){
  return get("payreq/"+requestString)
}

export function settleInvoice(preimage: Buffer) {
  var requestBody = {
    preimage: preimage.toString('hex'),
  }
  return post("channels/transactions",JSON.stringify(requestBody))
}

export function chainBalance() {
  return get("balance/blockchain")
}

export function channelBalance() {
  return get("balance/channels")
}

export function lookupInvoice(rHash: Buffer) {
  return get("invoice/"+rHash)
}

export function getinfo() {
  return get("getinfo")
}
