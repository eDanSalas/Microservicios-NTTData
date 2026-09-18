import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { RegisterComponent } from './register.component';

class RouterStub {
  navigate = jasmine.createSpy('navigate');
}

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let http: HttpTestingController;
  let router: RouterStub;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [RegisterComponent],
      imports: [FormsModule, HttpClientTestingModule],
      providers: [{provide: Router, useClass: RouterStub}],
      schemas: [NO_ERRORS_SCHEMA]
    });
    component = TestBed.createComponent(RegisterComponent).componentInstance;
    http = TestBed.get(HttpTestingController);
    router = TestBed.get(Router);
  });

  afterEach(() => http.verify());

  it('should obtain CSRF and register through the backend', () => {
    component.ngOnInit();
    http.expectOne('/csrf').flush({headerName: 'X-CSRF-TOKEN', token: 'token-1'});

    component.register();

    const request = http.expectOne('/register');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('token-1');
    expect(request.request.withCredentials).toBe(true);
    request.flush(null);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
