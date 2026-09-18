import { Component, OnInit, Injectable } from '@angular/core';
import { CartService } from './cart-service';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Component({
  selector: 'taco-cart',
  templateUrl: 'cart.component.html',
  styleUrls: ['./cart.component.css']
})

@Injectable()
export class CartComponent implements OnInit {

  model = {
    deliveryName: '',
    deliveryStreet: '',
    deliveryCity: '',
    deliveryState: '',
    deliveryZip: '',
    paymentMethodId: '',
    couponCode: '',
    items: []
  };

  constructor(private cart: CartService, private httpClient: HttpClient) {
    this.cart = cart;
  }

  ngOnInit() {}

  get cartItems() {
    return this.cart.getItemsInCart();
  }

  get cartTotal() {
    return this.cart.getCartTotal();
  }

  onSubmit() {
    this.model.items = this.cart.getItemsInCart().filter(item => Number(item.quantity) > 0).map(item => ({
      taco: {
        name: item.taco.name,
        ingredientIds: item.taco.ingredients.map(ingredient => ingredient.id)
      },
      quantity: Number(item.quantity)
    }));

    this.httpClient.post(
        'http://localhost:8080/api/orders',
        this.model, {
            headers: new HttpHeaders().set('Content-type', 'application/json')
                    .set('Accept', 'application/json'),
        }).subscribe(r => this.cart.emptyCart());

  }

}
