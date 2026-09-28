import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@gimac/design-system/styles.css'
import './catalogue.css'
import { Catalogue } from './Catalogue'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Catalogue />
  </StrictMode>,
)
