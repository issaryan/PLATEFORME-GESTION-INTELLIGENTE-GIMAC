import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, within } from '@testing-library/react'
import { useParams } from 'react-router'
import axe from 'axe-core'
import { ApiError, type Langue } from '@gimac/socle'
import { rendrePage, sessionTest } from '../../../../test/rendu'
import { servicesSimules } from '../../../../services/simule'
import type { Services } from '../../../../services/ports'
import { useDossierLiaison } from '../../../../hooks/parcours'
import { PROFILS } from '../../../../permissions'
import { OngletMiseEnService } from '../onglets/OngletMiseEnService'

// Onglet « Mise en service » (étape 10) sur un banc d'essai : fiche de suivi (remise, constat), couverture de
// mise en service, suivi de l'exploitation, service supplémentaire qui conduit vers la nouvelle liaison.

afterEach(cleanup)

function Banc() {
  const { id = '' } = useParams()
  const q = useDossierLiaison(id)
  return q.data ? <div data-testid={`banc-${id}`}><OngletMiseEnService dossier={q.data} /></div> : null
}

const ouvrir = (liaison: string, role?: string, o: { services?: Services; langue?: Langue; theme?: 'light' | 'dark' } = {}) =>
  rendrePage(<Banc />, {
    chemin: `/des/parcours/${liaison}?onglet=mise-en-service`, motif: '/des/parcours/:id', services: o.services ?? servicesSimules({ latenceMs: 0 }), langue: o.langue, theme: o.theme,
    session: role ? sessionTest(PROFILS[role], [role]) : undefined,
  })

