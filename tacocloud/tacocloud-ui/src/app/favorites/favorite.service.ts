import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/switchMap';

@Injectable()
export class FavoriteService {
  private path = '/api/v1/users/me/favorites';

  constructor(private http: HttpClient) { }

  getAll() {
    return this.http.get<any>(this.path + '?size=50', {withCredentials: true});
  }

  add(tacoId: string): Observable<any> {
    return this.csrf().switchMap(token => this.http.put(this.path + '/' + tacoId, null,
        {headers: this.headers(token), withCredentials: true}));
  }

  remove(tacoId: string): Observable<any> {
    return this.csrf().switchMap(token => this.http.delete(this.path + '/' + tacoId,
        {headers: this.headers(token), withCredentials: true}));
  }

  private csrf(): Observable<any> {
    return this.http.get<any>('/csrf', {withCredentials: true});
  }

  private headers(token: any): HttpHeaders {
    return new HttpHeaders().set(token.headerName, token.token);
  }
}
