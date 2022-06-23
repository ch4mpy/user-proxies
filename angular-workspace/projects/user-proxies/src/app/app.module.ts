import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';
import { BrowserModule } from '@angular/platform-browser';
import { RouteReuseStrategy } from '@angular/router';
import { Deeplinks } from '@awesome-cordova-plugins/deeplinks/ngx';
import { IonicModule, IonicRouteStrategy } from '@ionic/angular';
import { IonicStorageModule } from '@ionic/storage-angular';
import { AuthInterceptor, AuthModule } from 'angular-auth-oidc-client';
import { environment } from '../environments/environment';

import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatSelectModule } from '@angular/material/select';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HomeScreen } from './home.screen';
import { NetworkErrorDialog } from './network-error.dialog';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatMomentDateModule, MAT_MOMENT_DATE_ADAPTER_OPTIONS, MomentDateAdapter, MomentDateModule } from '@angular/material-moment-adapter';

@NgModule({
  declarations: [AppComponent, NetworkErrorDialog, HomeScreen],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    MomentDateModule,
    HttpClientModule,
    MatAutocompleteModule,
    MatDialogModule,
    MatSelectModule,
    IonicModule.forRoot(),
    IonicStorageModule.forRoot(),
    AuthModule.forRoot(environment.authConfig),
    AppRoutingModule,
    BrowserAnimationsModule,
  ],
  providers: [
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    Deeplinks,
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    {
      provide: DateAdapter,
      useClass: MomentDateAdapter,
      deps: [MAT_DATE_LOCALE, MAT_MOMENT_DATE_ADAPTER_OPTIONS],
    },
    {
      provide: MAT_DATE_FORMATS,
      useValue: {
        parse: {
          dateInput: 'YYYY-MM-DD',
        },
        display: {
          dateInput: 'YYYY-MM-DD',
        },
      },
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
