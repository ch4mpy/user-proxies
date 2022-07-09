import { LogLevel, PassedInitialConfig } from 'angular-auth-oidc-client'

const usersBasePath = 'https://localhost:9443'
const greetBasePath = 'https://localhost:9445'

const secureRoutes = [
  `${usersBasePath}/users`,
  `${greetBasePath}/greet`,
]

export const authConfig: PassedInitialConfig = {
  config: {
    authority: 'https://localhost:8443/realms/master',
    secureRoutes,
    redirectUrl: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
    clientId: 'user-proxies-client',
    scope: 'openid profile offline_access email audience',
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
