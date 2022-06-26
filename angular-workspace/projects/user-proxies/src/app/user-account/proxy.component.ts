import { COMMA, ENTER } from '@angular/cdk/keycodes';
import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnInit,
  Output,
  ViewChild
} from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipInputEvent } from '@angular/material/chips';
import { ProxyDto, ProxyEditDto, UsersApi } from '@c4-soft/user-proxies-api';
import { lastValueFrom } from 'rxjs';
import { LoadingService } from '../loading.service';
import { UserService } from '../user.service';

@Component({
  selector: 'app-proxy',
  template: `<div
    fxLayout="row"
    fxLayout.lt-sm="column"
    style="margin-top: .5em;"
    class="mat-elevation-z1"
  >
    <div fxLayout="column">
      <div fxLayout="row" fxLayout.lt-md="column">
        <div class="indent">
          {{ proxy.grantingUsername }} => {{ proxy.grantedUsername }}
        </div>
        <mat-chip-list class="indent" #grants>
          <mat-chip *ngFor="let g of proxy.grants">
            {{ g }}
            <button matChipRemove *ngIf="!disabled" (click)="removeGrant(g)">
              <mat-icon>cancel</mat-icon>
            </button>
          </mat-chip>
          <input
            placeholder="New Grant..."
            #grantInput
            [formControl]="grantsCtrl"
            [matAutocomplete]="grantsAutoComplete"
            [matChipInputFor]="grants"
            [matChipInputSeparatorKeyCodes]="separatorKeysCodes"
            (matChipInputTokenEnd)="addGrant($event)"
            [disabled]="disabled"
          />
        </mat-chip-list>
        <mat-autocomplete
          #grantsAutoComplete="matAutocomplete"
          (optionSelected)="selectGrant($event)"
        >
          <mat-option *ngFor="let g of grantValues" [value]="g">
            {{ g }}
          </mat-option>
        </mat-autocomplete>
      </div>
      <div class="indent">
        <mat-date-range-input [rangePicker]="validityPicker" [disabled]="disabled" appearance="fill">
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
    </div>
    <div fxFlex></div>
    <div fxLayout="row">
      <div fxFlex></div>
      <div style="margin: .5em;">
        <button mat-mini-fab (click)="delete()" *ngIf="disabled">
          <mat-icon>delete_forever</mat-icon>
        </button>
      </div>
    </div>
  </div>`,
  styles: ['.indent { margin-left: 1em;}'],
})
export class ProxyComponent implements OnInit {
  @Input()
  proxy!: ProxyDto;

  @Output()
  deleted = new EventEmitter<ProxyDto>();

  @Output()
  edited = new EventEmitter<ProxyDto>();

  @ViewChild('grantInput') grantInput!: ElementRef<HTMLInputElement>;

  separatorKeysCodes: number[] = [ENTER, COMMA];

  readonly grantValues: ProxyEditDto.GrantsEnum[];

  grantsCtrl = new FormControl<string>('', [Validators.required]);
  startCtrl!: FormControl<Date | null>;
  endCtrl!: FormControl<Date | null>;

  constructor(
    private usersApi: UsersApi,
    private loading: LoadingService,
    private user: UserService
  ) {
    this.grantValues = Object.values(ProxyDto.GrantsEnum);
  }

  ngOnInit(): void {
    for (let g of this.proxy.grants) {
      this.grantValues.splice(this.grantValues.indexOf(g), 1);
    }
    this.startCtrl = new FormControl<Date>(new Date(this.proxy.start), [
      Validators.required,
    ]);
    this.endCtrl = new FormControl<Date | null>(
      this.proxy.end ? new Date(this.proxy.end) : null,
      []
    );
  }

  get disabled(): boolean {
    return (
      this.proxy.grantingUsername !== this.user.current.username && !this.user.current.getProxy(this.proxy.grantingUsername)?.includes(ProxyDto.GrantsEnum.proxiesEdit)
    );
  }

  delete() {
    this.loading.wrap(
      lastValueFrom(
        this.usersApi.deleteProxy(
          this.proxy.grantingUsername,
          this.proxy.grantedUsername,
          this.proxy.id
        )
      ).then(() => this.deleted.emit(this.proxy))
    );
  }

  removeGrant(g: ProxyDto.GrantsEnum) {
    this.proxy.grants.splice(this.proxy.grants.indexOf(g), 1);
    this.grantValues.push(g);
    this.sendUpdate();
  }

  addGrant(event: MatChipInputEvent) {
    this.add(event.value);
    event.chipInput!.clear();
  }

  selectGrant(event: MatAutocompleteSelectedEvent) {
    this.add(event.option.viewValue);
  }

  private add(value: any) {
    const grant = value as ProxyEditDto.GrantsEnum;
    if (!Object.values(ProxyEditDto.GrantsEnum).includes(grant)) {
      return;
    }
    this.proxy.grants.push(grant);
    this.grantValues.splice(this.grantValues.indexOf(grant), 1);
    this.sendUpdate();
  }

  private async sendUpdate() {
    await this.loading
      .wrap(
        lastValueFrom(
          this.usersApi.updateProxy(
            this.proxy.grantingUsername,
            this.proxy.grantedUsername,
            this.proxy.id,
            {
              start: this.proxy.start,
              end: this.proxy.end,
              grants: this.proxy.grants,
            }
          )
        )
      )
      .catch(() => {});
    this.edited.emit(this.proxy);
  }
}
