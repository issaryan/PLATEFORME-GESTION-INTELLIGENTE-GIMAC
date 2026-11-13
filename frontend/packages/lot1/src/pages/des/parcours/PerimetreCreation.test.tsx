import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import i18next from 'i18next'
import { ApiError } from '@gimac/socle'
import InstitutionPage from '../InstitutionPage'
import ParcoursPage from './ParcoursPage'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import type { OuvertureLiaison, PerimetreProposable } from '../../../modele/parcours'

// D-145 : le gestionnaire du projet définit le périmètre à la création, sur les deux écrans d'ouverture du DES.
afterEach(cleanup)
const CHEF = ['PAR_INSTITUTION_READ', 'HOM_LIAISON_LIST', 'HOM_LIAISON_READ', 'HOM_DASHBOARD_READ', 'HOM_LIAISON_OPEN', 'HOM_PROJECT_DESIGNATE']
const SOUSCRITS: PerimetreProposable = {
  souscrits: true,
  services: [
    { code: 'W2W', libelle: 'W2W', interfaceSandbox: 'MOBILE' },
    { code: 'ISO.W2W', libelle: 'ISO.W2W', interfaceSandbox: 'ISO20022' },
    { code: 'CRT.NET', libelle: 'Gestion du réseau', interfaceSandbox: 'CARTE' },
  ],
}
function services(proposable: () => Promise<PerimetreProposable>, recus: OuvertureLiaison[]): Services {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, parcours: { ...s.parcours, perimetreProposable: proposable, ouvrirLiaison: async (o) => { recus.push(o); throw new ApiError(422, 'HOM_PERIMETRE_SERVICE_NON_SOUSCRIT', 'refus', 'cid') } } }
}
const fiche = (s: Services, langue?: 'fr' | 'en' | 'es') =>
  rendrePage(<InstitutionPage />, { chemin: '/des/institutions/INST-0042?formulaire=liaison', motif: '/des/institutions/:id', services: s, langue, session: sessionTest(CHEF, ['ROLE_CHEF_DES']) })
const formulaire = (c: HTMLElement) => waitFor(() => {
  const f = c.querySelector('form')
  if (!f) throw new Error('formulaire absent')
  return f
})
const choisirType = async (c: HTMLElement, type: string) => {
  await waitFor(() => expect(c.querySelector(`input[type="radio"][value="${type}"]`)).toBeTruthy())
  fireEvent.click(c.querySelector(`input[type="radio"][value="${type}"]`)!)
}
const choisirConducteur = async (c: HTMLElement, nom: string) => {
  await waitFor(() => expect(c.querySelector(`select[name="${nom}"] option[value="U-COND-2"]`)).toBeTruthy())
  fireEvent.change(c.querySelector(`select[name="${nom}"]`)!, { target: { value: 'U-COND-2' } })
}

