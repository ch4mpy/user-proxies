import { Component, Input, OnInit, Output } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { UsersApi } from '@c4-soft/user-proxies-api';
import { BehaviorSubject } from 'rxjs/internal/BehaviorSubject';
import { Observable } from 'rxjs/internal/Observable';
import { of } from 'rxjs/internal/observable/of';
import { mergeMap } from 'rxjs/operators';

@Component({
  selector: 'app-username-autocomplete',
  template: `<mat-form-field appearance="fill">
    <mat-label>{{ inputLabel }}</mat-label>
    <input
      matInput
      [formControl]="usernameCtrl"
      [matAutocomplete]="usernameAutocomplete"
      (keypress.enter)="submit(usernameInput.value)"
      #usernameInput
    />
    <mat-autocomplete
      #usernameAutocomplete="matAutocomplete"
      (optionSelected)="submit($event.option.value)"
    >
      <mat-option
        *ngFor="let username of options | async"
        [value]="username"
      >
        {{ username }}
      </mat-option>
    </mat-autocomplete>
  </mat-form-field>`,
  styles: [],
})
export class UsernameAutocompleteComponent implements OnInit {
  @Input()
  inputLabel!: string;

  private _optionSelected$ = new BehaviorSubject<string | null>(null);

  private _option$ = new BehaviorSubject<string[]>([]);

  usernameCtrl = new FormControl<string>('', [
    Validators.required,
    Validators.minLength(3),
  ]);

  constructor(private usersApi: UsersApi) {
    this.usernameCtrl.valueChanges
      .pipe(
        mergeMap((value) => {
          if (this.usernameCtrl.valid && typeof value === 'string') {
            return this.usersApi.retrieveUsernamesLike(value as string);
          }
          return of([]);
        })
      )
      .subscribe((usernames) => {
        this._option$.next(usernames);
        if (usernames.length === 1) {
          this.submit(usernames[0]);
        }
        return usernames;
      });
  }

  ngOnInit(): void {}

  @Output()
  get optionSelected(): Observable<string | null> {
    return this._optionSelected$;
  }

  @Output()
  get options(): Observable<string[]> {
    return this._option$;
  }

  submit(username: string | null) {
    if(username !== this._optionSelected$.value) {
      this.usernameCtrl.patchValue(username)
      this._optionSelected$.next(
        !!username && this._option$.value?.includes(username) ? username : null
      )
    }
  }
}
