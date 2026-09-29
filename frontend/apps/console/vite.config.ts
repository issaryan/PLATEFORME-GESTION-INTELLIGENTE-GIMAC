import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { pluginsE2E } from '../../e2e/instrumentation.mts'

// Couche « Console d'administration et d'audit ». En développement, les appels /api, /bff et /oauth2 sont
// relayés vers MS-GATEWAY (modèle BFF, RG-58) : aucun jeton n'est manipulé par le navigateur. Le flux
// SSE /api/plateforme/flux passe par le même relais.
const passerelle = process.env.GIMAC_PASSERELLE ?? 'http://localhost:8080'

export default defineConfig({
  // GIMAC_E2E=1 : construction instrumentée pour la couverture des tests de bout en bout (e2e/).
  plugins: [react(), ...pluginsE2E()],
  server: {
    port: 4202,
    strictPort: true,
    proxy: {
      '/api': passerelle,
      '/bff': { target: passerelle, ws: false },
      '/logout': passerelle,
      '/oauth2': passerelle,
      '/login': passerelle,
    },
  },
})
