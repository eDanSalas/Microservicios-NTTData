import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { FavoriteService } from './favorite.service';

describe('FavoriteService', () => {
  let service: FavoriteService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [FavoriteService]
    });
    service = TestBed.get(FavoriteService);
    http = TestBed.get(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should reload favorites from the authenticated user endpoint', () => {
    service.getAll().subscribe(result => expect(result.content[0].id).toBe('taco-1'));
    const request = http.expectOne('/api/v1/users/me/favorites?size=50');
    expect(request.request.withCredentials).toBe(true);
    request.flush({content: [{id: 'taco-1'}]});
  });

  it('should add and remove favorites with CSRF', () => {
    service.add('taco-1').subscribe();
    http.expectOne('/csrf').flush({headerName: 'X-CSRF-TOKEN', token: 'token-1'});
    const add = http.expectOne('/api/v1/users/me/favorites/taco-1');
    expect(add.request.method).toBe('PUT');
    expect(add.request.headers.get('X-CSRF-TOKEN')).toBe('token-1');
    add.flush({id: 'taco-1'});

    service.remove('taco-1').subscribe();
    http.expectOne('/csrf').flush({headerName: 'X-CSRF-TOKEN', token: 'token-2'});
    const remove = http.expectOne('/api/v1/users/me/favorites/taco-1');
    expect(remove.request.method).toBe('DELETE');
    expect(remove.request.headers.get('X-CSRF-TOKEN')).toBe('token-2');
    remove.flush(null);
  });
});
