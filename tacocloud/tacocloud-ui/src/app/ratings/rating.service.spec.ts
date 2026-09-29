import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { RatingService } from './rating.service';

describe('RatingService', () => {
  let service: RatingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RatingService]
    });
    service = TestBed.get(RatingService);
    http = TestBed.get(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should load the top ranking', () => {
    service.getTop(10).subscribe(result => expect(result[0].taco.id).toBe('taco-1'));
    http.expectOne('/api/v1/tacos/top?limit=10').flush([{taco: {id: 'taco-1'}}]);
  });

  it('should submit a score with CSRF and no user id', () => {
    service.rate('taco-1', 5).subscribe();
    http.expectOne('/csrf').flush({headerName: 'X-CSRF-TOKEN', token: 'token-1'});
    const request = http.expectOne('/api/v1/tacos/taco-1/rating');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({score: 5});
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('token-1');
    request.flush({});
  });
});
