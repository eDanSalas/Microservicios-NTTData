import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { OrderHistoryService } from './order-history.service';

describe('OrderHistoryService', () => {
  let service: OrderHistoryService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OrderHistoryService]
    });
    service = TestBed.get(OrderHistoryService);
    http = TestBed.get(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should request the private paged history', () => {
    service.findAll(2, 10).subscribe();

    const request = http.expectOne('/api/v1/users/me/orders?page=2&size=10');
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);
    request.flush({content: []});
  });

  it('should request an owned order detail', () => {
    service.findOne('order-1').subscribe();

    const request = http.expectOne('/api/v1/users/me/orders/order-1');
    expect(request.request.method).toBe('GET');
    request.flush({id: 'order-1'});
  });

  it('should reorder with csrf and an idempotency key', () => {
    service.reorder('order-1', 'payment-2', false, 'retry-1').subscribe();

    const csrf = http.expectOne('/csrf');
    csrf.flush({headerName: 'X-CSRF-TOKEN', token: 'csrf-token'});
    const request = http.expectOne('/api/v1/orders/order-1/reorder');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({paymentMethodId: 'payment-2', confirmPriceChange: false});
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-token');
    expect(request.request.headers.get('Idempotency-Key')).toBe('retry-1');
    expect(request.request.withCredentials).toBe(true);
    request.flush({status: 'CREATED'});
  });

  it('should cancel with csrf and the supplied reason', () => {
    service.cancel('order-1', 'Changed plans').subscribe();

    const csrf = http.expectOne('/csrf');
    csrf.flush({headerName: 'X-CSRF-TOKEN', token: 'csrf-token'});
    const request = http.expectOne('/api/v1/orders/order-1/cancel');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({reason: 'Changed plans'});
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('csrf-token');
    request.flush({status: 'CANCELLED'});
  });
});
