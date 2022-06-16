import { LogLevel, PassedInitialConfig } from 'angular-auth-oidc-client'

const basePath = 'https://bravo-ch4mp:8443'

const secureRoutes = [
  `${basePath}/users`,
  `${basePath}/referential`,
]

export const authConfig: PassedInitialConfig = {
  config: {
    authority: 'https://bravo-ch4mp:9443/auth/realms/master',
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