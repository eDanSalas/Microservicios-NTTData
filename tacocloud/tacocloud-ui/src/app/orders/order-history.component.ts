import { Component, OnInit } from '@angular/core';

import { OrderHistoryService } from './order-history.service';

@Component({
  selector: 'order-history',
  templateUrl: 'order-history.component.html',
  styleUrls: ['order-history.component.css']
})
export class OrderHistoryComponent implements OnInit {
  orders: any[] = [];
  selected: any;
  paymentMethodId = '';
  reorderResult: any;
  reorderKey = '';
  cancellationReason = '';
  page = 0;
  totalPages = 0;
  readonly size = 10;

  constructor(private history: OrderHistoryService) { }

  ngOnInit() {
    this.load(0);
  }

  load(page: number) {
    if (page < 0 || this.totalPages && page >= this.totalPages) return;
    this.history.findAll(page, this.size).subscribe(result => {
      this.orders = result.content;
      this.page = result.page;
      this.totalPages = result.totalPages;
      this.selected = null;
      this.reorderResult = null;
      this.reorderKey = '';
      this.cancellationReason = '';
    });
  }

  view(id: string) {
    this.history.findOne(id).subscribe(order => {
      this.selected = order;
      this.reorderResult = null;
      this.reorderKey = '';
      this.cancellationReason = '';
    });
  }

  reorder(confirmPriceChange = false) {
    if (!this.paymentMethodId.trim()) return;
    if (!this.reorderKey) this.reorderKey = 'reorder-' + this.selected.id + '-' + Date.now();
    this.history.reorder(this.selected.id, this.paymentMethodId, confirmPriceChange,
        this.reorderKey).subscribe(result => this.reorderResult = result);
  }

  cancel() {
    this.history.cancel(this.selected.id, this.cancellationReason).subscribe(order => {
      this.selected = order;
      this.load(this.page);
    });
  }
}
