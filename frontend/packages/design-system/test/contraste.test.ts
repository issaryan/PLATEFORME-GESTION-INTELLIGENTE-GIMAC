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
  // Aide et libellés atténués posés sur le fond de page (en-tête de fiche, « non renseigné ») : D-63.
  ['Aide sur la page', 'gm-color-text-muted', 'gm-color-bg-page'],
  ['TextField erreur', 'gm-color-status-crit-fg', 'gm-color-surface'],
  // Mon compte (docs/lot1/mon-compte.md §4) : initiales de l'avatar, menu utilisateur, recadrage, appareils.
  ['Avatar : initiales', 'gm-color-action', 'gm-color-action-subtle'],
  ['Menu utilisateur : nom', 'gm-color-text-primary', 'gm-color-surface-raised'],
  ['Menu utilisateur : fonction', 'gm-color-text-secondary', 'gm-color-surface-raised'],
  ['Bouton du menu utilisateur survolé', 'gm-color-text-primary', 'gm-color-action-subtle'],
  ['Recadrage : aide et valeur du zoom dans un dialogue', 'gm-color-text-secondary', 'gm-color-surface-raised'],
  ['Appareils : détail sur la surface', 'gm-color-text-secondary', 'gm-color-surface'],
  // Fil de la messagerie sur fond texturé (D-66) : le texte n'est jamais posé sur le motif ; fonds pleins inchangés.
  ['Messagerie : bulle reçue', 'gm-color-text-primary', 'gm-color-surface-sunken'],
  ['Messagerie : bulle envoyée', 'gm-color-text-primary', 'gm-color-action-subtle'],
  ['Messagerie : auteur et heure (cartouche pleine)', 'gm-color-text-secondary', 'gm-color-surface'],
  ['Messagerie : lien dans une bulle reçue', 'gm-color-action', 'gm-color-surface-sunken'],
]

describe.each(['light', 'dark'] as Theme[])('contrastes en thème %s', (theme) => {
  it.each(TEXTE)('%s : texte à 4,5:1 au moins', (_nom, avant, fond) => {
    expect(contraste(avant, fond, theme)).toBeGreaterThanOrEqual(4.5)
  })
  it('anneau de focus à 3:1 au moins sur la surface', () => {
    expect(contraste('gm-color-focus', 'gm-color-surface', theme)).toBeGreaterThanOrEqual(3)
  })
})

// États actif, survol et focus dans les deux thèmes (D-66) : barre latérale sombre en sombre, entrée
// active en texte blanc sur une teinte, sans filet ; onglets, menus, listes, boutons secondaires,
// en-têtes de tableau, sélection ; indicateurs et cartouches.
const ETATS: [string, string, string][] = [
  ['Barre latérale : entrée au repos', 'gm-color-nav-texte', 'gm-color-nav-fond'],
  ['Barre latérale : entrée active (texte et pictogramme)', 'gm-color-nav-texte-actif', 'gm-color-nav-actif'],
  ['Barre latérale : entrée survolée', 'gm-color-nav-texte-actif', 'gm-color-nav-survol'],
  ['Barre latérale : entrée focalisée', 'gm-color-nav-texte-actif', 'gm-color-nav-fond'],
  ['Barre latérale : intitulé de groupe', 'gm-color-nav-groupe', 'gm-color-nav-fond'],
  ['Onglet actif sur la page', 'gm-color-action', 'gm-color-bg-page'],
  ['Onglet actif sur une carte', 'gm-color-action', 'gm-color-surface'],
  ['Onglet survolé', 'gm-color-text-primary', 'gm-color-surface'],
  ['Entrée de menu survolée ou focalisée', 'gm-color-text-primary', 'gm-color-action-subtle'],
  ['Option de liste survolée', 'gm-color-text-primary', 'gm-color-action-subtle'],
  ['Bouton secondaire survolé', 'gm-color-action', 'gm-color-action-subtle'],
  ['Bouton principal survolé', 'gm-color-text-inverse', 'gm-color-action-hover'],
  ['En-tête de tableau', 'gm-color-table-entete-texte', 'gm-color-table-entete-fond'],
  ['Ligne de tableau survolée', 'gm-color-text-primary', 'gm-color-action-subtle'],
  ['Ligne de tableau sélectionnée', 'gm-color-text-primary', 'gm-color-surface-selected'],
  ['Lien dans une ligne survolée', 'gm-color-action', 'gm-color-action-subtle'],
  ['Liste de liens survolée', 'gm-color-action', 'gm-color-surface-sunken'],
  ['Jour choisi du calendrier', 'gm-color-text-inverse', 'gm-color-action'],
  ['Menu surgissant et dialogue', 'gm-color-text-primary', 'gm-color-surface-raised'],
  ['Aide dans un dialogue', 'gm-color-text-secondary', 'gm-color-surface-raised'],
  ['Pied de dialogue (cartouche)', 'gm-color-text-primary', 'gm-color-surface-sunken'],
  ['Note d\'aide (cartouche)', 'gm-color-text-primary', 'gm-color-surface-sunken'],
  ['Libellé d\'indicateur', 'gm-color-text-secondary', 'gm-color-surface'],
  ['Indicateur : état nominal', 'gm-color-status-ok-fg', 'gm-color-surface'],
  ['Indicateur : état à surveiller', 'gm-color-status-warn-fg', 'gm-color-surface'],
  ['Indicateur : état critique', 'gm-color-status-crit-fg', 'gm-color-surface'],
  ['Indicateur : information', 'gm-color-status-info-fg', 'gm-color-surface'],
  ['Indicateur cliquable survolé', 'gm-color-text-primary', 'gm-color-surface-sunken'],
  ['Champ désactivé ou en lecture seule', 'gm-color-text-secondary', 'gm-color-surface-sunken'],
  ['Puce d\'information', 'gm-color-status-info-fg', 'gm-color-action-subtle'],
]

