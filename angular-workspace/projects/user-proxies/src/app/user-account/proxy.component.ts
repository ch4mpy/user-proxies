import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ProxyDto, UsersApi } from '@c4-soft/user-proxies-api';
import { lastValueFrom } from 'rxjs';

@Component({
  selector: 'app-proxy',
  template: `
    <div fxLayout="row" fxLayout.lt-md="column">
      <div>{{ proxy.grantingUsername }} => {{ proxy.grantedUsername }}</div>
      <div>{{ proxy.grants }}</div>
      <div><button mat-mini-fab (click)="delete()"><mat-icon>delete_forever</mat-icon></button></div>
    </div>
  `,
  styles: [],
})
export class ProxyComponent implements OnInit {
  @Input()
  proxy!: ProxyDto;

  @Output()
  deleted = new EventEmitter<ProxyDto>()

  constructor(private usersApi: UsersApi) {}

  ngOnInit(): void {}

  delete() {
    lastValueFrom(this.usersApi.deleteProxy(this.proxy.grantingUsername, this.proxy.grantedUsername, this.proxy.id)).then(() => this.deleted.emit(this.proxy))
  }
}
