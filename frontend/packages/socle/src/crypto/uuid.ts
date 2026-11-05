// Identifiant aléatoire au format UUID v4 (RFC 9562). crypto.randomUUID n'existe que dans un contexte sécurisé
// (HTTPS ou localhost) : servie en http par un nom interne (D-75), la plateforme tire les octets avec
// crypto.getRandomValues, disponible partout. Sans générateur cryptographique, aucune clé n'est tirée : une erreur
// explicite l'emporte sur un aléa prévisible (Math.random).

type CryptoPartiel = { randomUUID?: () => string; getRandomValues?: (t: Uint8Array<ArrayBuffer>) => Uint8Array<ArrayBuffer> }

const octets = (c: CryptoPartiel | undefined): Uint8Array<ArrayBuffer> => {
  const o = new Uint8Array(16)
  if (typeof c?.getRandomValues !== 'function') throw new Error('crypto.getRandomValues indisponible : aucun identifiant aléatoire sûr ne peut être tiré')
  return c.getRandomValues(o)
}

/** UUID v4 tiré par randomUUID s'il existe, sinon à partir de seize octets de getRandomValues ; sans l'un ni l'autre, erreur. */
export function uuidV4(c: CryptoPartiel | undefined = globalThis.crypto): string {
  if (typeof c?.randomUUID === 'function') return c.randomUUID()
  const o = octets(c)
  o[6] = (o[6] & 0x0f) | 0x40
  o[8] = (o[8] & 0x3f) | 0x80
  const h = Array.from(o, (b) => b.toString(16).padStart(2, '0')).join('')
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`
}

/**
 * Pose crypto.randomUUID quand le navigateur ne l'expose pas (contexte non sécurisé), pour les bibliothèques qui
 * l'appellent directement ; chargé en tête de chaque application par '@gimac/socle/polyfill'.
 */
export function installerRandomUUID(c: CryptoPartiel | undefined = globalThis.crypto): void {
  if (!c || typeof c.randomUUID === 'function') return
  c.randomUUID = () => uuidV4({ getRandomValues: c.getRandomValues?.bind(c) })
}
