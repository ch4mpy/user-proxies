import { Injectable } from '@angular/core'
import { MatSnackBar } from '@angular/material/snack-bar'
import {
  ActivatedRouteSnapshot,
  CanActivate,
  CanActivateChild,
  CanLoad,
  Route,
  Router,
  RouterStateSnapshot,
  UrlSegment,
  UrlTree,
} from '@angular/router'
import { Observable } from 'rxjs'
import { UserService } from './user.service'

@Injectable({
  providedIn: 'root',
})
export class HasRoleGuard implements CanActivate, CanActivateChild, CanLoad {
  constructor(private userService: UserService, private router: Router, private snackBar: MatSnackBar) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    return this.hasRole(route.data['role'])
  }

  canActivateChild(
    childRoute: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    return this.hasRole(childRoute.data['role'])
  }

  canLoad(route: Route, segments: UrlSegment[]): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    return this.hasRole(route.data ? route.data['role'] : undefined)
  }

  private hasRole(allowed: string | string[] | undefined): boolean {
    if (!allowed) {
      return true
    }

    if (typeof allowed === 'string' && this.userService.current.roles.includes(allowed)) {
      return true
    }

    for(const allowedRole of allowed as Array<string>) {
      if (this.userService.current.roles.includes(allowedRole)) {
        return true
      }
    }

    this.snackBar.open(`Vous n'avez pas le rôle ${allowed}`, 'Ok', {verticalPosition: 'top'})
    this.router.navigate(['/home'])
    return false
  }
}
