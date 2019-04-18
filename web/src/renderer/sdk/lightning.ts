// Setup lnd rpc

let config: Config

export class Config {
  lndUrl: string;
  lndMacaroon: string;


  constructor(lndUrl: string, lndMacaroon: string) {
    this.lndUrl = lndUrl;
    this.lndMacaroon = lndMacaroon;
  }
}

export function init(_config: Config) {
  config = _config
  console.log("init", _config)
}

function get(api: string) {
  console.log("config:", config);
  let url = config.lndUrl + "/v1" + api;
  return fetch(url, {
    method: "GET",
    mode: "cors",
    credentials: "omit",
    headers: {
      "Content-Type": "application/json;charset=UTF-8",
      "Grpc-Metadata-macaroon": config.lndMacaroon
    }
  }).then(response => {
    console.log("request resp:", response);
    return response.json()
  })
}

function post(api: string,body :string) {
  console.log("config:", config);
  let url = config.lndUrl + "/v1" + api;
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


export function invoice() {
  return get("/invoices")
}

export function addInvoice(rHash:string,value:number){
  var requestBody = { 
    r_hash:rHash,
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

export function settleInvoice(preimage:string){
  var requestBody = { 
    preimage:preimage,
  }
  return post("channels/transactions",JSON.stringify(requestBody))
}

export function walletBalance(){
  return get("balance/blockchain") 
}

export function lookupInvoice(rHash:string){
  return get("invoice/"+rHash)
}