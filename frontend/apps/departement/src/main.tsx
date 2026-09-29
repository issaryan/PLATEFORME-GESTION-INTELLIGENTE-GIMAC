import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import '@gimac/design-system/reset.css'
import '@gimac/design-system/styles.css'
import { initI18n, langueInitiale } from '@gimac/socle'
import { catalogues } from '@gimac/lot1'
import { catalogueCompte } from '@gimac/compte'
import App from './App.tsx'

initI18n([...catalogues, catalogueCompte], langueInitiale())

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
