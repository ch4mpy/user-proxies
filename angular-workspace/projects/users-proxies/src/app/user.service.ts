import { Injectable } from '@angular/core';
import { UsersApi } from '@c4-soft/proxies-api';
import { LoadingController } from '@ionic/angular';
import { OAuthService } from 'angular-oauth2-oidc';
import { environment } from '../environments/environment';

export class Proxy {
  constructor(
    readonly grantedUser: string,
    public start: Date,
    public grants: string[] = [],
    public end?: Date
  ) {}
}

@Injectable({
  providedIn: 'root',
})
export class UserService {
  picture!: string;
  sub!: string;
  email!: string;
  preferredUsername!: string;
  proxies!: Map<String, Proxy>;

  private loading: Promise<HTMLIonLoadingElement>;

  constructor(
    private oauthService: OAuthService,
    private usersApi: UsersApi,
    loadingCtrl: LoadingController
  ) {
    this.loading = loadingCtrl.create({ duration: 10000 });
    this.refreshUserData(undefined);
    this.oauthService.configure(environment.authConfig);
    this.refresh();
  }

  get isAuthenticated(): boolean {
    return !!this.sub;
  }

  async refresh() {
    if (!this.oauthService.discoveryDocumentLoaded) {
      this.loading.then((l) => l.present());
      await this.oauthService.loadDiscoveryDocument();
      this.loading.then((l) => l.dismiss());
    }
    if (
      !!this.oauthService.getIdentityClaims() &&
      this.oauthService.hasValidAccessToken()
    ) {
      this.refreshUserData(this.oauthService.getIdentityClaims());
    } else {
      this.loading.then((l) => l.present());
      await this.oauthService
        .tryLogin()
        .then(async (loginResp) => {
          console.log('loginResp: ', loginResp);
          if (!this.oauthService.hasValidAccessToken()) {
            await this.oauthService.silentRefresh();
          }
        })
        .then(() => {
          this.refreshUserData(this.oauthService.getIdentityClaims());
        })
        .finally(() => this.loading.then((l) => l.dismiss()));
    }
  }

  login() {
    this.loading.then((l) => l.present());
    this.oauthService.initLoginFlow();
    this.oauthService
      .tryLogin()
      .then(
        (isSuccess) => {
          console.log('Login isSuccess: ', isSuccess);
          if (isSuccess) {
            this.refreshUserData(this.oauthService.getIdentityClaims());
          } else {
            this.refreshUserData(undefined);
          }
        },
        (error) => console.log('Login error: ', error)
      )
      .finally(() => this.loading.then((l) => l.dismiss()));
  }

  logout() {
    this.oauthService.revokeTokenAndLogout();
    this.refreshUserData(undefined);
  }

  private async refreshUserData(idClaims: any) {
    console.log('refreshUserData: ', idClaims);
    this.sub = idClaims?.sub || '';
    this.picture = idClaims?.picture || '';
    this.email = idClaims?.email;
    this.preferredUsername = idClaims?.preferred_username;
  }

  get idClaims() {
    return this.oauthService.getIdentityClaims();
  }
}
