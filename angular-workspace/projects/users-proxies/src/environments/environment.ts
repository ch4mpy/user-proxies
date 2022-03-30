import { AuthConfig, OAuthModuleConfig } from 'angular-oauth2-oidc';

export const authConfig: AuthConfig = {
  issuer: 'https://bravo-ch4mp:9443/auth/realms/master',
  redirectUri: 'https://bravo-ch4mp:8100',
  postLogoutRedirectUri: window.location.origin,
  clientId: 'starter',
  responseType: 'code',
  scope: 'openid profile email offline_access',
  customQueryParams: {
    //this is where OpenAPI REST resource-server is located
    audience: 'https://bravo-ch4mp:4204',
  },
  showDebugInformation: true,
};

export const oAuthModuleConfig: OAuthModuleConfig = {
  resourceServer: {
    allowedUrls: ['https://bravo-ch4mp:4204/users'],
    sendAccessToken: true,
  },
};

export const environment = {
  production: false,
  authConfig,
  oAuthModuleConfig,
};
