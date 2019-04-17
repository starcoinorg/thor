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

export function invoice() {
  return get("/invoices")
}
