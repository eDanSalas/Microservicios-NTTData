import { BrowserModule } from '@angular/platform-browser';
import { HttpModule } from '@angular/http';
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { HeaderComponent } from './header/header.component';
import { FooterComponent } from './footer/footer.component';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './login/login.component';
import { RecentTacosComponent } from './recents/recents.component';
import { ApiService } from './api/ApiService';
import { RecentTacosService } from './recents/RecentTacosService';
import { SpecialsComponent } from './specials/specials.component';

import { routes } from './app.routes';
import { CloudTitleComponent } from './cloud-title/cloudtitle.component';
import { NonWrapsPipe } from './recents/NonWrapsPipe';
import { WrapsPipe } from './recents/WrapsPipe';
import { DesignComponent } from './design/design.component';
import { GroupBoxComponent } from './group-box/groupbox.component';
import { BigButtonComponent } from './big-button/bigbutton.component';
import { LittleButtonComponent } from './little-button/littlebutton.component';
import { LocationsComponent } from './locations/locations.component';
import { FormGroupDirective } from '@angular/forms/src/directives/reactive_directives/form_group_directive';
import { HttpClientModule } from '@angular/common/http';

import { OAuthModule } from 'angular-oauth2-oidc';
import { CartComponent } from './cart/cart.component';
import { CartService } from './cart/cart-service';
import { RegisterComponent } from './register/register.component';
import { FavoriteService } from './favorites/favorite.service';
import { RatingService } from './ratings/rating.service';
import { TopTacosComponent } from './ratings/top-tacos.component';
import { OrderHistoryComponent } from './orders/order-history.component';
import { OrderHistoryService } from './orders/order-history.service';

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    HomeComponent,
    LoginComponent,
    RegisterComponent,
    FooterComponent,
    RecentTacosComponent,
    SpecialsComponent,
    LocationsComponent,
    CloudTitleComponent,
    DesignComponent,
    CartComponent,
    NonWrapsPipe,
    WrapsPipe,
    GroupBoxComponent,
    BigButtonComponent,
    LittleButtonComponent,
    TopTacosComponent,
    OrderHistoryComponent,
  ],
  imports: [
    BrowserModule,
    HttpModule,
    HttpClientModule,
    FormsModule,
    RouterModule.forRoot(routes),
    OAuthModule.forRoot()
  ],
  providers: [
    ApiService,
    CartService,
    RecentTacosService,
    FavoriteService,
    RatingService,
    OrderHistoryService,
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
