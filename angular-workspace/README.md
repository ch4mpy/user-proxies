# AngularWorkspace

This project was generated with https://github.com/ch4mpy/angular-ionic-workspace-template. It contains:
- a regular angular worksapce (using [Angular CLI](https://github.com/angular/angular-cli) version 14.0.1).
- an angular UI project with
  * Ionic
  * material-angular (with moment adapter)
  * angular-auth-oidc-client
  * CDK and FlexLayout
- two libs generated from OpenAPI specs of spring-boot `user-proxies-api` and `greet-api` micro-services

## Development server

Dev-server is configured with HTTPS using self-signed certificate. Please edit `projects.user-proxies.architect.serve.configurations.localhost` properties in `angular.json` to point to your own certificates files before you run `npm run user-proxies:localhost`.

To run on Android or iOS device, either virtual or not, serving from `localhost` won't work (localhost is the mobile device, not the host running dev-server). You might edit following files:
- a copy of `projects/user-proxies/src/environments/environment.bravo-ch4mp.ts`
- `angular.json`: `projects.user-proxies.architect.build.configurations.bravo-ch4mp` and `projects.user-proxies.architect.serve.configurations.bravo-ch4mp`
- `projects/user-proxies/package.json`: `android:pc` script
- `package.json`: `user-proxies:android:pc` script
