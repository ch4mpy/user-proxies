import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ProxyDto, UsersApi } from '@c4-soft/user-proxies-api';
import { lastValueFrom } from 'rxjs';
import { ProxyEditDialog } from './proxy-edit.dialog';

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
        <div class="indent" fxLayout="row" fxLayout.lt-sm="column">
          <div *ngFor="let g of proxy.grants">{{ g }}</div>
        </div>
      </div>
      <div class="indent" fxLayout="row">
        <div>{{ proxy.start | date }}&nbsp;</div>
        <div *ngIf="!!proxy.end">- {{ proxy.end | date }}</div>
      </div>
    </div>
    <div fxFlex></div>
    <div fxLayout="row">
      <div fxFlex></div>
      <div style="margin: .5em;">
        <button mat-mini-fab (click)="openEditDialog()">
          <mat-icon>edit</mat-icon>
        </button>
      </div>
      <div style="margin: .5em;">
        <button mat-mini-fab (click)="delete()">
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

  @Input()
  isEditable!: boolean;

  @Output()
  deleted = new EventEmitter<ProxyDto>();

  @Output()
  edited = new EventEmitter<ProxyDto>();

  constructor(private usersApi: UsersApi, private matDlg: MatDialog) {}

  ngOnInit(): void {}

  delete() {
    lastValueFrom(
      this.usersApi.deleteProxy(
        this.proxy.grantingUsername,
        this.proxy.grantedUsername,
        this.proxy.id
      )
    ).then(() => this.deleted.emit(this.proxy));
  }

  openEditDialog() {
    const dlg = this.matDlg.open(ProxyEditDialog, {
      data: this.proxy,
    });
    dlg.afterClosed().subscribe(() => this.edited.emit(this.proxy));
  }
}
