import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';

import { RecentTacosComponent } from './recents.component';

describe('RecentTacosComponent', () => {
  it('should restore favorite state when the page reloads', () => {
    const http: any = {get: jasmine.createSpy('get').and.returnValue(
        Observable.of({content: [{id: 'taco-1'}]}))};
    const favorites: any = {
      getAll: jasmine.createSpy('getAll').and.returnValue(
          Observable.of({content: [{id: 'taco-1'}]})),
      add: jasmine.createSpy('add').and.returnValue(Observable.of({})),
      remove: jasmine.createSpy('remove').and.returnValue(Observable.of({}))
    };
    const ratings: any = {rate: jasmine.createSpy('rate').and.returnValue(Observable.of({}))};
    const component = new RecentTacosComponent(http, favorites, ratings);

    component.ngOnInit();

    expect(component.favoriteIds['taco-1']).toBe(true);
  });
});
