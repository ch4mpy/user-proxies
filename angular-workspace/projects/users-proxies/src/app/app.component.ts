import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Deeplinks } from '@awesome-cordova-plugins/deeplinks/ngx';
import { MenuController, NavController, Platform } from '@ionic/angular';
import { Subscription } from 'rxjs/internal/Subscription';
import { SettingsScreen } from './settings/settings.screen';
import { UserAccountScreen } from './user-account/user-account.screen';
import { UserService } from './user.service';

@Component({
  selector: 'app-root',
  template: `
    <ion-app>
      <ion-split-pane contentId="main">
        <ion-menu side="start" contentId="main">
          <ion-header>
            <ion-toolbar translucent color="primary">
              <ion-title>Menu</ion-title>
            </ion-toolbar>
          </ion-header>
          <ion-content>
            <ion-menu-toggle autoHide="false">
              <ion-list>
                <ion-item
                  routerDirection="root"
                  routerLink="/account"
                  lines="none"
                  detail="false"
                  *ngIf="userService.isAuthenticated"
                >
                  <ion-icon slot="start" name="person-circle"></ion-icon>
                  <ion-label>{{ userService.preferredUsername }}</ion-label>
                </ion-item>
                <ion-item
                  lines="none"
                  detail="false"
                  *ngIf="!userService.isAuthenticated"
                  (click)="userService.login()"
                >
                  <ion-icon slot="start" name="person-circle"></ion-icon>
                  <ion-label>Login</ion-label>
                </ion-item>
                <ion-item
                  routerDirection="root"
                  routerLink="/settings"
                  lines="none"
                  detail="false"
                >
                  <ion-icon slot="start" name="settings"></ion-icon>
                  <ion-label>Configuration</ion-label>
                </ion-item>
              </ion-list>
            </ion-menu-toggle>
          </ion-content>
        </ion-menu>
        <ion-router-outlet id="main"></ion-router-outlet>
      </ion-split-pane>
    </ion-app>
  `,
  styles: [],
})
export class AppComponent implements OnInit, AfterViewInit, OnDestroy {
  private deeplinksRouteSubscription?: Subscription;

  constructor(
    private menuController: MenuController,
    public userService: UserService,
    private platform: Platform,
    private deeplinks: Deeplinks,
    private navController: NavController,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('PLATFORMS: ' + this.platform.platforms());
    if (this.platform.is('capacitor')) {
      this.setupDeeplinks();
    }
  }

  public openMenu() {
    return this.menuController.open();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy() {
    this.deeplinksRouteSubscription?.unsubscribe();
  }

  private setupDeeplinks() {
    this.deeplinksRouteSubscription = this.deeplinks
      .routeWithNavController(this.navController, {})
      .subscribe({
        next: async (match) => {
          console.log('Deeplink matched: ', match)
          await this.navController.navigateForward(
            match.$link.path + '?' + match.$link.queryString
          );
          await this.userService.refresh()
          this.cdr.detectChanges()
        },
        error: (nomatch) =>
          console.error(
            "Deeplink didn't match",
            JSON.stringify(nomatch)
          ),
      });
  }
}
