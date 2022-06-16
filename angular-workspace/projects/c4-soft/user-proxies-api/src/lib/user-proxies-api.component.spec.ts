import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserProxiesApiComponent } from './user-proxies-api.component';

describe('UserProxiesApiComponent', () => {
  let component: UserProxiesApiComponent;
  let fixture: ComponentFixture<UserProxiesApiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ UserProxiesApiComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserProxiesApiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
