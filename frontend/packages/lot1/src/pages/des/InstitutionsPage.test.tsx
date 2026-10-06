import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import InstitutionsPage from './InstitutionsPage'
import { rendrePage, sessionTest } from '../../test/rendu'
import { servicesSimules } from '../../services/simule'
import type { Services } from '../../services/ports'

afterEach(cleanup)
const cleBrute = /\b(institutions?|piece|transition|adhesion|prerequis|tableau|crud|commun)\.[a-zA-Z]/
const CHARGE = ['PAR_INSTITUTION_READ', 'PAR_INSTITUTION_CREATE', 'PAR_INSTITUTION_EDIT', 'PAR_ADHESION_INSTRUCT', 'PAR_TRANSITION_REQUEST']
const LECTEUR = ['PAR_INSTITUTION_READ']
const avec = (surcharge: Partial<Services['participant']>): Services => {
  const s = servicesSimules({ latenceMs: 0 })
  return { ...s, participant: { ...s.participant, ...surcharge } }
}
const page = (o: { services?: Services; langue?: 'fr' | 'en' | 'es'; chemin?: string; permissions?: string[] } = {}) =>
  rendrePage(<InstitutionsPage />, {
    chemin: o.chemin ?? '/des/institutions', motif: '/des/institutions', langue: o.langue, services: o.services,
    session: sessionTest(o.permissions ?? CHARGE),
  })

const remplirObligatoires = (c: HTMLElement) => {
  fireEvent.change(c.querySelector('select[name="pays"]')!, { target: { value: 'GA' } })
  fireEvent.change(c.querySelector('select[name="categorie"]')!, { target: { value: 'BANQUE' } })
}