describe('Étape 10 : mise en service constatée', () => {
  it('Normes et Agréments : remise de la fiche de suivi au participant', async () => {
    ouvrir('LIA-2025-071', 'ROLE_SNA')
    expect(await screen.findByText('À remettre au participant')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Remettre la fiche au participant' }))
    const d = await screen.findByRole('alertdialog')
    expect(d.textContent).toContain('service par service')
    fireEvent.click(within(d).getByRole('button', { name: 'Remettre la fiche au participant' }))
    expect(await screen.findByText('Fiche remise : le participant la renseigne sur son portail.')).toBeTruthy()
    expect(screen.queryByTestId('actions-fiche')).toBeNull()
  })

  it('Normes et Agréments : constat de la mise en service d\'une fiche renseignée', async () => {
    ouvrir('LIA-2025-064', 'ROLE_SNA')
    expect(await screen.findByText(/est à constater/)).toBeTruthy()
    expect(screen.getByText('Ouverture reportée à la prochaine fenêtre de maintenance')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Constater la mise en service' }))
    const d = await screen.findByRole('alertdialog')
    expect(d.textContent).toContain('2 service(s) ouvert(s) sur 3')
    fireEvent.click(within(d).getByRole('button', { name: 'Constater la mise en service' }))
    expect(await screen.findByText(/Mise en service constatée le/)).toBeTruthy()
  })

  it('conducteur : lecture seule de la fiche, aucun geste', async () => {
    ouvrir('LIA-2025-064', 'ROLE_CONDUCTEUR_PROJET')
    await screen.findByText(/est à constater/)
    expect(screen.queryByTestId('actions-fiche')).toBeNull()
    expect(screen.queryByText('Service supplémentaire (rang 35)')).toBeNull()
  })

  it('refus du service lors du constat : message traduit et identifiant de corrélation', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const services: Services = { ...s, decision: { ...s.decision, constaterMiseEnService: () => Promise.reject(new ApiError(409, 'HOM_SERVICE_SHEET_INCOMPLETE', 'x', 'cid-42')) } }
    ouvrir('LIA-2025-064', 'ROLE_SNA', { services })
    fireEvent.click(await screen.findByRole('button', { name: 'Constater la mise en service' }))
    const d = await screen.findByRole('alertdialog')
    fireEvent.click(within(d).getByRole('button', { name: 'Constater la mise en service' }))
    expect(await within(d).findByText(/cid-42/)).toBeTruthy()
  })

  it('couverture de mise en service et suivi de l\'exploitation', async () => {
    ouvrir('LIA-2025-064', 'ROLE_CHEF_DES')
    expect(await screen.findByText('Couverture de mise en service')).toBeTruthy()
    expect(screen.getByText('2 services ouverts sur 3')).toBeTruthy()
    expect(await screen.findByText('Constat des services effectivement ouverts')).toBeTruthy()
    expect(screen.getByText('Suivi de l\'exploitation', { selector: 'td, span, div' })).toBeTruthy()
  })

  it('avant la production : mise en service à venir, aucun geste', async () => {
    ouvrir('LIA-2026-027', 'ROLE_SNA')
    expect(await screen.findByText('Mise en service à venir')).toBeTruthy()
    expect(screen.getByText(/« En test »/)).toBeTruthy()
    expect(screen.queryByTestId('actions-fiche')).toBeNull()
  })
})

describe('Service supplémentaire (rang 35)', () => {
  it('chef du DES : ouvre une nouvelle liaison au cadrage et y est conduit', async () => {
    ouvrir('LIA-2025-088', 'ROLE_CHEF_DES')
    const choix = await screen.findByRole('checkbox', { name: 'Cash-in agent' })
    const bouton = screen.getByRole('button', { name: 'Ouvrir une nouvelle liaison' }) as HTMLButtonElement
    expect(bouton.disabled || bouton.getAttribute('aria-disabled') === 'true').toBe(true)
    fireEvent.click(choix)
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir une nouvelle liaison' }))
    const d = await screen.findByRole('alertdialog')
    expect(d.textContent).toContain('Cash-in agent')
    fireEvent.click(within(d).getByRole('button', { name: 'Ouvrir une nouvelle liaison' }))
    const nouvelle = await screen.findByText('Cette liaison ouvre un service supplémentaire de la liaison', { exact: false })
    expect(within(nouvelle.closest('[role]') ?? nouvelle).getByRole('link', { name: 'LIA-2025-088' }).getAttribute('href')).toBe('/des/parcours/LIA-2025-088')
    expect(screen.getByTestId(/^banc-LIA-2026-/)).toBeTruthy()
  })

  it('Normes et Agréments : pas d\'ouverture de service supplémentaire', async () => {
    ouvrir('LIA-2025-088', 'ROLE_SNA')
    await screen.findByText('Fiche de suivi des services (rangs 32 et 33)')
    expect(screen.queryByRole('button', { name: 'Ouvrir une nouvelle liaison' })).toBeNull()
  })

  it('tous les services déjà au périmètre : explication', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const services: Services = { ...s, parcours: { ...s.parcours, dossier: (id) => s.parcours.dossier(id).then((d) => ({ ...d, perimetre: d.perimetre.map((x) => ({ ...x, inclus: true })) })) } }
    ouvrir('LIA-2025-088', 'ROLE_CHEF_DES', { services })
    expect(await screen.findByText(/déjà au périmètre de cette liaison/)).toBeTruthy()
  })
})

describe('Langues, thème et accessibilité', () => {
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    ouvrir('LIA-2025-064', 'ROLE_SNA', { langue })
    await screen.findByText(langue === 'en' ? 'Service opening coverage' : 'Cobertura de puesta en servicio')
    expect(document.body.textContent).not.toMatch(/\b(decision|parcours|miseEnService)\.[a-zA-Z]/)
  })

  it.each(['light', 'dark'] as const)('axe sans violation (thème %s)', async (theme) => {
    const { container } = ouvrir('LIA-2025-088', 'ROLE_CHEF_DES', { theme })
    await screen.findByRole('checkbox', { name: 'Cash-in agent' })
    await screen.findByText('Constat des services effectivement ouverts')
    const res = await axe.run(container, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] }, rules: { 'color-contrast': { enabled: false }, region: { enabled: false } } })
    expect(res.violations.map((v) => `${v.id}: ${v.nodes[0]?.html.slice(0, 120)}`)).toEqual([])
  })
})
