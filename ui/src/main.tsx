import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'

import 'ratchet-ui/tokens.css'
import './palette.css'

import { App } from './App.js'

const root = document.getElementById('root')
if (root === null) {
  throw new Error('index.html has no #root to mount into')
}
createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
