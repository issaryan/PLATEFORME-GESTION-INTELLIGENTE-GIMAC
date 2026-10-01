import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import '@gimac/design-system/reset.css'
import '@gimac/design-system/styles.css'
import { initI18n, langueInitiale } from '@gimac/socle'
import { catalogueCompte } from '@gimac/compte'
import { catalogueMessagerie } from '@gimac/lot1'
import { catalogues } from './i18n'
import App from './App.tsx'

initI18n([catalogueCompte, catalogueMessagerie, ...catalogues], langueInitiale())

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
