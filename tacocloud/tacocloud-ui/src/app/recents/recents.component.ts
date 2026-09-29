import { Component, OnInit, Injectable } from '@angular/core';
import { Http } from '@angular/http';
import { HttpClient } from '@angular/common/http';
import { FavoriteService } from '../favorites/favorite.service';
import { RatingService } from '../ratings/rating.service';

@Component({
  selector: 'recent-tacos',
  templateUrl: 'recents.component.html',
  styleUrls: ['./recents.component.css']
})

@Injectable()
export class RecentTacosComponent implements OnInit {
  recentTacos: any;
  favoriteIds: {[id: string]: boolean} = {};
  ratingScores: {[id: string]: number} = {};
  ratingMessages: {[id: string]: string} = {};

  constructor(private httpClient: HttpClient, private favorites: FavoriteService,
      private ratings: RatingService) { }

  ngOnInit() {
    this.httpClient.get<any>('http://localhost:8080/api/v1/tacos?size=12&sort=createdAt,desc')
        .subscribe(data => this.recentTacos = data.content);
    this.favorites.getAll().subscribe(data => data.content.forEach(taco =>
        this.favoriteIds[taco.id] = true), () => this.favoriteIds = {});
  }

  toggleFavorite(taco: any) {
    const request = this.favoriteIds[taco.id]
        ? this.favorites.remove(taco.id) : this.favorites.add(taco.id);
    request.subscribe(() => this.favoriteIds[taco.id] = !this.favoriteIds[taco.id]);
  }

  rate(taco: any) {
    const score = this.ratingScores[taco.id];
    if (!score) return;
    this.ratings.rate(taco.id, score).subscribe(result => this.ratingMessages[taco.id] =
        result.average.toFixed(2) + ' from ' + result.count + ' votes');
  }
}
