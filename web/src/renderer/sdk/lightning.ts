// Setup lnd rpc
let grpc = require('grpc');
process.env.GRPC_SSL_CIPHER_SUITES = 'HIGH+ECDSA'


let lnrpcDescriptor = grpc.load("rpc.proto");
let lnrpc = lnrpcDescriptor.lnrpc;
let lightning

export class Config {
  lndUrl: string;
  lndPassword: string;
  lndCert: Buffer;


  constructor(lndUrl: string, lndPassword: string, lndCert: Buffer) {
    this.lndUrl = lndUrl;
    this.lndPassword = lndPassword;
    this.lndCert = lndCert;
  }

}

export function init(config: Config) {
  // trying to unlock the wallet:
  let credentials = grpc.credentials.createSsl(config.lndCert);
  if (config.lndPassword) {
    console.log('trying to unlock the wallet');
    let walletUnlocker = new lnrpc.WalletUnlocker(config.lndUrl, credentials);
    walletUnlocker.unlockWallet(
      {
        wallet_password: config.lndPassword,
      },
      function (err: any, response: any) {
        if (err) {
          console.log('unlockWallet failed, probably because its been aleady unlocked');
        } else {
          console.log('unlockWallet:', response);
        }
      },
    );
  }
  lightning = new lnrpc.Lightning(config.lndUrl, credentials);
}
