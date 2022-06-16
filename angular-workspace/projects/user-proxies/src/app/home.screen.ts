import { Component, OnInit } from '@angular/core';

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
  TODO
</ion-content>`,
  styles: [
  ]
})
export class HomeScreen implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }
}
