# MobileCtl Documentation

This directory contains the VitePress documentation website for MobileCtl.

## Development

```bash
# Install dependencies (from project root)
npm install

# Start dev server
npm run docs:dev
```

The site will be available at `http://localhost:5173`

## Building

```bash
# Build for production
npm run docs:build

# Preview production build
npm run docs:preview
```

## Project Structure

```
website/
├── .vitepress/
│   ├── config.mts          # VitePress configuration
│   └── theme/
│       ├── index.ts        # Custom theme
│       └── custom.css      # Premium styling
├── public/
│   └── logo.svg           # Site logo
├── guide/                 # User guides
│   ├── getting-started.md
│   ├── installation.md
│   └── ...
├── reference/            # Command and config reference
│   ├── commands.md
│   ├── version.md
│   └── ...
├── examples/            # Real-world examples
│   ├── android.md
│   ├── ios.md
│   └── ...
└── index.md            # Home page

```

## Content Guidelines

- Use clear, concise language
- Include code examples for all features
- Provide both basic and advanced examples
- Link between related pages
- Keep navigation structure intuitive

## Deployment

The documentation can be deployed to:

- GitHub Pages
- Netlify
- Vercel
- Any static hosting service

```bash
# Build
npm run docs:build

# Deploy dist folder
# website/.vitepress/dist/
```

## Contributing

To add new documentation:

1. Create markdown file in appropriate directory
2. Add to navigation in `.vitepress/config.mts`
3. Test locally with `npm run docs:dev`
4. Build to verify: `npm run docs:build`
