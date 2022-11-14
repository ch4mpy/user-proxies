# Howto extend OpenID with advanced users authorisation

OpenID spec solves only first "A" of UAA (Users Authentication and Authorisation): as per its name, it is focused on user __IDentity__.
We show here how to satisfy to the other "A", user __authorisations__, using private claims:
- provide with user roles (RBAC, Role Based Access Control)
- also demo how easy it can be to implement a quite secific use case: delegation of permissions between users

Here-after, a `Proxy` is a set of `grants` an authenticated user is given by a `proxiedUser`.

This repo is a complete tutorial covering:
- spring-boot RESTful API
  * security evaluating roles and proxies from the access-token
  * OpenAPI spec (ease consumption by clients written in almost any language)
- multi-platform client (web, android & iOS) written with Angular (and Capacitor / Ionic)
  * `UserService` exposing roles and proxies contained in ID-token
  * two route guards evaluating respectively roles and proxies
- Keycloak authorization server
  * use built-in mappers to add user roles to issued tokens
  * create a custom mapper to add a "proxies" private claim to tokens, containing collections of grants per proxied-user

## TLS / SSL
When you work with OAuth2, it is highly recommanded you serve all your services over HTTPS (Keycloak, APIs and client web-server) even when working on your dev machine. I provide with a [script to generate self-signed certificates in seconds](https://github.com/ch4mpy/self-signed-certificate-generation).

## Keycloak
You'll need admin access to a Keycloak server instance. You might [download](https://www.keycloak.org/downloads) one to run on your localhost either as standalone, docker container or K8s deployment, depending on what is easiest for you.

You should refer to [Keycloak doc to enable TLS](https://www.keycloak.org/server/enabletls).

We'll assume you already have
- Keycloak instance running on https://localhost:8443, with a master realm
- "user-proxies-public" client in "master" realm with following settings:
  * `Access Type`: public
  * `Standard Flow Enabled`: on
  * `Valid Redirect URIs`: https://localhost:4200/*, https://localhost:8100/*, https://{hostanme}:4200/*, https://{hostanme}:8100/* where {hostname} should be replaced with the name of your machine on the network (who you generated self-signed certificate for)
  * `Web Origins`: same as redirect URIs sockets (or just * if lazy)
- "user-proxies-mapper" client in "master" realm with following settings:
  * `Access Type`: confidential
  * `Service Accounts Enabled` and `Authorization Enabled`: on, other flows off
- enabled "client roles" mapper for both clients: under `Clients` > [`user-proxies-public` | `user-proxies-mapper`] > `Mappers` > `Add Builtin` and then edit configuration to set:
  * `Client ID`: user-proxies
  * `Add to ID token`: on
  * `Add to access token`: on
- `NICE_GUY` role under `Clients` > `user-proxies-public` > `Roles` (plus whichever roles you like)
- `TOKEN_ISSUER` role under `Clients` > `user-proxies-mapper` > `Roles`
- defined a few users with various roles assignements for `user-proxies-public`
- granted `TOKEN_ISSUER` under `Clients` > `user-proxies-mapper` > `Service Account Roles` > `Client Roles` > `user-proxies-mapper`
- a password and an email defined for each user

As you might guess:
- `user-proxies-public` will be used by Angular web & mobile clients to authenticate users (and add UAA to requests sent to resource-servers)
- `user-proxies-mapper` will be used to allow keycloak mapper to fetch proxies from spring resource-server

## Required tooling
- JDK 17 or above
- maven
- node 14 & npm. `nvm`(`-windows`) is probably your best option
- optionnaly, docker to build API native-image

## Sub-projects
Please follow futher instructions in `api` (which contains a maven multi-module project for RESTful API) and then `angular-workspace` folders
