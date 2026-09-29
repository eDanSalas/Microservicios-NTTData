import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';

import { OrderHistoryComponent } from './order-history.component';

describe('OrderHistoryComponent', () => {
  it('should load pages and owned details', () => {
    const history: any = {
      findAll: jasmine.createSpy('findAll').and.returnValue(Observable.of({
        content: [{id: 'order-1'}], page: 0, totalPages: 1
      })),
      findOne: jasmine.createSpy('findOne').and.returnValue(Observable.of({id: 'order-1'})),
      reorder: jasmine.createSpy('reorder').and.returnValue(Observable.of({status: 'QUOTE'})),
      cancel: jasmine.createSpy('cancel')
    };
    const component = new OrderHistoryComponent(history);

    component.ngOnInit();
    component.view('order-1');

    expect(history.findAll).toHaveBeenCalledWith(0, 10);
    expect(history.findOne).toHaveBeenCalledWith('order-1');
    expect(component.selected.id).toBe('order-1');
  });

  it('should keep the same key when confirming a quote', () => {
    const history: any = {
      findAll: jasmine.createSpy('findAll'),
      findOne: jasmine.createSpy('findOne'),
      reorder: jasmine.createSpy('reorder').and.returnValue(Observable.of({status: 'QUOTE'})),
      cancel: jasmine.createSpy('cancel')
    };
    const component = new OrderHistoryComponent(history);
    component.selected = {id: 'order-1'};
    component.paymentMethodId = 'payment-2';

    component.reorder();
    const key = component.reorderKey;
    component.reorder(true);

    expect(history.reorder.calls.argsFor(0)).toEqual(['order-1', 'payment-2', false, key]);
    expect(history.reorder.calls.argsFor(1)).toEqual(['order-1', 'payment-2', true, key]);
  });

  it('should cancel the selected order and refresh the page', () => {
    const history: any = {
      findAll: jasmine.createSpy('findAll').and.returnValue(Observable.of({
        content: [], page: 0, totalPages: 1
      })),
      findOne: jasmine.createSpy('findOne'),
      reorder: jasmine.createSpy('reorder'),
      cancel: jasmine.createSpy('cancel').and.returnValue(Observable.of({
        id: 'order-1', status: 'CANCELLED'
      }))
    };
    const component = new OrderHistoryComponent(history);
    component.selected = {id: 'order-1', status: 'CREATED'};
    component.cancellationReason = 'Changed plans';

    component.cancel();

    expect(history.cancel).toHaveBeenCalledWith('order-1', 'Changed plans');
    expect(history.findAll).toHaveBeenCalledWith(0, 10);
  });
});
