import { TestBed } from '@angular/core/testing';

import { UserProxiesApiService } from './user-proxies-api.service';

describe('UserProxiesApiService', () => {
  let service: UserProxiesApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(UserProxiesApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