describe.each(['light', 'dark'] as Theme[])('états actif, survol et focus en thème %s', (theme) => {
  it.each(ETATS)('%s : texte à 4,5:1 au moins', (_nom, avant, fond) => {
    expect(contraste(avant, fond, theme)).toBeGreaterThanOrEqual(4.5)
  })
  it('anneau de focus à 3:1 au moins sur la page et les cartouches', () => {
    expect(contraste('gm-color-focus', 'gm-color-bg-page', theme)).toBeGreaterThanOrEqual(3)
    expect(contraste('gm-color-focus', 'gm-color-surface-sunken', theme)).toBeGreaterThanOrEqual(3)
  })
  it('anneau de focus de la barre latérale à 3:1 au moins', () => {
    expect(contraste('gm-color-nav-texte-actif', 'gm-color-nav-fond', theme)).toBeGreaterThanOrEqual(3)
  })
})

describe('en-tête de tableau en mode sombre', () => {
  it('fond sombre (bleu nuit), jamais un aplat clair lumineux', () => {
    expect(luminance(resoudre('gm-color-table-entete-fond', 'dark'))).toBeLessThan(0.1)
    expect(luminance(resoudre('gm-color-table-entete-texte', 'dark'))).toBeGreaterThan(0.7)
  })
})

describe('barre latérale en mode sombre (D-66)', () => {
  it('fond sombre du thème, jamais le bleu du thème clair', () => {
    expect(resoudre('gm-color-nav-fond', 'dark')).not.toBe(resoudre('gm-color-nav-fond', 'light'))
    expect(luminance(resoudre('gm-color-nav-fond', 'dark'))).toBeLessThanOrEqual(luminance(resoudre('gm-color-bg-page', 'dark')))
  })
  it('texte de l\'entrée active blanc', () => {
    expect(resoudre('gm-color-nav-texte-actif', 'dark').toLowerCase()).toBe('#ffffff')
  })
})

describe('écarts du système publié, conservés à l\'identique et signalés', () => {
  // DS-GIMAC-01 §3.3 exige 3:1 pour les bordures de champ (WCAG 1.4.11) : en clair gm-grey-500
  // (#8A8F98, 3,25:1 ; gm-grey-400 n'atteignait que 1,9:1), en sombre #7A818C (3,3:1).
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
  it('bordure de champ en clair à 3:1 au moins', () => {
    expect(contraste('gm-color-border-strong', 'gm-color-surface', 'light')).toBeGreaterThanOrEqual(3)
  })
})

// Motif du fil (D-66) : trait très atténué, quasi invisible à côté du texte (moins de 1,3:1 sur la surface).
describe.each(['light', 'dark'] as Theme[])('motif de trame du fil en thème %s', (theme) => {
  it('jeton rgba atténué (6 à 8 %), de la teinte brand-accent', () => {
    const v = table.get('gm-color-motif-fil') as Record<Theme, string>
    const [, r, g, b, a] = v[theme].match(/rgba\((\d+), (\d+), (\d+), ([\d.]+)\)/)!.map(Number)
    expect(a).toBeGreaterThanOrEqual(0.04)
    expect(a).toBeLessThanOrEqual(0.08)
    const accent = resoudre('gm-color-brand-accent', theme).toLowerCase()
    expect(`#${[r, g, b].map((x) => x.toString(16).padStart(2, '0')).join('')}`).toBe(accent)
  })
})
