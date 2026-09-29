import { Component, OnInit } from '@angular/core';

import { RatingService } from './rating.service';

@Component({
  selector: 'top-tacos',
  templateUrl: 'top-tacos.component.html',
  styleUrls: ['top-tacos.component.css']
})
export class TopTacosComponent implements OnInit {
  ranking: any[] = [];

  constructor(private ratings: RatingService) { }

  ngOnInit() {
    this.ratings.getTop(10).subscribe(ranking => this.ranking = ranking);
  }
}
