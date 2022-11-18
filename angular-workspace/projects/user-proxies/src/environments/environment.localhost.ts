import { LogLevel, PassedInitialConfig } from 'angular-auth-oidc-client'

const usersBasePath = 'https://localhost:9443' // https://localhost:8080 can be used instead if gateway is up
const greetBasePath = 'https://localhost:9445' // https://localhost:8080 can be used instead if gateway is up

const secureRoutes = [
  `${usersBasePath}/users`,
  `${greetBasePath}/greet`,
]

export const authConfig: PassedInitialConfig = {
  config: {
    authority: 'https://localhost:8443/realms/master', // https://localhost:8080/realms/master can be used instead if gateway is up
    secureRoutes,
    redirectUrl: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
    clientId: 'user-proxies-public',
    scope: 'openid profile offline_access email',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
    logLevel: LogLevel.Debug
  }
}

export const environment = {
  enableRoutesTracing: false,
  production: false,
  authConfig,
  errorHttpInterceptorIgnore: [/^[\w\d\:\/\-]*\/machin(\?.*)?$/],
  usersBasePath,
  greetBasePath,
}
