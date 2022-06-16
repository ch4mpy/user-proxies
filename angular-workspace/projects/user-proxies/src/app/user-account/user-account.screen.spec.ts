import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserAccountScreen } from './user-account.screen';

describe('UserAccountScreen', () => {
  let component: UserAccountScreen;
  let fixture: ComponentFixture<UserAccountScreen>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ UserAccountScreen ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserAccountScreen);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
