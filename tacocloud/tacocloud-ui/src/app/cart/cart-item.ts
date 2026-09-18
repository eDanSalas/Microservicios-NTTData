export class CartItem {

  quantity = 1;

  taco: any;

  constructor(taco: any) {
    this.taco = taco;
  }

  get unitPrice() {
    return this.taco.ingredients.reduce((total, ingredient) => total + Number(ingredient.unitPrice), 0);
  }

  get lineTotal() {
    return this.quantity * this.unitPrice;
  }

}
