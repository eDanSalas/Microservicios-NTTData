import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';

import { TopTacosComponent } from './top-tacos.component';

describe('TopTacosComponent', () => {
  it('should load the ranking', () => {
    const ratings: any = {getTop: jasmine.createSpy('getTop').and.returnValue(
        Observable.of([{taco: {id: 'taco-1'}, average: 4.5, count: 2}]))};
    const component = new TopTacosComponent(ratings);

    component.ngOnInit();

    expect(component.ranking[0].taco.id).toBe('taco-1');
  });
});
