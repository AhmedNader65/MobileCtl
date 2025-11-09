import { defineConfig } from 'vitepress'

export default defineConfig({
  base: '/MobileCtl/',
  title: 'MobileCtl',
  description: 'Modern DevOps automation for mobile apps. Build, version, and deploy iOS & Android with a single command.',

  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/logo.svg' }],
    ['meta', { name: 'theme-color', content: '#646cff' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:locale', content: 'en' }],
    ['meta', { property: 'og:title', content: 'MobileCtl | Modern Mobile DevOps' }],
    ['meta', { property: 'og:description', content: 'Build, version, and deploy iOS & Android with a single command.' }],
    ['meta', { property: 'og:site_name', content: 'MobileCtl' }],
  ],

  themeConfig: {
    logo: '/logo.svg',

    nav: [
      { text: 'Guide', link: '/guide/getting-started' },
      { text: 'Commands', link: '/reference/commands' },
      { text: 'Config', link: '/reference/configuration' },
      { text: 'Examples', link: '/examples/' },
      {
        text: 'v0.2.0',
        items: [
          { text: 'Changelog', link: '/changelog' },
          { text: 'Contributing', link: '/contributing' }
        ]
      }
    ],

    sidebar: {
      '/guide/': [
        {
          text: 'Introduction',
          items: [
            { text: 'What is MobileCtl?', link: '/guide/what-is-mobilectl' },
            { text: 'Getting Started', link: '/guide/getting-started' },
            { text: 'Installation', link: '/guide/installation' },
            { text: 'Setup Wizard', link: '/guide/setup-wizard' },
            { text: 'Quick Start', link: '/guide/quick-start' }
          ]
        },
        {
          text: 'Credentials & Setup',
          items: [
            { text: 'Google Play Service Account', link: '/guide/google-play-service-account' },
            { text: 'Firebase Service Account', link: '/guide/firebase-service-account' },
            { text: 'App Store Connect API Key', link: '/guide/app-store-connect-api-key' }
          ]
        },
        {
          text: 'Core Concepts',
          items: [
            { text: 'Configuration', link: '/guide/configuration' },
            { text: 'Build Automation', link: '/guide/build-automation' },
            { text: 'Version Management', link: '/guide/version-management' },
            { text: 'Changelog Generation', link: '/guide/changelog' },
            { text: 'Deployment', link: '/guide/deployment' }
          ]
        },
        {
          text: 'Advanced',
          items: [
            { text: 'Multi-Platform Builds', link: '/guide/multi-platform' },
            { text: 'Environment Variables', link: '/guide/environment' },
            { text: 'Backups & Recovery', link: '/guide/backups' },
            { text: 'CI/CD Integration', link: '/guide/ci-cd' }
          ]
        }
      ],
      '/reference/': [
        {
          text: 'Command Reference',
          items: [
            { text: 'Overview', link: '/reference/commands' },
            { text: 'setup', link: '/reference/setup' },
            { text: 'build', link: '/reference/build' },
            { text: 'deploy', link: '/reference/deploy' },
            { text: 'version', link: '/reference/version' },
            { text: 'changelog', link: '/reference/changelog' }
          ]
        },
        {
          text: 'Configuration',
          items: [
            { text: 'Configuration File', link: '/reference/configuration' },
            { text: 'App Config', link: '/reference/config-app' },
            { text: 'Build Config', link: '/reference/config-build' },
            { text: 'Version Config', link: '/reference/config-version' },
            { text: 'Changelog Config', link: '/reference/config-changelog' },
            { text: 'Deploy Config', link: '/reference/config-deploy' },
            { text: 'Notifications', link: '/reference/config-notifications' }
          ]
        }
      ],
      '/examples/': [
        {
          text: 'Examples',
          items: [
            { text: 'Overview', link: '/examples/' },
            { text: 'Android App', link: '/examples/android' },
            { text: 'iOS App', link: '/examples/ios' },
            { text: 'Multi-Platform', link: '/examples/multi-platform' },
            { text: 'CI/CD Workflows', link: '/examples/ci-cd' },
            { text: 'Advanced Scenarios', link: '/examples/advanced' }
          ]
        }
      ]
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/AhmedNader65/MobileCtl' }
    ],

    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Made with ❤️ for mobile developers'
    },

    search: {
      provider: 'local'
    },

    outline: {
      level: [2, 3]
    },

    editLink: {
      pattern: 'https://github.com/AhmedNader65/MobileCtl/edit/master/website/:path',
      text: 'Edit this page on GitHub'
    }
  }
})
