import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'register-tacocloud',
  templateUrl: 'register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  model = {
    username: '', password: '', confirm: '', fullname: '', street: '',
    city: '', state: '', zip: '', phone: '', email: ''
  };
  error = '';
  submitting = false;
  csrf: any;

  constructor(private http: HttpClient, private router: Router) { }

  ngOnInit() {
    this.http.get('/csrf', {withCredentials: true}).subscribe(
        token => this.csrf = token,
        () => this.error = 'Registration service is not available');
  }

  register() {
    this.error = '';
    if (!this.csrf) return;
    this.submitting = true;
    const headers = new HttpHeaders().set(this.csrf.headerName, this.csrf.token);
    this.http.post('/register', this.model, {headers: headers, withCredentials: true}).subscribe(
        () => this.router.navigate(['/login']),
        response => {
          this.submitting = false;
          this.error = response.status === 409
              ? 'Username or email is already registered'
              : 'Registration could not be completed';
        });
  }
}
