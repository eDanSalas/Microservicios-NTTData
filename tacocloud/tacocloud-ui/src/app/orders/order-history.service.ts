import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import 'rxjs/add/operator/switchMap';

@Injectable()
export class OrderHistoryService {
  constructor(private http: HttpClient) { }

  findAll(page: number, size: number) {
    return this.http.get<any>('/api/v1/users/me/orders?page=' + page + '&size=' + size,
        {withCredentials: true});
  }

  findOne(id: string) {
    return this.http.get<any>('/api/v1/users/me/orders/' + id, {withCredentials: true});
  }

  reorder(id: string, paymentMethodId: string, confirmPriceChange: boolean, key: string) {
    return this.http.get<any>('/csrf', {withCredentials: true}).switchMap(csrf =>
      this.http.post<any>('/api/v1/orders/' + id + '/reorder', {paymentMethodId, confirmPriceChange}, {
        withCredentials: true,
        headers: new HttpHeaders().set(csrf.headerName, csrf.token).set('Idempotency-Key', key)
      }));
  }

  cancel(id: string, reason: string) {
    return this.http.get<any>('/csrf', {withCredentials: true}).switchMap(csrf =>
      this.http.post<any>('/api/v1/orders/' + id + '/cancel', {reason}, {
        withCredentials: true,
        headers: new HttpHeaders().set(csrf.headerName, csrf.token)
      }));
  }
}
