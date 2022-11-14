import { LogLevel, PassedInitialConfig } from 'angular-auth-oidc-client'

const usersBasePath = 'https://localhost:8443'
const greetBasePath = 'https://localhost:8444'

const secureRoutes = [
  `${usersBasePath}/users`,
  `${greetBasePath}/greet`,
]

export const authConfig: PassedInitialConfig = {
  config: {
    authority: 'https://localhost:9443/auth/realms/master',
    secureRoutes,
    redirectUrl: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
    clientId: 'user-proxies-public',
    scope: 'openid profile email offline_access',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
    logLevel: LogLevel.Debug,
  }
}

export const environment = {
  enableRoutesTracing: false,
  production: true,
  authConfig,
  errorHttpInterceptorIgnore: [/^[\w\d\:\/\-]*\/users(\?.*)?$/],
  usersBasePath,
  greetBasePath,
}
