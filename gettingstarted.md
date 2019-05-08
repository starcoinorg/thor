# Getting started

## Required
+ Java version >= "1.8.0"
+ Gradle version >= "4.10.x"
+ Docker version >= 18 for testing enviroment of lnd
+ Node version  "v11.10.0"
+ Yarn version "1.13.0" 


## Run lnd node
```
./scripts/setup_lnd.sh
```

## Setup arbitrate service

### Compile project 
You could compile source code by such command:
```
./gradlew fatJar
```

### Run arbitrate service
You could run arbitrate service by such command:
```
java -jar thor-arbitrate-service/build/libs/thor-arbitrate-service-all.jar
```

### Run VM service
### Compile VM service
You could compile game client by such command(in thor-app directory):
```
yarn install
```

### Run VM service
You could compile game client by such command(in thor-app directory):
```
./app.js
```

## Setup game client
### Compile and package game client
You could compile game client by such command(in thor-app directory):
```
yarn install
yarn dist:dir
```

then go to dist/mac directory you could find packaged app.

### Config LND
Get lnd account macaroon hex string by :
```
./scripts/macaroon.sh 
```
Copy account macaroon to thor.app. Lnd url of alice is https://localhost:8080 and lnd url of bob is https://localhost:8081 . Make sure lnd url and lnd macaroon should be matched.
