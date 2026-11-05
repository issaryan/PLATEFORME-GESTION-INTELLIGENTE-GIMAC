// crypto.randomUUID en http (contexte non sécurisé) : premier import, avant toute bibliothèque.
import '@gimac/socle/polyfill'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import '@gimac/design-system/reset.css'
import '@gimac/design-system/styles.css'
import { initI18n, langueInitiale, reprendreSessionDemo } from '@gimac/socle'
import { catalogues } from '@gimac/lot1'
import { catalogueCompte } from '@gimac/compte'
import { cataloguesAdministration } from '@gimac/administration'
import { cataloguesExploitation } from '@gimac/exploitation'
import { cataloguesSandbox } from '@gimac/sandbox-admin'
import App from './App.tsx'

// Démonstration (services simulés) : session apportée par une autre application dans le fragment d'adresse (D-79).
if (import.meta.env.VITE_SERVICES === 'simule') reprendreSessionDemo()

initI18n([...catalogues, catalogueCompte, ...cataloguesAdministration, ...cataloguesExploitation, ...cataloguesSandbox], langueInitiale())

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
