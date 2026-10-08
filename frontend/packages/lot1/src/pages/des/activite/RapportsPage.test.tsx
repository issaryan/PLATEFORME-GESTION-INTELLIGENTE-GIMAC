import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { ApiError } from '@gimac/socle'
import RapportsPage from './RapportsPage'
import EquipePage from './EquipePage'
import TableauDeBordPage from '../TableauDeBordPage'
import { TexteRapport } from './communs'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import type { Services } from '../../../services/ports'
import { PROFILS } from '../../../permissions'
import { ajouterJours, aujourdhui, semaineIso, versSaisie } from '../../../modele/jours'
import { render } from '@testing-library/react'

// Rapports (D-76, /des/rapports) : mes rapports et rapports reçus, création selon trois modes (prérempli,
// rédigé, document déposé avec progression), brouillon, soumission, consultation, aperçu du document, visa ;
// Mon équipe (/des/equipe) ; section « Ma journée » du tableau de bord.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const COLLAB = { id: 'u-collab', nom: 'Nadège Mvondo Assiga' }
const RESP = { id: 'u-resp-info', nom: 'Gaston Ndzana Ebode' }
const services = (u = COLLAB) => servicesSimules({ latenceMs: 0, utilisateur: u })
const session = (u = COLLAB, role = 'ROLE_COLLABORATEUR') => ({ ...sessionTest(PROFILS[role], [role]), sujet: u.id, nom: u.nom })
const page = (chemin = '/des/rapports', o: { s?: Services; u?: typeof COLLAB; role?: string } = {}) =>
  rendrePage(<RapportsPage />, { chemin, motif: '/des/rapports', services: o.s ?? services(o.u), session: session(o.u, o.role) })

