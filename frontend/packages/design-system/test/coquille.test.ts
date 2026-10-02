// Coquille applicative (§6.2) : la page ne défile pas ; barre supérieure fixe, navigation latérale sur
// toute la hauteur disponible avec son propre défilement, zone de contenu seule à défiler. jsdom ne
// calcule pas la mise en page : on vérifie les règles qui la portent (le rendu réel est vérifié par
// Playwright, e2e/specs/coquille.spec.ts).
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const css = readFileSync(resolve(__dirname, '../src/styles/11-enveloppe.css'), 'utf8')
const regle = (selecteur: string) => css.match(new RegExp(`${selecteur.replace(/[.[\]]/g, '\\$&').replace(/>/g, '\\s*>\\s*')} \\{([^}]*)\\}`))?.[1] ?? ''

describe('coquille applicative', () => {
  it('la page ne défile pas : coquille à la hauteur de la fenêtre, débordement masqué', () => {
    expect(regle('.gm-shell')).toMatch(/height: 100dvh/)
    expect(regle('.gm-shell')).toMatch(/overflow: hidden/)
    // Navigation sur toute la hauteur à gauche ; colonne (barre supérieure, contenu, pied de page) à droite.
    expect(regle('.gm-shell')).toMatch(/flex-direction: row/)
    expect(regle('.gm-shell-colonne')).toMatch(/flex-direction: column/)
    expect(regle('.gm-shell-colonne')).toMatch(/min-width: 0/)
  })
  it('barre supérieure hors du défilement, sans position collante', () => {
    expect(regle('.gm-shell-top')).toMatch(/flex: none/)
    expect(css).not.toMatch(/position: sticky/)
  })
  it('corps de la coquille borné à l’espace restant', () => {
    expect(regle('.gm-shell-corps')).toMatch(/min-height: 0/)
    expect(regle('.gm-shell-corps')).toMatch(/display: flex/)
  })
  it('navigation latérale de largeur fixe, jamais étirée par le contenu, défilement propre de ses entrées', () => {
    expect(regle('.gm-shell-nav')).toMatch(/flex: none/)
    expect(regle('.gm-shell-nav')).not.toMatch(/overflow-y: auto/)
    expect(regle('.gm-sidenav')).toMatch(/overflow: hidden/)
    expect(regle('.gm-sidenav-entrees')).toMatch(/overflow-y: auto/)
  })
  it('zone de contenu : seul conteneur qui défile, pied de page en bas de la zone', () => {
    expect(regle('.gm-shell-contenu')).toMatch(/overflow-y: auto/)
    expect(regle('.gm-shell-contenu')).toMatch(/min-height: 0/)
    expect(regle('.gm-shell-contenu')).toMatch(/flex-direction: column/)
    expect(regle('.gm-shell-main')).toMatch(/flex: 1 0 auto/)
  })
  it('aucun calcul de hauteur au-dessus du pied de page : plus de navigation collante', () => {
    expect(css).not.toMatch(/gm-shell-nav-fixe/)
  })
  it('sobriété (DS-GIMAC-01 §0.4, D-66) : fond uni sans dégradé, ni bande de drapeaux, ni filet actif', () => {
    expect(regle('.gm-sidenav')).not.toMatch(/gradient/)
    expect(regle('.gm-sidenav')).toMatch(/--gm-color-nav-fond/)
    expect(css).not.toMatch(/gm-sidenav-bande/)
    expect(regle('.gm-sidenav a[aria-current="page"]')).toMatch(/--gm-color-nav-texte-actif/)
    expect(regle('.gm-sidenav a[aria-current="page"]')).not.toMatch(/border-left/)
  })
})
