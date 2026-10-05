import { configure } from '@testing-library/react'

// Les pages attendent des services simulés et des chargements paresseux : sous la charge de la suite
// complète, le délai d'une seconde de Testing Library rend certains tests instables.
configure({ asyncUtilTimeout: 5000 })

// Contexte sécurisé du navigateur (HTTPS) : WebAuthn y est présent ; les tests qui simulent une clé de
// sécurité remplacent navigator.credentials, ceux du contexte non sécurisé (http) redéfinissent ces valeurs.
Object.defineProperty(globalThis, 'isSecureContext', { value: true, configurable: true, writable: true })
if (typeof (globalThis as { PublicKeyCredential?: unknown }).PublicKeyCredential === 'undefined') {
  Object.defineProperty(globalThis, 'PublicKeyCredential', { value: class {}, configurable: true, writable: true })
}
if (typeof navigator !== 'undefined' && !navigator.credentials) {
  Object.defineProperty(navigator, 'credentials', { value: {}, configurable: true, writable: true })
}
