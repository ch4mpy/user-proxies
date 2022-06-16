import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AutoLoginPartialRoutesGuard } from 'angular-auth-oidc-client';
import { UserAccountScreen } from './user-account.screen';

const routes: Routes = [
  {
    path: '',
    canActivate: [AutoLoginPartialRoutesGuard],
    component: UserAccountScreen,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class UserAccountRoutingModule {}
