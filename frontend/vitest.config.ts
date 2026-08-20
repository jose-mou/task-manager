import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    // Unit/integration tests live in src/. `e2e/` holds Playwright specs, which
    // vitest must not pick up; scoping `include` keeps vitest's own default
    // `exclude` list intact instead of replacing it.
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
  },
})
