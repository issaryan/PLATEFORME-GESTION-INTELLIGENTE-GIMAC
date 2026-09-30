// Enveloppe fixe (§6.2) : la barre supérieure et la navigation latérale restent en place pendant le
// défilement. jsdom ne calcule pas la mise en page : on garde les règles qui la portent.
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const css = readFileSync(resolve(__dirname, '../src/styles/11-enveloppe.css'), 'utf8')
const regle = (selecteur: string) => css.match(new RegExp(`${selecteur.replace('.', '\\.')} \\{([^}]*)\\}`))?.[1] ?? ''

describe('enveloppe fixe', () => {
  it('barre supérieure et contenu de la navigation collés au défilement', () => {
    expect(regle('.gm-shell-top')).toMatch(/position: sticky/)
    expect(regle('.gm-shell-nav-fixe')).toMatch(/position: sticky/)
  })
  it('colonne de navigation étirée jusqu’au pied de page, aux couleurs de la marque', () => {
    expect(regle('.gm-shell-nav')).toMatch(/align-self: stretch/)
    expect(regle('.gm-sidenav')).toMatch(/linear-gradient/)
    expect(css).toMatch(/\.gm-sidenav-bande \{[^}]*--gm-cemac-/)
  })
})
