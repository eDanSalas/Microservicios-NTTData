import { CartItem } from './cart-item';
import { CartService } from './cart-service';

describe('CartService', () => {
  it('keeps quantity greater than one and calculates the line total', () => {
    const service = new CartService();
    service.addToCart({name: 'Test taco', ingredients: [{id: 'A', unitPrice: 1.25}, {id: 'B', unitPrice: 0.50}]});
    const item: CartItem = service.getItemsInCart()[0];
    item.quantity = 2;
    expect(item.quantity).toBe(2);
    expect(item.lineTotal).toBe(3.50);
    expect(service.getCartTotal()).toBe(3.50);
  });
});
