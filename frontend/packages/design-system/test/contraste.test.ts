// Contrastes WCAG 2.2 des paires employées par Button, StatusBadge et TextField, calculés
// depuis tokens.json dans les deux thèmes (DS-GIMAC-01 §3.3 et §12.2).
import { describe, expect, it } from 'vitest'
import jetons from '../src/jetons/tokens.json'

type Theme = 'light' | 'dark'
const table = new Map(jetons.color.tokens.map((t) => [t.name, t.value as string | Record<Theme, string>]))

function resoudre(nom: string, theme: Theme): string {
  const v = table.get(nom)
  if (v === undefined) throw new Error(`jeton inconnu : ${nom}`)
  const brut = typeof v === 'string' ? v : v[theme] ?? v.light
  const alias = brut.match(/^\{(.+)\}$/)
  return alias ? resoudre(alias[1], theme) : brut
}

function luminance(hex: string): number {
  const [r, g, b] = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
  const lin = (c: number) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)
  return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
}

export function contraste(avant: string, fond: string, theme: Theme): number {
  const [a, b] = [luminance(resoudre(avant, theme)), luminance(resoudre(fond, theme))]
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05)
}

const TEXTE: [string, string, string][] = [
  ['Button primary', 'gm-color-text-inverse', 'gm-color-action'],
  ['Button primary au survol', 'gm-color-text-inverse', 'gm-color-action-hover'],
  ['Button secondary et ghost', 'gm-color-action', 'gm-color-surface'],
  ['Button danger', 'gm-color-status-crit-fg', 'gm-color-surface'],
  ['StatusBadge ok', 'gm-color-status-ok-fg', 'gm-color-status-ok-bg'],
  ['StatusBadge warn', 'gm-color-status-warn-fg', 'gm-color-status-warn-bg'],
  ['StatusBadge crit', 'gm-color-status-crit-fg', 'gm-color-status-crit-bg'],
  ['StatusBadge neutral', 'gm-color-status-neutral-fg', 'gm-color-surface-sunken'],
  ['TextField valeur', 'gm-color-text-primary', 'gm-color-surface'],
  ['TextField étiquette sur la page', 'gm-color-text-primary', 'gm-color-bg-page'],
  ['TextField aide', 'gm-color-text-muted', 'gm-color-surface'],
  ['TextField erreur', 'gm-color-status-crit-fg', 'gm-color-surface'],
]

describe.each(['light', 'dark'] as Theme[])('contrastes en thème %s', (theme) => {
  it.each(TEXTE)('%s : texte à 4,5:1 au moins', (_nom, avant, fond) => {
    expect(contraste(avant, fond, theme)).toBeGreaterThanOrEqual(4.5)
  })
  it('anneau de focus à 3:1 au moins sur la surface', () => {
    expect(contraste('gm-color-focus', 'gm-color-surface', theme)).toBeGreaterThanOrEqual(3)
  })
})

describe('écarts du système publié, conservés à l\'identique et signalés', () => {
  // DS-GIMAC-01 §3.3 exige 3:1 pour les bordures de champ ; en clair, gm-grey-400 sur blanc
  // n'atteint que 1,9:1. En sombre, la bordure (#7A818C) tient 3,3:1. À trancher au référentiel.
  it('bordure de champ en sombre à 3:1 au moins', () => {
    expect(contraste('gm-color-border-strong', 'gm-color-surface', 'dark')).toBeGreaterThanOrEqual(3)
  })
  it('StatusBadge info en clair à 4,5:1 au moins', () => {
    expect(contraste('gm-color-status-info-fg', 'gm-color-action-subtle', 'light')).toBeGreaterThanOrEqual(4.5)
  })
  // Corrigé (D-62) : le fond d'action atténué en sombre passe de #2A4467 (4,48:1) à #263E5F (4,9:1).
  it('StatusBadge info en sombre à 4,5:1 au moins', () => {
    expect(contraste('gm-color-status-info-fg', 'gm-color-action-subtle', 'dark')).toBeGreaterThanOrEqual(4.5)
  })
  it.fails('bordure de champ en clair à 3:1 au moins (écart connu)', () => {
    expect(contraste('gm-color-border-strong', 'gm-color-surface', 'light')).toBeGreaterThanOrEqual(3)
  })
})
