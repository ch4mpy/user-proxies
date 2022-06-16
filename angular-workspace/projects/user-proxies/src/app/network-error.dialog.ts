import { Component, Inject, OnInit } from '@angular/core'
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

export class NetworkErrorDialogData {
  constructor(readonly title: string, readonly messages: string[]) {}
}

@Component({
  selector: 'app-network-error-dialog',
  template: `
    <div class="error-dialog">
      <h1 mat-dialog-title>{{data.title}}</h1>
      <div mat-dialog-content>
        <p *ngFor="let msg of data.messages">{{msg}}</p>
      </div>
    </div>
  `,
  styles: [],
})
export class NetworkErrorDialog implements OnInit {
  constructor(@Inject(MAT_DIALOG_DATA) public data: NetworkErrorDialogData) {}

  ngOnInit(): void {}
}
