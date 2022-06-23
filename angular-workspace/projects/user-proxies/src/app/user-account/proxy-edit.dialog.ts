import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ProxyDto, ProxyEditDto, UsersApi } from '@c4-soft/user-proxies-api';
import { lastValueFrom } from 'rxjs';
import { LoadingService } from '../loading.service';
import { UserService } from '../user.service';

@Component({
  selector: 'app-proxy-creation',
  template: `
    <div fxLayout="row" fxLayout.lt-md="column">
      <div fxLayout="row" fxLayout.lt-sm="column">
        <mat-form-field appearance="fill">
          <mat-label>Granted user</mat-label>
          <input matInput [value]="data.grantedUsername" disabled />
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
        <mat-hint>YYYY-MM-DD</mat-hint>
        <mat-datepicker-toggle
          matSuffix
          [for]="validityPicker"
        ></mat-datepicker-toggle>
        <mat-date-range-picker #validityPicker></mat-date-range-picker>
      </div>
      <div fxLayout="row" style="margin-left: 1em;">
        <button mat-mini-fab style="margin-left: auto;" (click)="update()">
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
export class ProxyEditDialog {
  readonly PROFILE_READ_GRANT = ProxyDto.GrantsEnum.profileRead.toString();

  readonly grantsCtrl!: FormControl<string[] | null>
  readonly startCtrl!: FormControl<Date | null>
  readonly endCtrl!: FormControl<Date | null>
  readonly editGrp!: FormGroup

  grants: ProxyEditDto.GrantsEnum[];

  constructor(
    private user: UserService,
    private usersApi: UsersApi,
    readonly load: LoadingService,
    @Inject(MAT_DIALOG_DATA) readonly data: ProxyDto,
    private dialogRef: MatDialogRef<ProxyEditDialog>
  ) {
    this.grants = Object.values(ProxyDto.GrantsEnum);
    this.grantsCtrl = new FormControl<string[]>(
      data.grants.map(g => g.toString()),
      [Validators.required, Validators.minLength(1)]
    );
    this.startCtrl = new FormControl<Date>(new Date(data.start), [Validators.required]);
    this.endCtrl = new FormControl<Date | null>(data.end ? new Date(data.end) : null, []);
    this.editGrp = new FormGroup({
      grants: this.grantsCtrl,
      start: this.startCtrl,
      end: this.endCtrl,
    })
  }

  update() {
    if (this.editGrp.invalid || !this.startCtrl.value) {
      return;
    }
    this.load.wrap(
      lastValueFrom(
        this.usersApi.updateProxy(
          this.user.current.username,
          this.data.grantedUsername,
          this.data.id,
          {
            grants: this.grantsCtrl.value as ProxyEditDto.GrantsEnum[],
            start: new Date(this.startCtrl.value).getTime(),
            end: this.endCtrl?.value
              ? new Date(this.endCtrl?.value).getTime()
              : undefined,
          }
        )
      )
    ).then(() => this.dialogRef.close());
  }
}
