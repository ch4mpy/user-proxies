import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.c4_soft.user_proxies.ui',
  appName: 'user-proxies',
  webDir: '../../dist/user-proxies',
  bundledWebRuntime: false,
  server: {
    hostname: 'localhost',
    androidScheme: 'https',
  },
};

export default config;