describe('Périmètre défini à la création du projet (D-145)', () => {
  it('fiche de l’institution : services souscrits tous retenus, le gestionnaire en retire un, le périmètre part avec la création', async () => {
    const recus: OuvertureLiaison[] = []
    const { container } = fiche(services(async () => SOUSCRITS, recus))
    const f = await formulaire(container)
    expect(screen.getByText(/Choisissez l'institution et le type de projet/)).toBeTruthy()
    await choisirType(container, 'PAIEMENT_MOBILE')
    await choisirConducteur(container, 'chefDeProjetId')
    expect(await screen.findByLabelText('ISO.W2W · mobile ISO 20022')).toBeTruthy()
    expect((screen.getByLabelText('W2W · mobile propriétaire') as HTMLInputElement).checked).toBe(true)
    expect(screen.getByText(/Services souscrits par l'institution/)).toBeTruthy()
    fireEvent.click(screen.getByLabelText('Gestion du réseau (CRT.NET) · carte ISO 8583'))
    fireEvent.submit(f)
    await waitFor(() => expect(recus).toHaveLength(1))
    expect(recus[0]).toMatchObject({ institutionId: 'INST-0042', typeProjet: 'PAIEMENT_MOBILE', perimetre: ['W2W', 'ISO.W2W'] })
  })
  it('fiche de l’institution : aucun service retenu, la création est retenue et l’erreur affichée', async () => {
    const recus: OuvertureLiaison[] = []
    const { container } = fiche(services(async () => ({ souscrits: false, services: [{ code: 'RETRAIT', libelle: 'Retrait', interfaceSandbox: null }] }), recus))
    const f = await formulaire(container)
    await choisirType(container, 'CARTE_INTERBANCAIRE')
    await choisirConducteur(container, 'chefDeProjetId')
    fireEvent.click(await screen.findByLabelText('Retrait (RETRAIT)'))
    expect(screen.getByText(/catalogue du type de projet est proposé/)).toBeTruthy()
    fireEvent.submit(f)
    expect(await screen.findByText('Retenez au moins un service au périmètre du projet.')).toBeTruthy()
    expect(recus).toEqual([])
  })
  it('services proposables illisibles ou absents : la création part sans périmètre, le service dérive', async () => {
    const recus: OuvertureLiaison[] = []
    const { container } = fiche(services(async () => { throw new ApiError(503, 'HOM_REGISTRE_INDISPONIBLE', 'registre', 'cid') }, recus), 'en')
    const f = await formulaire(container)
    await choisirType(container, 'PAIEMENT_MOBILE')
    await choisirConducteur(container, 'chefDeProjetId')
    expect(await screen.findByText(/The eligible services cannot be read at the moment/)).toBeTruthy()
    fireEvent.submit(f)
    await waitFor(() => expect(recus).toHaveLength(1))
    expect(recus[0].perimetre).toBeUndefined()
    cleanup()
    const vide = fiche(services(async () => ({ souscrits: false, services: [] }), []), 'es')
    await formulaire(vide.container)
    await choisirType(vide.container, 'PAIEMENT_MOBILE')
    expect(await screen.findByText(/Ningún servicio es elegible para este tipo de proyecto/)).toBeTruthy()
    await i18next.changeLanguage('fr')
  })
  it('dialogue du parcours : périmètre proposé dès l’institution et le type choisis, envoyé avec la création', async () => {
    const recus: OuvertureLiaison[] = []
    const { container } = rendrePage(<ParcoursPage />, { chemin: '/des/parcours?formulaire=ouvrir', motif: '/des/parcours', services: services(async () => SOUSCRITS, recus), session: sessionTest(CHEF, ['ROLE_CHEF_DES']) })
    await waitFor(() => expect(container.ownerDocument.querySelector('select[name="institutionId"] option[value="INST-0042"]')).toBeTruthy())
    const d = container.ownerDocument.body
    fireEvent.change(d.querySelector('select[name="institutionId"]')!, { target: { value: 'INST-0042' } })
    await choisirType(d, 'PAIEMENT_MOBILE')
    fireEvent.click(d.querySelector('input[type="radio"][value="SANS"]')!)
    await choisirConducteur(d, 'conducteurId')
    fireEvent.click(await screen.findByLabelText('W2W · mobile propriétaire'))
    fireEvent.click(screen.getByLabelText('ISO.W2W · mobile ISO 20022'))
    fireEvent.click(screen.getByLabelText('Gestion du réseau (CRT.NET) · carte ISO 8583'))
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir la liaison' }))
    expect(await screen.findByText('Retenez au moins un service au périmètre du projet.')).toBeTruthy()
    expect(recus).toEqual([])
    fireEvent.click(screen.getByLabelText('ISO.W2W · mobile ISO 20022'))
    fireEvent.click(screen.getByRole('button', { name: 'Ouvrir la liaison' }))
    await waitFor(() => expect(recus).toHaveLength(1))
    expect(recus[0]).toMatchObject({ institutionId: 'INST-0042', perimetre: ['ISO.W2W'] })
  })
})
