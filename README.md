# Howto extend OpenID with advanced users authorisation

OpenID spec solves only first "A" of UAA (Users Authentication and Authorisation): as per its name, it is focused on user __IDentity__.
We show here how to handle user __authorisations__ using private claims (roles and proxies in this tutorial).

Here-after, a `Proxy` is a set of `grants` an authenticated user is given by a `proxiedUser`.

This repo is a complete tutorial covering:
- Keycloak authorization server
  * use built-in mappers to add user roles to issued tokens
  * create a custom mapper to add a "proxies" private claim to tokens, containing collections of grants per proxied-user
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
- created a "user-proxies-client" client in "master" realm with following settings:
  * `Access Type`: public
  * `Standard Flow Enabled`: on
  * `Valid Redirect URIs`: https://localhost:4200/, https://localhost:8100/, https://{hostanme}:4200/, https://{hostanme}:8100/ where {hostname} should be replaced with the name of your machine on the network (who you generated self-signed certificate for)
  * `Web Origins`: *
- created a "user-proxies-mapper" client in "master" realm with following settings:
  * `Access Type`: confidential
  * `Service Accounts Enabled` and `Authorization Enabled`: on, other flows off
  * `Web Origins`: https://localhost:8443, https://{hostanme}:8443 where {hostname} should be replaced with the name of your machine on the network (who you generated self-signed certificate for)
- enabled "client roles" mapper for both clients: under `Clients` > `user-proxies-client` > `Mappers` > `Add Builtin` and then edit configuration to set (same thing with `user-proxies-mapper`):
  * `Client ID`: user-proxies
  * `Add to ID token`: on
  * `Add to access token`: on
- created a `NICE_GUY` role under `Clients` > `user-proxies-client` > `Roles` (plus whichever roles you like)
- created a `TOKEN_ISSUER` role under `Clients` > `user-proxies-mapper` > `Roles`
- defined a few users with various roles assignements for `user-proxies-client`
- granted `TOKEN_ISSUER` under `Clients` > `user-proxies-mapper` > `Service Account Roles` > `Client Roles` > `user-proxies-mapper`

As you might guess:
- `user-proxies-client` will be used by Angular web & mobile clients to authenticate users (and add UAA to requests sent to resource-servers)
- `user-proxies-mapper` will be used to allow keycloak mapper to fetch proxies from spring resource-server

## Required tooling
- JDK 17 or above
- maven
- node & npm. `nvm`(`-windows`) is probably your best option
- optionnaly, docker to build API native-image

## Sub-projects
Please follow futher instructions in `api` (which contains a maven multi-module project for RESTful API) and then `angular-workspace` folders
