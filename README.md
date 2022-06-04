# Howto extend OpenID with advanced users authorisation

OpenID spec solves only first "A" of UAA (Users Authentication and Authorisation): as per its name is focused on user __IDentity__.
We show here how to handle user __authorisation__ using private claims (roles and proxies in this tutorial).

Here-after, a `Proxy` is a set of `permissions` an authenticated user is granted by a `proxiedUser`.

This repo is a complete tutorial covering:
- Keycloak authorization server
  * use built-in mappers to add user roles to issued tokens
  * create a custom mapper to add a "proxies" private claim to tokens, containing collections of granted permissions per proxied-user
- spring-boot RESTful API
  * security evaluating roles and proxies from the access-token
  * OpenAPI spec (ease consumption by clients written in almost any language)
- multi-platform client (web, android & iOS) written with Angular (and Capacitor / Ionic)
  * `UserService` exposing roles and proxies contained in ID-token
  * two route guards evaluating respectively roles and proxies

## TLS / SSL
It is highly recommanded you [generate a self-signed certificate](https://github.com/ch4mpy/self-signed-certificate-generation) for your dev machine when working with OAuth (OIDC is OpenID which is OAuth2)

## Keycloak
You'll need admin access to a Keycloak __server__ instance. You might [download](https://www.keycloak.org/downloads) one to run on your localhost either as standalone, docker container or K8s deployment, depending on what is easiest for you.

You might refer to [Keycloak doc to enable TLS](https://www.keycloak.org/server/enabletls).

We'll assume you already
- created a "user-proxies" client in "master" realm with following settings:
  * `Standard Flow Enabled`: on
  * `Valid Redirect URIs`: https://localhost:4200/, https://localhost:8100/, https://{hostanme}:4200/, https://{hostanme}:8100/ where {hostname} should be replaced with the name of your machine on the network (who you generated self-signed certificate for)
  * `Web Origins`: *
- enabled "client roles" mapper under `Clients` > `user-proxies` > `Mappers` > `Add Builtin` and then edit configuration to set:
  * `Client ID`: user-proxies
  * `Add to ID token`: on
  * `Add to access token`: on
- created a few roles under `Clients` > `user-proxies` > `Roles` (among which one called `NICE_GUY`)
- defined a few users with various roles assignements

## Aditional tooling
- JDK 17 or above
- maven
- node & npm. `nvm`(`-windows`) is probably your best option
- optionnaly, docker to build API native-image

## Sub-projects
Please follow futher instructions in `api` (which contains a maven multi-module project for RESTful API) and then `angular-workspace` folders
