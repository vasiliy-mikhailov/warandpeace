import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * ONE BUNDLE, SERVED BY THE JAVA PROCESS THAT SERVES THE API.
 *
 * No node in the runtime image and no second container. The reader is already an HTTP server with a
 * record mounted into it; giving it a `static/` directory is a handler, where a Node process beside
 * it would be a second thing to keep alive, a second thing to restart, and a second place for the
 * dashboard to be dark exactly when the sweep it reports on has died.
 */
export default defineConfig({
  plugins: [react()],
  base: '/',
  build: { outDir: 'dist', emptyOutDir: true, sourcemap: false },
})
