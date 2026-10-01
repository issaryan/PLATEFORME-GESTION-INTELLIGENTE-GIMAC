import type { ReactElement } from 'react'
import { expect } from 'vitest'
import axe from 'axe-core'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import type { Langue } from '@gimac/socle'

// Outils des tests du portail du participant : services simulés sans latence (surchargeables), session du
// correspondant (matrice ROLE_CORRESPONDANT, éventuellement amputée), audit axe du rendu.

export const PERMISSIONS_CORRESPONDANT = ['HOM_PORTAL_READ', 'HOM_PORTAL_SUBMIT', 'CER_PORTAL_READ', 'CER_PORTAL_SIGN']

export const services = (surcharge?: (s: Services) => Partial<Services['portailParcours']>): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return surcharge ? { ...s, portailParcours: { ...s.portailParcours, ...surcharge(s) } } : s
}

export function ouvrir(page: ReactElement, chemin: string, motif: string, o: { langue?: Langue; services?: Services; sans?: string[]; autres?: { motif: string; element: ReactElement }[] } = {}) {
  const session = sessionTest(PERMISSIONS_CORRESPONDANT.filter((p) => !o.sans?.includes(p)), ['ROLE_CORRESPONDANT'])
  return rendrePage(page, { chemin, motif, langue: o.langue, services: o.services ?? services(), session: { ...session, nom: 'Correspondant A', institutionId: 'INST-0042' }, autres: o.autres })
}

/** Audit axe (WCAG 2.2 AA) du rendu ; les contrastes ne sont pas calculables sous jsdom (vérifiés en e2e). */
export async function auditer(conteneur: Element) {
  const r = await axe.run(conteneur, { rules: { 'color-contrast': { enabled: false }, region: { enabled: false } } })
  expect(r.violations.map((v) => `${v.id} : ${v.nodes.map((n) => n.html.slice(0, 120)).join(' | ')}`)).toEqual([])
}

/** Aucune clé brute ni nom d'intervenant interne du GIMAC dans le texte rendu. */
export function sansCleNiNomInterne() {
  const texte = document.body.textContent ?? ''
  expect(texte).not.toMatch(/\b(pp|parcours|erreur)\.[a-zA-Z]+\.?[a-zA-Z]*/)
  expect(texte).not.toMatch(/Conducteur de projet \d|Chargé DDRM|Ingénieur pôle|Ingénieur Normes|Opérateur production|Chef du département DDRM|\bU-[A-Z]/)
}
