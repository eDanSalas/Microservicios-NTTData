import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/switchMap';

@Injectable()
export class RatingService {
  constructor(private http: HttpClient) { }

  getTop(limit: number) {
    return this.http.get<any[]>('/api/v1/tacos/top?limit=' + limit);
  }

  rate(tacoId: string, score: number): Observable<any> {
    return this.http.get<any>('/csrf', {withCredentials: true}).switchMap(token =>
        this.http.put('/api/v1/tacos/' + tacoId + '/rating', {score: score}, {
          headers: new HttpHeaders().set(token.headerName, token.token), withCredentials: true
        }));
  }
}
