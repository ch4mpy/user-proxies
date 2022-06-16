import { Injectable } from '@angular/core';
import { UsersApi } from '@c4-soft/user-proxies-api';
import {
  OidcSecurityService,
  OpenIdConfiguration,
} from 'angular-auth-oidc-client';
import { BehaviorSubject, lastValueFrom, Observable } from 'rxjs';
import { environment } from '../environments/environment';

export class User {
  static readonly ANONYMOUS = new User(
    '',
    '',
    '',
    '',
    [],
    new Map<string, Array<string>>()
  );

  constructor(
    readonly subject: string,
    readonly username: string,
    readonly displayName: string,
    readonly email: string,
    readonly roles: Array<string>,
    readonly proxies: Map<string, Array<string>>
  ) {}

  get isAuthenticated(): boolean {
    return !!this.username;
  }

  getProxy(proxiedUsername: string): Array<string> {
    return this.proxies.get(proxiedUsername) || [];
  }

  static of(userData?: any): User {
    const realmRoles: string[] = userData?.realm_access?.roles || [];
    if (
      environment.authConfig.config?.constructor?.name ===
      'OpenIdConfiguration[]'
    ) {
      throw 'Update User class to pick clientId from the right configuration';
    }
    var clientId = (environment.authConfig.config as OpenIdConfiguration)
      ?.clientId;
    const clientRoles: string[] =
      userData?.resource_access && clientId
        ? userData.resource_access[clientId]?.roles || []
        : [];
    return userData?.preferred_username
      ? new User(
          userData.sub,
          userData.preferred_username,
          userData.name || userData.preferred_username,
          userData.email,
          realmRoles
            .concat(clientRoles)
            .map((r) => r?.trim()?.toUpperCase())
            .filter((r) => !!r?.length),
          userData?.proxies || new Map<string, Array<string>>()
        )
      : User.ANONYMOUS;
  }
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly _currentUser$ = new BehaviorSubject<User>(User.ANONYMOUS);

  constructor(
    private oidcService: OidcSecurityService,
    private usersApi: UsersApi
  ) {
    this.refreshUserData(undefined);
    this.oidcService
      .checkAuth()
      .subscribe(({ isAuthenticated, userData, accessToken, idToken }) => {
        this.refreshUserData(userData);
      });
  }

  private async refreshUserData(idClaims: any) {
    console.log('refreshUserData: ', idClaims);
    this._currentUser$.next(User.of(idClaims));
    lastValueFrom(
      this.usersApi.retrieveByPreferredUsername(idClaims.preferred_username)
    ).catch(() =>
      lastValueFrom(
        this.usersApi.create({
          email: idClaims.email,
          preferredUsername: idClaims.preferred_username,
          subject: idClaims.sub,
        })
      )
    );
  }

  ngOnDestroy() {}

  login() {
    this.oidcService.authorize();
  }

  logout() {
    this.oidcService.logoff();
    this.refreshUserData(undefined);
  }

  get valueChanges(): Observable<User> {
    return this._currentUser$;
  }

  get current(): User {
    return this._currentUser$.getValue();
  }
}
