import { Component, OnInit } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { GreetApi, GreetDto } from '@c4-soft/greet-api';
import { UsersApi } from '@c4-soft/user-proxies-api';
import { lastValueFrom } from 'rxjs';
import { startWith } from 'rxjs';
import { of } from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { map, mergeMap, Observable } from 'rxjs';
import { LoadingService } from './loading.service';
import { UserService } from './user.service';

@Component({
  selector: 'app-home',
  template: `<ion-header>
      <ion-toolbar translucent color="primary">
        <ion-buttons slot="start">
          <ion-menu-button></ion-menu-button>
        </ion-buttons>
        <ion-title>Home</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content>
      <mat-label *ngIf="!(isAuthenticated$ | async)"
        >Please authenticate</mat-label
      >
      <div *ngIf="isAuthenticated$ | async" fxLayout="column">
        <h2>Your personnal greeting</h2>
        <div>{{ greeting$ | async }}</div>
        <h2>Greet on behalf of someone</h2>
        <mat-label
          >You need a 'GREET' proxy for that username to proceed</mat-label
        >
        <app-username-autocomplete inputLabel="username" (optionSelected)="greetOther($event || '')"></app-username-autocomplete>
        <div>{{ greetingForOther$ | async }}</div>
      </div>
    </ion-content>`,
  styles: [],
})
export class HomeScreen implements OnInit {
  isAuthenticated$!: Observable<boolean>;

  greeting$!: Observable<string>;
  greetingForOther$ = new BehaviorSubject<string>('');

  constructor(
    private user: UserService,
    private greetApi: GreetApi,
    readonly loading: LoadingService
  ) {}

  ngOnInit(): void {
    this.isAuthenticated$ = this.user.valueChanges.pipe(
      map((u) => u.isAuthenticated)
    );
    this.greeting$ = this.user.valueChanges.pipe(
      mergeMap((u) =>
        u.roles.includes('')
          ? this.greetApi.getGreeting().pipe(map((dto) => dto.message || ''))
          : of('You are not nice enough to be greeted')
      )
    )
  }

  greetOther(username: string) {
    this.greetingForOther$.next('');
    if(!username) {
      return
    }
    console.log('get greeting')
    this.loading
      .wrap(lastValueFrom(this.greetApi.getGreetingOnBehalfOf(username)))
      .then((dto) => {
        this.greetingForOther$.next(dto.message || '')
      }).catch(e => {
        console.log(e)
        if(e?.status === 403) {
          this.greetingForOther$.next(`You are not allowed to get greeting on behalf of ${username}`)
        }
      })
  }
}