describe('Rapports', () => {
  it('rédiger un rapport en Markdown : période déjà couverte refusée, puis brouillon enregistré et soumis', async () => {
    page()
    expect(await screen.findByRole('heading', { level: 1, name: 'Rapports' })).toBeTruthy()
    expect(screen.queryByRole('tab')).toBeNull()
    expect(await screen.findByRole('button', { name: /^Semaine \d+, du / })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Nouveau rapport' }))
    const d = await screen.findByRole('dialog', { name: 'Nouveau rapport' })
    fireEvent.click(within(d).getByLabelText('Rédiger'))
    fireEvent.change(within(d).getByLabelText(/^Jour/), { target: { value: versSaisie(ajouterJours(aujourdhui(), -1)) } })
    fireEvent.change(within(d).getByLabelText(/Contenu du rapport/), { target: { value: '# Bilan\n\n- **Liaison** testée\nTexte libre' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le brouillon' }))
    expect(await within(d).findByText('Vous avez déjà un rapport pour cette période : ouvrez-le depuis la liste.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Jour/), { target: { value: 'hier' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le brouillon' }))
    expect(await within(d).findByText('Date attendue au format JJ/MM/AAAA.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Jour/), { target: { value: versSaisie(aujourdhui()) } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le brouillon' }))
    expect(await screen.findByText('Brouillon enregistré.')).toBeTruthy()
    // La consultation s'ouvre sur le brouillon : texte rendu, modification puis soumission.
    const c = await screen.findByRole('dialog', { name: /Rapport quotidien de Nadège Mvondo Assiga/ })
    expect(await within(c).findByRole('heading', { name: 'Bilan' })).toBeTruthy()
    expect(within(c).getByText('Liaison').tagName).toBe('STRONG')
    fireEvent.click(within(c).getByRole('button', { name: 'Modifier le brouillon' }))
    const m = await screen.findByRole('dialog', { name: 'Modifier le brouillon' })
    fireEvent.change(within(m).getByLabelText(/Contenu du rapport/), { target: { value: '# Bilan complété' } })
    fireEvent.click(within(m).getByRole('button', { name: 'Enregistrer et soumettre' }))
    expect(await screen.findByText(/^Rapport remis à .+ \(votre n\+1\)\.$/)).toBeTruthy()
  })

  it('destinataire annoncé avant la soumission, choix dans la chaîne hiérarchique ; refus expliqué ; dépôt au sommet', async () => {
    const s = services()
    const r = await s.pilotage.creerRapport({ type: 'QUOTIDIEN', periode: aujourdhui(), mode: 'REDIGE', contenu: 'Supervision des liens' })
    page(`/des/rapports?rapport=${r.id}`, { s })
    const c = await screen.findByRole('dialog', { name: /Rapport quotidien de Nadège Mvondo Assiga/ })
    expect(await within(c).findByText('Sera remis à : Gaston Ndzana Ebode (votre n+1)')).toBeTruthy()
    const choix = within(c).getByLabelText(/^Remettre à/) as HTMLSelectElement
    expect([...choix.options].map((o) => o.value)).toEqual(['u-resp-info', 'U-001', 'u-dg'])
    fireEvent.change(choix, { target: { value: 'U-001' } })
    expect(within(c).getByText('Sera remis à : Paul Mbarga Atangana (choisi par vous)')).toBeTruthy()
    // Refus du service : message traduit et détail rendu, jamais un 422 muet.
    vi.spyOn(s.pilotage, 'soumettre').mockRejectedValueOnce(new ApiError(422, 'PIL_DESTINATAIRE_INVALIDE', 'Hors de la chaîne hiérarchique de l’auteur', 'cid-d'))
    fireEvent.click(within(c).getByRole('button', { name: 'Enregistrer et soumettre' }))
    expect(await within(c).findByText(/n’est pas dans votre chaîne hiérarchique/)).toBeTruthy()
    expect(within(c).getByText('Hors de la chaîne hiérarchique de l’auteur')).toBeTruthy()
    fireEvent.click(within(c).getByRole('button', { name: 'Enregistrer et soumettre' }))
    expect(await screen.findByText('Rapport remis à Paul Mbarga Atangana (choisi par vous).')).toBeTruthy()
    cleanup()
    // Sommet de l'organisation : le rapport est déposé.
    const DG = { id: 'u-dg', nom: 'Jean-Baptiste Ella Mintsa' }
    const sd = services(DG)
    const rd = await sd.pilotage.creerRapport({ type: 'QUOTIDIEN', periode: aujourdhui(), mode: 'REDIGE', contenu: 'Comité de direction' })
    page(`/des/rapports?rapport=${rd.id}`, { s: sd, u: DG, role: 'ROLE_DG' })
    const cd = await screen.findByRole('dialog', { name: /Rapport quotidien de Jean-Baptiste Ella Mintsa/ })
    expect(await within(cd).findByText(/Personne n’est au-dessus de vous dans l’organisation/)).toBeTruthy()
    fireEvent.click(within(cd).getByRole('button', { name: 'Enregistrer et soumettre' }))
    expect(await screen.findByText('Rapport déposé : personne n’est au-dessus de vous, il reste consultable.')).toBeTruthy()
    expect((await sd.pilotage.rapport(rd.id)).etat).toBe('DEPOSE')
  })

  it('rapport hebdomadaire prérempli ; refus de la période future', async () => {
    const s = services()
    vi.spyOn(s.pilotage, 'preremplir').mockRejectedValueOnce(new ApiError(422, 'PIL_PERIODE_FUTURE', 'x', 'cid-7'))
    page('/des/rapports', { s })
    fireEvent.click(await screen.findByRole('button', { name: 'Nouveau rapport' }))
    const d = await screen.findByRole('dialog', { name: 'Nouveau rapport' })
    fireEvent.click(within(d).getByLabelText('Hebdomadaire'))
    fireEvent.change(within(d).getByLabelText('Semaine'), { target: { value: semaineIso(aujourdhui()) } })
    fireEvent.click(within(d).getByRole('button', { name: 'Préremplir à partir de mes activités' }))
    expect(await within(d).findByText(/La période n’a pas encore commencé/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Préremplir à partir de mes activités' }))
    await waitFor(() => expect((within(d).getByLabelText(/Contenu du rapport/) as HTMLTextAreaElement).value).toContain('## Total des heures'))
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer et soumettre' }))
    expect(await screen.findByText(/^Rapport remis à .+ \(votre n\+1\)\.$/)).toBeTruthy()
  })

  it('déposer un document : choix obligatoire, progression, puis soumission ; refus de la vérification', async () => {
    page()
    fireEvent.click(await screen.findByRole('button', { name: 'Nouveau rapport' }))
    const d = await screen.findByRole('dialog', { name: 'Nouveau rapport' })
    fireEvent.click(within(d).getByLabelText('Déposer un document'))
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer et soumettre' }))
    expect(await within(d).findByText('Choisissez le document du rapport.')).toBeTruthy()
    const champ = d.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(champ, { target: { files: [new File(['%PDF'], 'carte-client.pdf', { type: 'application/pdf' })] } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer et soumettre' }))
    expect(await within(d).findByText(/Le document a été refusé par la vérification/, {}, { timeout: 4000 })).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Retirer le fichier' }))
    fireEvent.change(champ, { target: { files: [new File(['%PDF-1.4'], 'rapport-du-jour.pdf', { type: 'application/pdf' })] } })
    fireEvent.change(within(d).getByLabelText(/Texte d’accompagnement/), { target: { value: 'Rapport signé joint.' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer et soumettre' }))
    expect(await within(d).findByRole('progressbar')).toBeTruthy()
    expect(await screen.findByText(/^Rapport remis à .+ \(votre n\+1\)\.$/, {}, { timeout: 5000 })).toBeTruthy()
    const c = await screen.findByRole('dialog', { name: /Rapport quotidien de Nadège/ })
    expect(await within(c).findByText('rapport-du-jour.pdf')).toBeTruthy()
  })

  it('responsable : rapports reçus, filtre par auteur, aperçu du document, visa commenté', async () => {
    page('/des/rapports?vue=recus&auteur=u-res', { u: RESP, role: 'ROLE_RESPONSABLE_SERVICE' })
    expect(await screen.findByText('Rapports de Idriss Mahamat Saleh.')).toBeTruthy()
    await screen.findByText('Document déposé')
    const lignes = screen.getAllByRole('row')
    expect(lignes.slice(1).every((l) => l.textContent?.includes('Idriss Mahamat Saleh'))).toBe(true)
    const document = screen.getAllByRole('row').find((l) => l.textContent?.includes('Document déposé'))!
    fireEvent.click(within(document).getByRole('button'))
    const c = await screen.findByRole('dialog', { name: /Rapport quotidien de Idriss Mahamat Saleh/ })
    fireEvent.click(await within(c).findByRole('button', { name: 'Voir le document' }))
    const apercu = await screen.findByRole('dialog', { name: /rapport-pole-reseau/ })
    fireEvent.click(within(apercu).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    await waitFor(() => expect(screen.queryByRole('dialog', { name: /rapport-pole-reseau/ })).toBeNull())
    fireEvent.change(within(c).getByLabelText('Commentaire pour l’auteur'), { target: { value: 'Merci, bien reçu.' } })
    fireEvent.click(within(c).getByRole('button', { name: 'Viser le rapport' }))
    expect(await screen.findByText('Rapport visé.')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Mes rapports' }))
    expect(await screen.findByRole('columnheader', { name: 'Soumis le' })).toBeTruthy()
  })
})

describe('Mon équipe', () => {
  it('membres : activités, rapport du jour, tâches, absence ; accès à leur journée ; équipe vide', async () => {
    rendrePage(<EquipePage />, { chemin: '/des/equipe', motif: '/des/equipe', services: services(RESP), session: session(RESP, 'ROLE_RESPONSABLE_SERVICE') })
    expect(await screen.findByRole('heading', { level: 1, name: 'Mon équipe' })).toBeTruthy()
    const chantal = (await screen.findByText('Chantal Ngo Bassong')).closest('tr')!
    expect(within(chantal).getByText('Absent')).toBeTruthy()
    expect(within(chantal).getByText('Ingénieure système · Pôle système', { exact: false })).toBeTruthy()
    expect(within(chantal).getByRole('link', { name: 'Journée' }).getAttribute('href')).toContain('collaborateur=u-sys')
    fireEvent.click(screen.getByRole('button', { name: 'Jour précédent' }))
    expect(await screen.findByRole('button', { name: 'Aujourd’hui' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Aujourd’hui' }))
    cleanup()
    rendrePage(<EquipePage />, { chemin: '/des/equipe', motif: '/des/equipe', services: services(), session: session() })
    expect(await screen.findByText('Aucun collaborateur rattaché')).toBeTruthy()
  })
  it('chef de département : membres de deuxième niveau', async () => {
    rendrePage(<EquipePage />, { chemin: '/des/equipe', motif: '/des/equipe', services: services({ id: 'U-001', nom: 'Paul Mbarga Atangana' }), session: session({ id: 'U-001', nom: 'Paul Mbarga Atangana' }, 'ROLE_CHEF_DES') })
    expect((await screen.findAllByText('Niveau 2')).length).toBe(3)
  })
})

describe('Tableau de bord : « Ma journée »', () => {
  it('synthèse du jour en tête, prochains rendez-vous, rapports à viser pour un responsable', async () => {
    rendrePage(<TableauDeBordPage />, { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', services: services(RESP), session: session(RESP, 'ROLE_RESPONSABLE_SERVICE') })
    const section = await screen.findByRole('region', { name: 'Ma journée' })
    expect(await within(section).findByText('Rapports à viser')).toBeTruthy()
    expect(within(section).getByText('Activités faites')).toBeTruthy()
    expect(within(section).getAllByRole('link', { name: /·/ }).length).toBeGreaterThan(0)
    fireEvent.click(within(section).getByText('Rapports à viser'))
  })
  it('collaborateur : pas de rapports à viser ; synthèse indisponible expliquée', async () => {
    const s = services()
    rendrePage(<TableauDeBordPage />, { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', services: s, session: session() })
    const section = await screen.findByRole('region', { name: 'Ma journée' })
    expect(await within(section).findByText('Rapport du jour')).toBeTruthy()
    expect(within(section).queryByText('Rapports à viser')).toBeNull()
    cleanup()
    const ko = services()
    vi.spyOn(ko.pilotage, 'synthese').mockRejectedValue(new ApiError(503, 'PIL_ORGANISATION_INDISPONIBLE', 'x', 'cid-8'))
    rendrePage(<TableauDeBordPage />, { chemin: '/des/tableau-de-bord', motif: '/des/tableau-de-bord', services: ko, session: session() })
    expect(await screen.findByText(/L’organisation \(équipe, responsables\) est momentanément indisponible/)).toBeTruthy()
  })
})

describe('Markdown simple des rapports', () => {
  it('titres, listes, gras et paragraphes, sans interprétation HTML', () => {
    const { container } = render(<TexteRapport contenu={'# Titre\n## Sous-titre\n### Jour\n- un\n* deux\n\n<b>brut</b> et **gras**'} />)
    expect(container.querySelectorAll('h3')).toHaveLength(1)
    expect(container.querySelectorAll('h4')).toHaveLength(2)
    expect(container.querySelectorAll('li')).toHaveLength(2)
    expect(container.querySelector('b')).toBeNull()
    expect(container.textContent).toContain('<b>brut</b>')
    expect(container.querySelector('strong')?.textContent).toBe('gras')
  })
})
