import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { pluginsE2E } from '../../e2e/instrumentation.mts'

// Couche « Portail des participants ». En développement, les appels /api et /bff sont relayés
// vers MS-GATEWAY (modèle BFF, RG-58) : aucun jeton n'est manipulé par le navigateur.
const passerelle = process.env.GIMAC_PASSERELLE ?? 'http://localhost:8080'

export default defineConfig({
  // GIMAC_E2E=1 : construction instrumentée pour la couverture des tests de bout en bout (e2e/).
  plugins: [react(), ...pluginsE2E()],
  server: {
    port: 4201,
    strictPort: true,
    proxy: {
      '/api': passerelle,
      '/bff': { target: passerelle, ws: false },
      '/logout': passerelle,
    },
  },
})
