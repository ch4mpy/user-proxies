import { Injectable } from '@angular/core'
import { BehaviorSubject, finalize, Observable } from 'rxjs'

@Injectable({ providedIn: 'root' })
export class LoadingService {

  private _isLoading$ = new BehaviorSubject<boolean>(false)

  get isLoading(): boolean {
    return this._isLoading$.value
  }

  get isLoading$(): Observable<boolean> {
    return this._isLoading$
  }

  start() {
    this._isLoading$.next(true)
  }

  end() {
    this._isLoading$.next(false)
  }

  async wrap<T>(promise: Promise<T>): Promise<T> {
    this.start()
    return promise.finally(() => this.end())
  }

  wrapObs<T>(obs: Observable<T>): Observable<T> {
    this.start()
    return obs.pipe(finalize(() => this.end()))
  }

}
