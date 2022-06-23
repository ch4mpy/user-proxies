import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FlexLayoutModule } from '@angular/flex-layout';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { IonicModule } from '@ionic/angular';
import { ProxyCreationDialog } from './proxy-creation.dialog';
import { UserAccountRoutingModule } from './user-account-routing.module';
import { UserAccountScreen } from './user-account.screen';
import { ProxyComponent } from './proxy.component';
import { MatDialogModule } from '@angular/material/dialog';
import { ProxyEditDialog } from './proxy-edit.dialog';

@NgModule({
  declarations: [UserAccountScreen, ProxyCreationDialog, ProxyEditDialog, ProxyComponent],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    IonicModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
    UserAccountRoutingModule,
  ],
  providers: [],
})
export class UserAccountModule {}
