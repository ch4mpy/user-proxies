import { LogLevel, PassedInitialConfig } from 'angular-auth-oidc-client'

const basePath = 'https://localhost:8443'

const secureRoutes = [
  `${basePath}/users`,
  `${basePath}/referential`,
]

export const authConfig: PassedInitialConfig = {
  config: {
    authority: 'https://mc-ch4mp.local:9443/realms/master',
    secureRoutes,
    redirectUrl: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
    clientId: 'user-proxies-client',
    scope: 'openid profile offline_access email roles',
    responseType: 'code',
    silentRenew: true,
    useRefreshToken: true,
    logLevel: LogLevel.Debug,

  }
}

export const environment = {
  enableRoutesTracing: false,
  production: false,
  authConfig,
  errorHttpInterceptorIgnore: [/^[\w\d\:\/\-]*\/machin(\?.*)?$/],
  basePath,
}
