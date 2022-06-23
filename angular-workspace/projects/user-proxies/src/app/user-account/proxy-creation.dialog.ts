import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ProxyDto, ProxyEditDto, UsersApi } from '@c4-soft/user-proxies-api';
import { lastValueFrom, mergeMap, Observable, of, startWith } from 'rxjs';
import { LoadingService } from '../loading.service';

export class ProxyCreationDialogData {
  constructor(readonly proxiedUsername: string) {}
}

@Component({
  selector: 'app-proxy-creation',
  template: `
    <div fxLayout="row" fxLayout.lt-md="column">
      <div fxLayout="row" fxLayout.lt-sm="column">
        <mat-form-field appearance="fill">
          <mat-label>Granted user</mat-label>
          <input
            matInput
            [formControl]="usernameCtrl"
            [matAutocomplete]="usernameAutocomplete"
          />
          <mat-autocomplete #usernameAutocomplete="matAutocomplete">
            <mat-option
              *ngFor="let username of filteredUsername$ | async"
              [value]="username"
            >
              {{ username }}
            </mat-option>
          </mat-autocomplete>
        </mat-form-field>
        <mat-form-field appearance="fill">
          <mat-label>Grants</mat-label>
          <mat-select [formControl]="grantsCtrl" multiple>
            <mat-option
              *ngFor="let g of grants"
              [value]="g"
              [disabled]="g === PROFILE_READ_GRANT"
              >{{ g }}</mat-option
            >
          </mat-select>
        </mat-form-field>
      </div>
      <div>
        <mat-label>Validity</mat-label>
        <mat-date-range-input [rangePicker]="validityPicker" appearance="fill">
          <input
            matStartDate
            [formControl]="startCtrl"
            #startInput
            required
            placeholder="Start date"
          />
          <input
            matEndDate
            [formControl]="endCtrl"
            #endInput
            placeholder="End date"
          />
        </mat-date-range-input>
        <mat-hint>MM/DD/YYYY</mat-hint>
        <mat-datepicker-toggle
          matSuffix
          [for]="validityPicker"
        ></mat-datepicker-toggle>
        <mat-date-range-picker #validityPicker></mat-date-range-picker>
      </div>
      <div fxLayout="row" style="margin-left: 1em;">
        <button mat-mini-fab style="margin-left: auto;" (click)="create()">
          <mat-icon>save</mat-icon>
        </button>
      </div>
      <mat-progress-bar
        mode="indeterminate"
        *ngIf="load.isLoading$ | async"
      ></mat-progress-bar>
    </div>
  `,
  styles: [],
})
export class ProxyCreationDialog implements OnInit {
  readonly PROFILE_READ_GRANT = ProxyDto.GrantsEnum.profileRead.toString();

  readonly usernameCtrl = new FormControl<string>('', [
    Validators.required,
    Validators.minLength(3),
  ]);
  readonly grantsCtrl = new FormControl<string[]>(
    [this.PROFILE_READ_GRANT],
    [Validators.required, Validators.minLength(1)]
  );
  readonly startCtrl = new FormControl<Date>(new Date(), [Validators.required]);
  readonly endCtrl = new FormControl<Date | null>(null, []);

  filteredUsername$!: Observable<string[]>;

  grants: ProxyEditDto.GrantsEnum[];

  constructor(
    private usersApi: UsersApi,
    readonly load: LoadingService,
    @Inject(MAT_DIALOG_DATA) readonly data: ProxyCreationDialogData,
    private dialogRef: MatDialogRef<ProxyCreationDialog>
  ) {
    this.grants = Object.values(ProxyDto.GrantsEnum);
  }

  ngOnInit(): void {
    this.filteredUsername$ = this.usernameCtrl.valueChanges.pipe(
      startWith([]),
      mergeMap((value) => {
        if (this.usernameCtrl.valid && typeof value === 'string') {
          return this.usersApi.retrieveUsernamesLike(value as string);
        }
        return of([]);
      })
    );
  }

  create() {
    if (this.usernameCtrl.invalid || this.grantsCtrl.invalid || this.startCtrl.invalid || !this.startCtrl.value) {
      return;
    }
    this.load
      .wrap(
        lastValueFrom(
          this.usersApi.createProxy(
            this.data.proxiedUsername,
            this.usernameCtrl.value || '',
            {
              grants: this.grantsCtrl.value as ProxyEditDto.GrantsEnum[],
              start: new Date(this.startCtrl.value).getTime(),
              end: this.endCtrl?.value ? new Date(this.endCtrl?.value).getTime() : undefined
            }
          )
        )
      )
      .then(() => this.dialogRef.close());
  }
}
