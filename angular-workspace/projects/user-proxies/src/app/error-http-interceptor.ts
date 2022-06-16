import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http'
import { Injectable } from '@angular/core'
import { MatDialog } from '@angular/material/dialog'
import { Observable, throwError } from 'rxjs'
import { catchError } from 'rxjs/operators'
import { NetworkErrorDialog, NetworkErrorDialogData } from './network-error.dialog'

@Injectable({ providedIn: 'root' })
export class ErrorHttpInterceptor implements HttpInterceptor {
  constructor(private dialog: MatDialog) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        const msg =
          error.error instanceof ErrorEvent ? `Client error: ${error.error.message}` : `Server error ${error.status}: ${error.message}`
        console.warn(msg)

        if (!(error.error instanceof ErrorEvent)) {
          const data = this.getNetworkErrorDialogData(error)
          this.dialog.open(NetworkErrorDialog, { data })
        }

        return throwError(() => msg)
      })
    )
  }

  private getNetworkErrorDialogData(error: HttpErrorResponse): NetworkErrorDialogData {
    if (error?.status === 400) {
      const messages = error.error instanceof Array ? error.error : [error.message]
      return new NetworkErrorDialogData(
        'Requête malformée',
        messages
      )
    }
    if (error?.status === 401) {
      return new NetworkErrorDialogData('Authentification manquante', ['Veuillez vous identifier et recommencer'])
    }
    if (error?.status === 403) {
      return new NetworkErrorDialogData('Droits insuffisants', [
        'Vous ne disposez pas des droits nécessaires pour mener à bien la dernière action tentée.'
      ])
    }
    if (error?.status === 404) {
      return new NetworkErrorDialogData('Ressource inconnue', [error.message])
    }
    if (error?.status >= 500) {
      return new NetworkErrorDialogData(
        'Erreur Serveur',
        [error.message]
      )
    }
    return new NetworkErrorDialogData(
      'Erreur réseau',
      [error.message],
    )
  }
}