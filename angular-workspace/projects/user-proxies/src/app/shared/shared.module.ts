import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UsernameAutocompleteComponent } from './username-autocomplete.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { FlexLayoutModule } from '@angular/flex-layout';



@NgModule({
  declarations: [
    UsernameAutocompleteComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    FlexLayoutModule,
    MatAutocompleteModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  exports: [
    UsernameAutocompleteComponent,
  ]
})
export class SharedModule { }
