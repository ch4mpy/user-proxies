import { Component, OnInit } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { ProxyDto, UserDto, UsersApi } from '@c4-soft/proxies-api';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserService } from '../user.service';

@Component({
  selector: 'app-user-account',
  template: `<ion-header>
      <ion-toolbar translucent color="primary">
        <ion-buttons slot="start">
          <ion-menu-button></ion-menu-button>
        </ion-buttons>
        <ion-title>{{ userService.preferredUsername || 'Compte' }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <div *ngIf="!userService.preferredUsername">
        <ion-button (click)="login()">Login</ion-button>
      </div>
      <div *ngIf="!!userService.preferredUsername">
        <ion-button (click)="logout()">Logout</ion-button>
      </div>
      <h2>Subject</h2>
      {{ userService.sub }}
      <h2>Procurations données</h2>
      <mat-list>
        <mat-list-item *ngFor="let p of grantingProxies">
          {{ p.grantedUserSubject }}: {{ p.grants | json }}
        </mat-list-item>
      </mat-list>
      <mat-form-field>
        <mat-label>Granted user subject</mat-label>
        <input matInput placeholder="mandaté" [formControl]="grantedUserCtrl" />
      </mat-form-field>
      <button mat-fab (click)="createProxy()">
        <mat-icon>add</mat-icon>
      </button>
      <h2>Procurations reçues</h2>
      <mat-list>
        <mat-list-item *ngFor="let p of grantedProxies">
          {{ p.grantingUserSubject }}: {{ p.grants | json }}
        </mat-list-item>
      </mat-list>
    </ion-content>`,
  styles: [],
})
export class UserAccountScreen implements OnInit {
  grantingProxies: Array<ProxyDto> = [];
  grantedProxies: Array<ProxyDto> = [];

  constructor(readonly userService: UserService, private userApi: UsersApi) {}

  grantedUserCtrl = new FormControl('', [Validators.required]);

  async ngOnInit() {
    const user = await firstValueFrom(
      this.userApi.retrieveBySubject(this.userService.sub)
    ).catch(() => undefined);
    if (!user) {
      await firstValueFrom(
        this.userApi.create({
          email: this.userService.email,
          preferedUsername: this.userService.preferredUsername,
          subject: this.userService.sub,
        })
      );
    }
    this.grantingProxies = await firstValueFrom(
      this.userApi.retrieveGrantingProxies(this.userService.sub)
    );
    this.grantedProxies = await firstValueFrom(
      this.userApi.retrieveGrantedProxies(this.userService.sub)
    );
  }

  login() {
    this.userService.login();
  }

  logout() {
    this.userService.logout();
  }

  get redirectUti(): string {
    return environment.authConfig.redirectUri || '';
  }

  async createProxy() {
    await firstValueFrom(
      this.userApi.createProxy(
        this.userService.sub,
        this.grantedUserCtrl.value,
        {
          start: new Date().getTime(),
          grants: ['READ_PROXIES', 'EDIT_PROXIES'],
        }
      )
    );
  }
}