describe('DES-05 Registre des institutions', () => {
  it('nominal : liste les institutions, leur statut et leur catégorie', async () => {
    page()
    expect(await screen.findByText('Fintech Kribi Pay')).toBeTruthy()
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Institutions membres')
    expect(screen.getAllByText('Candidat').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Gabon').length).toBeGreaterThan(0)
  })
  it('chargement : squelettes pendant la requête', () => {
    const { container } = page({ services: avec({ listerInstitutions: () => new Promise(() => undefined) }) })
    expect(container.querySelectorAll('.gm-skeleton').length).toBeGreaterThan(0)
  })
  it('vide : état vide', async () => {
    const { container } = page({ services: avec({ listerInstitutions: async () => ({ contenu: [], total: 0 }) }) })
    await waitFor(() => expect(container.querySelector('.gm-empty')).toBeTruthy())
  })
  it('erreur : identifiant de corrélation', async () => {
    page({ services: avec({ listerInstitutions: async () => { throw new ApiError(503, 'SERVICE_INDISPONIBLE', 'indisponible', 'RQ-PAR-503') } }) })
    expect(await screen.findByText('RQ-PAR-503')).toBeTruthy()
  })
  it('matrice des rôles : le chargé voit « Enregistrer » et « Mes dossiers » ; un lecteur ne les voit pas', async () => {
    page()
    await screen.findByText('Fintech Kribi Pay')
    expect(screen.getByRole('button', { name: 'Enregistrer une prise de contact' })).toBeTruthy()
    expect(screen.getByRole('tab', { name: 'Mes dossiers' })).toBeTruthy()
    cleanup()
    page({ permissions: LECTEUR })
    await screen.findByText('Fintech Kribi Pay')
    expect(screen.queryByRole('button', { name: 'Enregistrer une prise de contact' })).toBeNull()
    expect(screen.queryByRole('tab', { name: 'Mes dossiers' })).toBeNull()
    expect(screen.queryByRole('button', { name: "Proposer l'adhésion" })).toBeNull()
  })
  it('onglet « Mes dossiers » : filtre les dossiers portés par le chargé', async () => {
    let filtre: unknown
    const services = avec({ listerInstitutions: async (f) => { filtre = f; return { contenu: [], total: 0 } } })
    page({ services, chemin: '/des/institutions?onglet=mesDossiers&statut=CANDIDAT' })
    await waitFor(() => expect(filtre).toMatchObject({ mesDossiers: true, statut: ['CANDIDAT'] }))
  })
  it('prise de contact : un code déjà attribué est refusé avec le motif et la corrélation', async () => {
    const services = avec({ enregistrer: async () => { throw new ApiError(409, 'PAR_CODE_ALREADY_USED', 'doublon', 'RQ-PAR-409') } })
    const { container } = page({ services, chemin: '/des/institutions?formulaire=nouvelle' })
    const form = await waitFor(() => { const f = container.querySelector('form'); if (!f) throw new Error('formulaire absent'); return f })
    fireEvent.change(container.querySelector('input[name="nom"]')!, { target: { value: 'Banque Z' } })
    remplirObligatoires(container)
    fireEvent.submit(form)
    expect(await screen.findByText(/déjà attribué à une autre institution/)).toBeTruthy()
    expect(screen.getByText('RQ-PAR-409')).toBeTruthy()
  })
  it('prise de contact : les données saisies sont transmises au service', async () => {
    let recu: unknown
    const base = servicesSimules({ latenceMs: 0 })
    const services = avec({ enregistrer: async (c) => { recu = c; return base.participant.enregistrer(c) } })
    const { container } = page({ services, chemin: '/des/institutions?formulaire=nouvelle' })
    const form = await waitFor(() => { const f = container.querySelector('form'); if (!f) throw new Error('formulaire absent'); return f })
    fireEvent.change(container.querySelector('input[name="nom"]')!, { target: { value: '  Banque Nouvelle  ' } })
    fireEvent.change(container.querySelector('input[name="codeIdentification"]')!, { target: { value: 'bnouCMCX' } })
    remplirObligatoires(container)
    fireEvent.submit(form)
    await waitFor(() => expect(recu).toMatchObject({ nom: 'Banque Nouvelle', codeIdentification: 'BNOUCMCX', sigle: null }))
  })
  it('recherche, bouton d’enregistrement et annulation du formulaire', async () => {
    const filtres: unknown[] = []
    const base = servicesSimules({ latenceMs: 0 })
    const services = avec({ listerInstitutions: async (f) => { filtres.push(f); return base.participant.listerInstitutions(f) } })
    const { container } = page({ services })
    await screen.findByText('Fintech Kribi Pay')
    fireEvent.change(container.querySelector('input[type="search"]')!, { target: { value: 'libreville' } })
    await waitFor(() => expect(filtres).toContainEqual(expect.objectContaining({ recherche: 'libreville' })))
    fireEvent.click(screen.getByRole('tab', { name: 'Mes dossiers' }))
    await waitFor(() => expect(filtres).toContainEqual(expect.objectContaining({ mesDossiers: true })))
    fireEvent.click(screen.getByRole('tab', { name: 'Toutes les institutions' }))
    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer une prise de contact' }))
    expect(await screen.findByText('Enregistrer une prise de contact', { selector: 'h1, h2, .gm-heading-display' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Annuler' }))
    // Retour à la liste : la recherche en cours est conservée.
    expect(await screen.findByText('Banque de Libreville')).toBeTruthy()
  })
  it('prise de contact : BIC, code BEAC et date normalisés avant l’envoi (date ISO, majuscules, sans espaces)', async () => {
    let recu: unknown
    const base = servicesSimules({ latenceMs: 0 })
    const services = avec({ enregistrer: async (c) => { recu = c; return base.participant.enregistrer(c) } })
    const { container } = page({ services, chemin: '/des/institutions?formulaire=nouvelle' })
    const form = await waitFor(() => { const f = container.querySelector('form'); if (!f) throw new Error('formulaire absent'); return f })
    fireEvent.change(container.querySelector('input[name="nom"]')!, { target: { value: 'Banque des Lagunes' } })
    remplirObligatoires(container)
    fireEvent.change(container.querySelector('input[name="codeIdentification"]')!, { target: { value: 'bdlg ga li xxx' } })
    fireEvent.change(container.querySelector('input[name="codeBanqueBeac"]')!, { target: { value: '10005 001' } })
    fireEvent.change(container.querySelector('input[name="datePriseDeContact"]')!, { target: { value: '12/01/2026' } })
    fireEvent.submit(form)
    await waitFor(() => expect(recu).toMatchObject({ codeIdentification: 'BDLGGALIXXX', codeBanqueBeac: '10005001', datePriseDeContact: '2026-01-12', pays: 'GA', categorie: 'BANQUE' }))
  })
  it('prise de contact : saisie non conforme signalée sous le champ, sans appel au service', async () => {
    let appels = 0
    const services = avec({ enregistrer: async () => { appels++; throw new Error('inattendu') } })
    const { container } = page({ services, chemin: '/des/institutions?formulaire=nouvelle' })
    const form = await waitFor(() => { const f = container.querySelector('form'); if (!f) throw new Error('formulaire absent'); return f })
    fireEvent.change(container.querySelector('input[name="nom"]')!, { target: { value: 'Banque des Lagunes' } })
    remplirObligatoires(container)
    fireEvent.change(container.querySelector('input[name="codeIdentification"]')!, { target: { value: 'BDLGGALIX' } })
    fireEvent.change(container.querySelector('input[name="codeBanqueBeac"]')!, { target: { value: '10005-001' } })
    fireEvent.change(container.querySelector('input[name="datePriseDeContact"]')!, { target: { value: '01/01/2999' } })
    fireEvent.submit(form)
    expect((await screen.findAllByText(/Code BIC attendu : 8 ou 11 caractères/)).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/Code banque BEAC attendu : lettres et chiffres seulement/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/ne peut pas être postérieure à aujourd'hui/).length).toBeGreaterThan(0)
    expect(container.querySelector('input[name="codeIdentification"]')!.getAttribute('aria-invalid')).toBe('true')
    expect(appels).toBe(0)
  })
  it.each([
    ['fr', 'Code BIC refusé par le service', 'Le service a refusé la saisie'],
    ['en', 'BIC code rejected by the service', 'The service rejected the input'],
    ['es', 'Código BIC rechazado por el servicio', 'El servicio rechazó los datos'],
  ] as const)('prise de contact : refus du service par champ (fieldErrors) affiché sous le champ en %s', async (langue, sousLeChamp, titre) => {
    const services = avec({
      enregistrer: async () => {
        throw new ApiError(400, 'SAISIE_INVALIDE', 'Saisie refusée', 'RQ-PAR-400', { champs: { codeIdentification: 'must match "^[A-Z]{4}…"' } })
      },
    })
    const { container } = page({ services, langue, chemin: '/des/institutions?formulaire=nouvelle' })
    const form = await waitFor(() => { const f = container.querySelector('form'); if (!f) throw new Error('formulaire absent'); return f })
    fireEvent.change(container.querySelector('input[name="nom"]')!, { target: { value: 'Banque des Lagunes' } })
    remplirObligatoires(container)
    fireEvent.change(container.querySelector('input[name="codeIdentification"]')!, { target: { value: 'BDLGGALI' } })
    fireEvent.submit(form)
    expect((await screen.findAllByText(new RegExp(sousLeChamp))).length).toBeGreaterThan(0)
    expect(screen.getByText(new RegExp(titre))).toBeTruthy()
    expect(screen.getByText('RQ-PAR-400')).toBeTruthy()
    const champ = container.querySelector('input[name="codeIdentification"]')!
    expect(champ.getAttribute('aria-invalid')).toBe('true')
    expect(container.textContent).not.toMatch(cleBrute)
  })
  it.each(['en', 'es'] as const)('rendu en %s sans clé brute', async (langue) => {
    const { container } = page({ langue })
    await screen.findByText('Fintech Kribi Pay')
    expect(container.textContent).not.toMatch(cleBrute)
    expect(container.textContent).not.toContain('Institutions membres')
  })
})
