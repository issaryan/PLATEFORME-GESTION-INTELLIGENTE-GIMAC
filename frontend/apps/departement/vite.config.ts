import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Couche « Application du département ». En développement, les appels /api et /oauth2 sont relayés
// vers MS-GATEWAY (modèle BFF, RG-58) : aucun jeton n'est manipulé par le navigateur.
const passerelle = process.env.GIMAC_PASSERELLE ?? 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 4200,
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
