import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.c4_soft.users_proxies',
  appName: 'users-proxies',
  webDir: '../../dist/users-proxies',
  bundledWebRuntime: false,
  server: {
    hostname: 'localhost',
    androidScheme: 'https'
}
};

export default config;
