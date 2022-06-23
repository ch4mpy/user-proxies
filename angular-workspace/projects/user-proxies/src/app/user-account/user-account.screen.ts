import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { ProxyDto, UserDto, UsersApi } from '@c4-soft/user-proxies-api';
import { lastValueFrom } from 'rxjs';
import { LoadingService } from '../loading.service';
import { UserService } from '../user.service';
import {
  ProxyCreationDialog,
  ProxyCreationDialogData,
} from './proxy-creation.dialog';

@Component({
  selector: 'app-user-account',
  template: `<ion-header>
      <ion-toolbar translucent color="primary">
        <ion-buttons slot="start">
          <ion-menu-button></ion-menu-button>
        </ion-buttons>
        <ion-title>{{ user.current.displayName || 'Compte' }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <div fxLayout="column" style="max-width: 1080px; margin: 2em;">
        <mat-progress-bar
          mode="indeterminate"
          *ngIf="load.isLoading$ | async"
        ></mat-progress-bar>
        <div fxLayout="column" fxLayout.xl="row">
          <div fxLayout="row" fxLayout.lt-sm="column">
            <mat-form-field>
              <mat-label>Username</mat-label>
              <input
                matInput
                [value]="userDetails?.preferredUsername"
                disabled
              />
            </mat-form-field>
            <mat-form-field style="min-width: 20em;">
              <mat-label>e-mail</mat-label>
              <input matInput [value]="userDetails?.email" disabled />
            </mat-form-field>
          </div>
          <div fxLayout="row" fxLayout.lt-sm="column">
            <mat-form-field>
              <mat-label>ID</mat-label>
              <input matInput [value]="userDetails?.id" disabled />
            </mat-form-field>
            <mat-form-field style="min-width: 20em;">
              <mat-label>Subject</mat-label>
              <input matInput [value]="userDetails?.subject" disabled />
            </mat-form-field>
          </div>
        </div>
        <div>
          <h2>Granted proxies</h2>
          <app-proxy *ngFor="let p of grantedPorxies" [proxy]="p" (deleted)="refresh()" (edited)="refresh()"></app-proxy>
        </div>
        <div>
          <h2>Granting proxies</h2>
          <app-proxy *ngFor="let p of grantingPorxies" [proxy]="p" (deleted)="refresh()" (edited)="refresh()"></app-proxy>
          <div fxLayout="row" style="margin-top: 1em;">
            <div fxFlex></div>
            <button mat-mini-fab (click)="openProxyCreationDlg()">
              <mat-icon>add</mat-icon>
            </button>
          </div>
        </div>
        <div style="margin-top: 4em;">
          <button mat-raised-button (click)="logout()" style="width: 100%;">
            Logout
          </button>
        </div>
      </div>
    </ion-content>`,
  styles: [],
})
export class UserAccountScreen implements OnInit {
  grantedPorxies: Array<ProxyDto> = [];
  grantingPorxies: Array<ProxyDto> = [];
  userDetails?: UserDto;

  constructor(
    readonly user: UserService,
    private router: Router,
    private usersApi: UsersApi,
    private loading: LoadingService,
    private matDlg: MatDialog,
    readonly load: LoadingService
  ) {}

  async ngOnInit() {
    this.refresh();
  }

  refresh() {
    this.loading.wrap(
      Promise.all([
        lastValueFrom(
          this.usersApi.retrieveGrantedProxies(this.user.current.username)
        ).then((dto) => (this.grantedPorxies = dto)),
        lastValueFrom(
          this.usersApi.retrieveGrantingProxies(this.user.current.username)
        ).then((dto) => (this.grantingPorxies = dto)),
        lastValueFrom(
          this.usersApi.retrieveByPreferredUsername(this.user.current.username)
        ).then((dto) => (this.userDetails = dto)),
      ]).catch(() => {})
    );
  }

  logout() {
    this.user.logout();
    this.router.navigate(['/']);
  }

  openProxyCreationDlg() {
    const dlg = this.matDlg.open(ProxyCreationDialog, {
      data: new ProxyCreationDialogData(this.user.current.username),
    });
    dlg.afterClosed().subscribe(() => this.refresh());
  }
}
