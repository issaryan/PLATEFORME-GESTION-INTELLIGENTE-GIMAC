import { useMemo } from 'react'
import { encode } from 'uqr'

export interface QrCodeProps {
  /** Contenu encodé (URI otpauth://). Calculé dans le navigateur, jamais envoyé à un service tiers. */
  value: string
  /** Nom accessible de l'image. */
  label: string
  /** Côté en pixels (192 par défaut). */
  size?: number
  'data-testid'?: string
}

// Code QR calculé localement (D-65) : modules sombres sur fond blanc dans les deux thèmes (la lecture par un
// téléphone l'exige), zone de silence de quatre modules, correction d'erreur M, un seul tracé SVG net à
// toute échelle ; jetons du thème seulement. La bibliothèque d'encodage (uqr) est embarquée : aucun réseau.
// Promu au système de conception (D-63) depuis « Mon compte » (configuration du TOTP).
export function QrCode(p: QrCodeProps) {
  const { n, trace } = useMemo(() => {
    const qr = encode(p.value, { ecc: 'M', border: 4 })
    const d: string[] = []
    qr.data.forEach((ligne, y) => ligne.forEach((sombre, x) => { if (sombre) d.push(`M${x} ${y}h1v1h-1z`) }))
    return { n: qr.data.length, trace: d.join('') }
  }, [p.value])
  const cote = p.size ?? 192
  return (
    <svg role="img" aria-label={p.label} viewBox={`0 0 ${n} ${n}`} width={cote} height={cote} shapeRendering="crispEdges" data-testid={p['data-testid']}
      style={{ display: 'block', border: 'var(--gm-border-1) solid var(--gm-color-border)' }}>
      <rect width={n} height={n} style={{ fill: 'var(--gm-white)' }} />
      <path d={trace} style={{ fill: 'var(--gm-grey-900)' }} />
    </svg>
  )
}
