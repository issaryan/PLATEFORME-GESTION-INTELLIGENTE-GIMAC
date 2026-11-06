import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { useLocation } from 'react-router'
import { ApiError } from '@gimac/socle'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import { magasinPouvoirs } from '../../../services/simule/pouvoirs'
import type { Services } from '../../../services/ports'
import { PROFILS } from '../../../permissions'
import ProcessusPage from './ProcessusPage'
import ModelisateurProcessusPage from './ModelisateurProcessusPage'
import InstancePage from './InstancePage'
import MaJourneePage from '../activite/MaJourneePage'
import TachesPage from '../activite/TachesPage'
import { codeLivrableLibre, erreursEtape, ongletDe } from './ModaleEtape'
import { positionsAuto } from './CanevasProcessus'
import { etapeVierge } from '../../../modele/processus'

// Processus d'unité (D-87 §4) : catalogue par unité, création, lancement en modale, modélisateur (modale d'étape à
// onglets, contrôle, publication avec levée motivée, versions, retrait), suivi d'une instance (prise, livrables, champs,
// signature FIDO2, validation, renvoi, réaffectation, suspension, reprise, annulation), « Mes étapes ».
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

const PERSONNES = {
  chef: { id: 'U-001', nom: 'Paul Mbarga Atangana', role: 'ROLE_CHEF_DES' },
  resp: { id: 'u-resp-info', nom: 'Gaston Ndzana Ebode', role: 'ROLE_RESPONSABLE_SERVICE' },
  membre: { id: 'u-res', nom: 'Idriss Mahamat Saleh', role: 'ROLE_POLE_RESEAU' },
  collab: { id: 'u-collab', nom: 'Nadège Mvondo Assiga', role: 'ROLE_COLLABORATEUR' },
}
type Qui = (typeof PERSONNES)[keyof typeof PERSONNES]
/**
 * Organisation de démonstration et un second porteur de ROLE_POLE_RESEAU au pôle réseau : les étapes par profil y
 * restent à prendre (avec un seul porteur, elles lui seraient confiées directement : désignation hors de l'unité).
 */
const magasinDeuxPorteurs = () => {
  const m = magasinPouvoirs()
  m.organisation.push({ id: 'u-res-bis', nomAffiche: 'Armel Nkoulou Beti', fonction: 'Technicien réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info', actif: true })
  m.definirProfils('u-res-bis', ['ROLE_POLE_RESEAU'])
  return m
}
const services = (q: Qui, magasin = magasinDeuxPorteurs()) => servicesSimules({ latenceMs: 0, utilisateur: { id: q.id, nom: q.nom, profils: [q.role] }, pouvoirs: magasin })
const session = (q: Qui) => ({ ...sessionTest(PROFILS[q.role], [q.role]), sujet: q.id, nom: q.nom })
function Ici() {
  const l = useLocation()
  return <span data-testid="ici">{l.pathname + l.search}</span>
}
const AUTRES = [
  { motif: '/des/processus/:id', element: <><ModelisateurProcessusPage /><Ici /></> },
  { motif: '/des/processus/instances/:id', element: <><InstancePage /><Ici /></> },
]
const catalogue = (q: Qui = PERSONNES.chef, s: Services = services(q), chemin = '/des/processus') =>
  ({ ...rendrePage(<><ProcessusPage /><Ici /></>, { chemin, motif: '/des/processus', services: s, session: session(q), autres: AUTRES }), s })
const ouvrirDefinition = async (s: Services, q: Qui, codeProcessus: string, version = 1) => {
  const p = (await s.processus.lister({})).find((x) => x.code === codeProcessus && x.version === version)!
  rendrePage(<><ModelisateurProcessusPage /><Ici /></>, { chemin: `/des/processus/${p.id}`, motif: '/des/processus/:id', services: s, session: session(q), autres: [...AUTRES, { motif: '/des/processus', element: <Ici /> }] })
  await screen.findByRole('heading', { level: 1, name: p.libelle })
  return p
}
const ouvrirInstance = async (s: Services, q: Qui, debut: string, suffixe = '') => {
  const i = (await s.processus.instances({ vue: 'unite' })).contenu.concat((await s.processus.instances({ vue: 'miennes' })).contenu).find((x) => x.intitule.startsWith(debut))!
  rendrePage(<><InstancePage /><Ici /></>, { chemin: `/des/processus/instances/${i.id}${suffixe}`, motif: '/des/processus/instances/:id', services: s, session: session(q), autres: AUTRES })
  await screen.findByRole('heading', { level: 1, name: i.intitule })
  return i
}
const confirmer = async (libelle: string, motif?: string, titre = libelle) => {
  const d = await screen.findByRole('alertdialog', { name: titre })
  if (motif !== undefined) fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: motif } })
  fireEvent.click(within(d).getByRole('button', { name: libelle }))
  await waitFor(() => expect(screen.queryByRole('alertdialog', { name: titre })).toBeNull())
}

describe('D-87 : modale d’étape (règles de forme)', () => {
  it('erreurs par champ et onglet de chaque champ ; disposition automatique en couches', () => {
    const e = {
      ...etapeVierge(1), code: 'a b', libelle: '', delaiJours: 400, intervenant: { mode: 'AU_LANCEMENT' as const }, validateur: { mode: 'PERSONNE' as const }, action: 'x'.repeat(4001),
      livrables: [{ code: 'L', libelle: '', typesPieces: [], obligatoire: true, modele: null }, { code: 'L', libelle: 'L', typesPieces: [], obligatoire: true, modele: null }],
      signatures: [{ code: 's', libelle: '', signataire: { mode: 'PROFIL' as const }, porte: [], ordre: 0 }],
      champs: [{ code: 'C', libelle: '', type: 'CHOIX' as const, choix: [], obligatoire: false }],
    }
    expect(Object.keys(erreursEtape(e, []))).toEqual(expect.arrayContaining(['code', 'libelle', 'action', 'delaiJours', 'validateur.personneId', 'validateur.remplacement',
      'livrables.0.libelle', 'livrables.1.code', 'signatures.0.code', 'signatures.0.libelle', 'signatures.0.porte', 'signatures.0.ordre', 'signatures.0.signataire.profil', 'champs.0.libelle', 'champs.0.choix']))
    expect(erreursEtape({ ...etapeVierge(1), libelle: 'X' }, ['ETAPE_1'])).toEqual({ code: 'proc.erreur.codeDouble' })
    expect(['code', 'validateur.mode', 'livrables.0.code', 'predecesseurs', 'delaiJours', 'signatures.1.porte', 'champs.0.type'].map(ongletDe))
      .toEqual(['general', 'intervenants', 'livrables', 'chronologie', 'chronologie', 'signatures', 'champs'])
    const p = positionsAuto([{ code: 'A', predecesseurs: [], position: null }, { code: 'B', predecesseurs: ['A', 'Z'], position: null }, { code: 'C', predecesseurs: ['A'], position: null }, { code: 'D', predecesseurs: ['D'], position: { x: 5, y: 6 } }])
    expect(p.get('A')).toEqual({ x: 0, y: 40 })
    expect(p.get('B')!.x).toBeGreaterThan(0)
    expect(p.get('C')!.y).toBeGreaterThan(p.get('B')!.y)
    expect(p.get('D')).toEqual({ x: 5, y: 6 })
  })
})

describe('D-87 : catalogue', () => {
  it('chef du DES : catalogue par unité, filtres, création d’un brouillon (contrôles puis refus puis succès)', async () => {
    const { s } = catalogue()
    expect(await screen.findByRole('heading', { level: 2, name: 'Pôle réseau' })).toBeTruthy()
    expect(await screen.findByRole('button', { name: 'Ouverture d’un flux réseau' })).toBeTruthy()
    fireEvent.change(screen.getByLabelText('État'), { target: { value: 'BROUILLON' } })
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Ouverture d’un flux réseau' })).toBeNull())
    fireEvent.change(screen.getByLabelText('État'), { target: { value: '' } })
    fireEvent.change(screen.getByLabelText('Rechercher'), { target: { value: 'introuvable' } })
    expect(await screen.findByText('Aucun processus')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Rechercher'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Nouveau processus' }))
    const d = await screen.findByRole('dialog', { name: 'Nouveau processus' })
    fireEvent.change(within(d).getByLabelText(/^Délai cible/), { target: { value: '400' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer le brouillon' }))
    expect(within(d).getByText('Délai de 0 à 365 jours.')).toBeTruthy()
    // D-136 : le code n'est jamais demandé, le système l'attribue ({UNITE}-PRC-{N}).
    expect(within(d).queryByLabelText(/^Code/)).toBeNull()
    expect(within(d).getByText(/attribué par le système/)).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Règle de pare-feu' } })
    fireEvent.change(within(d).getByLabelText(/^Délai cible/), { target: { value: '' } })
    fireEvent.change(within(d).getByLabelText(/^Unité/), { target: { value: 'POLE_RESEAU' } })
    fireEvent.change(within(d).getByLabelText(/^Catégorie/), { target: { value: 'Réseau' } })
    fireEvent.change(within(d).getByLabelText(/^Description/), { target: { value: 'Changement de règle' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Créer le brouillon' }))
    await waitFor(() => expect(screen.getAllByTestId('ici').at(-1)!.textContent).toMatch(/^\/des\/processus\/PRC-/))
    expect((await s.processus.lister({ q: 'pare-feu' }))[0]).toMatchObject({ etat: 'BROUILLON', categorie: 'Réseau', code: expect.stringMatching(/^POLE_RESEAU-PRC-\d+$/) })
  })

  it('membre du pôle : lancement en modale (contrôles, désignation, champs), instance ouverte ; onglet Instances', async () => {
    const { s } = catalogue(PERSONNES.membre)
    expect(screen.queryByRole('button', { name: 'Nouveau processus' })).toBeNull()
    fireEvent.click(await screen.findByRole('button', { name: 'Lancer le processus Ouverture d’un flux réseau' }))
    const d = await screen.findByRole('dialog', { name: 'Lancer le processus' })
    fireEvent.change(within(d).getByLabelText(/^Port/), { target: { value: 'abc' } })
    fireEvent.change(within(d).getByLabelText(/^Date souhaitée/), { target: { value: '31/02' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Lancer l’instance' }))
    expect(within(d).getByText('Intitulé obligatoire, 200 caractères au plus.')).toBeTruthy()
    expect(within(d).getByText('Nombre attendu.')).toBeTruthy()
    expect(within(d).getByText('Date attendue au format JJ/MM/AAAA.')).toBeTruthy()
    expect(within(d).getByText('Désignez une personne.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Intitulé de l’instance/), { target: { value: 'Flux vers la BEAC' } })
    fireEvent.change(within(d).getByLabelText(/^Adresse source/), { target: { value: '10.1.1.1' } })
    fireEvent.change(within(d).getByLabelText(/^Adresse de destination/), { target: { value: '10.2.2.2' } })
    fireEvent.change(within(d).getByLabelText(/^Port/), { target: { value: '443' } })
    fireEvent.change(within(d).getByLabelText(/^Protocole/), { target: { value: 'TCP' } })
    fireEvent.change(within(d).getByLabelText(/^Date souhaitée/), { target: { value: '15/10/2026' } })
    const choix = within(d).getByLabelText(/^Intervenant de « Mise en œuvre/) as HTMLSelectElement
    await waitFor(() => expect(choix.options.length).toBeGreaterThan(3))
    // Porteurs du profil de remplacement groupés « Dans l'unité », puis l'annuaire.
    expect([...choix.querySelectorAll('optgroup')].map((g) => g.label)).toEqual(['Dans l’unité', 'Autres personnes de l’annuaire'])
    fireEvent.change(choix, { target: { value: 'u-res-bis' } })
    const lancer = vi.spyOn(s.processus, 'lancer')
    fireEvent.click(within(d).getByRole('button', { name: 'Lancer l’instance' }))
    await waitFor(() => expect(lancer).toHaveBeenCalled())
    expect(lancer.mock.calls[0][1]).toEqual({
      intitule: 'Flux vers la BEAC', champs: { SOURCE: '10.1.1.1', DESTINATION: '10.2.2.2', PORT: 443, PROTOCOLE: 'TCP', DATE_SOUHAITEE: '2026-10-15' }, designations: { MISE_EN_OEUVRE: 'u-res-bis' },
    })
    await waitFor(() => expect(screen.getAllByTestId('ici').at(-1)!.textContent).toMatch(/^\/des\/processus\/instances\/INS-/))
    cleanup()
    catalogue(PERSONNES.membre, s, '/des/processus?vue=instances')
    expect(await screen.findByRole('button', { name: 'Flux vers la BEAC' })).toBeTruthy()
    fireEvent.change(screen.getByRole('combobox', { name: 'Instances' }), { target: { value: 'lancees' } })
    fireEvent.change(screen.getByLabelText('État'), { target: { value: 'CLOSE' } })
    expect(await screen.findByText('Vous n’avez lancé aucune instance.')).toBeTruthy()
    fireEvent.click(screen.getByRole('tab', { name: 'Catalogue' }))
    expect(await screen.findByRole('heading', { level: 2, name: /^Pôle réseau/ })).toBeTruthy()
  })
})

describe('D-87 : modélisateur', () => {
  it('brouillon : modale d’étape à onglets (contrôles, intervenant, livrable, signature, champ), propriétés, contrôle, levée motivée, publication', async () => {
    const s = services(PERSONNES.chef)
    const modifier = vi.spyOn(s.processus, 'modifier')
    const p = await ouvrirDefinition(s, PERSONNES.chef, 'ACCES_VPN_PARTICIPANT')
    expect(await screen.findByTestId('processus-canevas')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Liste' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Modifier l’étape CONFIGURATION' }))
    let d = await screen.findByRole('dialog', { name: 'Étape CONFIGURATION' })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    // Profil de remplacement exigé pour une personne nommée : l'onglet de l'erreur s'ouvre.
    expect(await within(d).findByText(/Le profil de remplacement est obligatoire/)).toBeTruthy()
    expect(within(d).getByRole('tab', { name: 'Intervenant et validateur (1)' }).getAttribute('aria-selected')).toBe('true')
    fireEvent.change(within(d).getAllByLabelText(/^Profil de remplacement/)[0], { target: { value: 'ROLE_POLE_RESEAU' } })
    fireEvent.change(within(d).getAllByLabelText(/^Désignation/)[1], { target: { value: 'PROFIL' } })
    fireEvent.change(within(d).getAllByLabelText(/^Profil(?! de)/)[0], { target: { value: 'ROLE_RESPONSABLE_SERVICE' } })
    // Couverture du profil : aucun porteur au pôle, un ailleurs dans le GIMAC.
    // QUO-01 : la cheffe du service RH porte aussi ce profil (organisation simulée alignée sur MS-PILOTAGE).
    expect(await within(d).findByText(/Aucun porteur dans l’unité ; 2 ailleurs dans le GIMAC/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('tab', { name: 'Livrables' }))
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter un livrable' }))
    fireEvent.change(within(d).getAllByLabelText(/^Libellé/)[1], { target: { value: 'Journal de configuration' } })
    fireEvent.click(within(d).getByRole('tab', { name: 'Signatures' }))
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter une signature' }))
    fireEvent.change(within(d).getAllByLabelText(/^Libellé/)[0], { target: { value: 'Visa du chef' } })
    fireEvent.click(within(d).getByLabelText('FICHE_VPN · Fiche de paramètres'))
    fireEvent.click(within(d).getByRole('tab', { name: 'Champs' }))
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter un champ' }))
    fireEvent.change(within(d).getAllByLabelText(/^Libellé/)[0], { target: { value: 'Adresse du tunnel' } })
    fireEvent.change(within(d).getByLabelText(/^Type/), { target: { value: 'CHOIX' } })
    fireEvent.change(within(d).getByLabelText(/^Choix possibles/), { target: { value: 'IPsec, OpenVPN' } })
    fireEvent.click(within(d).getByLabelText('Obligatoire'))
    fireEvent.click(within(d).getByRole('tab', { name: 'Chronologie' }))
    fireEvent.change(within(d).getByLabelText(/^Délai cible de l’étape/), { target: { value: '3' } })
    // D-106 : délai maximal de l'étape, au moins le délai cible.
    fireEvent.change(within(d).getByLabelText(/^Délai maximal de l’étape/), { target: { value: '2' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(within(d).getByText('Le délai maximal va du délai cible à 365 jours.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Délai maximal de l’étape/), { target: { value: '6' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Étape CONFIGURATION' })).toBeNull())
    const etape = modifier.mock.calls.at(-1)![1].etapes.find((e) => e.code === 'CONFIGURATION')!
    expect(etape).toMatchObject({
      delaiJours: 3, delaiMaxJours: 6, intervenant: { mode: 'PERSONNE', personneId: 'u-res', remplacement: 'ROLE_POLE_RESEAU' }, validateur: { mode: 'PROFIL', profil: 'ROLE_RESPONSABLE_SERVICE' },
      livrables: [{ code: 'FICHE_VPN' }, { code: 'LIVRABLE_1', libelle: 'Journal de configuration' }], signatures: [{ code: 'SIGNATURE_1', libelle: 'Visa du chef', porte: ['FICHE_VPN'] }],
      champs: [{ code: 'CHAMP_1', type: 'CHOIX', choix: ['IPsec', 'OpenVPN'], obligatoire: true }],
    })
    // Ajout et suppression d'une étape.
    fireEvent.click(screen.getByRole('button', { name: 'Ajouter une étape' }))
    d = await screen.findByRole('dialog', { name: 'Nouvelle étape' })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Clôture' } })
    fireEvent.click(within(d).getByRole('tab', { name: 'Chronologie' }))
    fireEvent.click(within(d).getByLabelText('CONFIGURATION · Configuration du VPN'))
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter une étape' }))
    expect(await screen.findByRole('button', { name: /^Modifier l’étape .+-E\d+$/ })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /^Modifier l’étape .+-E\d+$/ }))
    d = await screen.findByRole('dialog', { name: /^Étape .+-E\d+$/ })
    fireEvent.click(within(d).getByRole('button', { name: 'Supprimer l’étape' }))
    await confirmer('Supprimer l’étape')
    await waitFor(() => expect(screen.queryByRole('button', { name: /^Modifier l’étape .+-E\d+$/ })).toBeNull())
    // Propriétés.
    fireEvent.click(screen.getByRole('button', { name: 'Propriétés' }))
    d = await screen.findByRole('dialog', { name: 'Propriétés' })
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: '' } })
    fireEvent.change(within(d).getByLabelText(/^Délai cible/), { target: { value: '-1' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(within(d).getByText('Délai de 0 à 365 jours.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Libellé/), { target: { value: 'Accès VPN d’un participant (recette)' } })
    fireEvent.change(within(d).getByLabelText(/^Délai cible/), { target: { value: '7' } })
    // D-106 : délai maximal du processus, au moins le délai cible.
    fireEvent.change(within(d).getByLabelText(/^Délai maximal/), { target: { value: '5' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(within(d).getByText('Le délai maximal va du délai cible à 365 jours.')).toBeTruthy()
    fireEvent.change(within(d).getByLabelText(/^Délai maximal/), { target: { value: '12' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Accès VPN d’un participant (recette)' })).toBeTruthy()
    // Contrôle puis publication : avertissement levable, levée motivée.
    // Validateur par un profil porté seulement hors de l'unité : simple information ; l'unité sans chef reste levable.
    fireEvent.click(screen.getByRole('button', { name: 'Contrôler' }))
    expect(await screen.findByText('Profil porté hors de l’unité')).toBeTruthy()
    expect(screen.queryByText('Profil non couvert dans l’unité')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Publier' }))
    await confirmer('Publier', undefined, 'Publier le processus')
    const lever = await screen.findByRole('alertdialog', { name: 'Lever les avertissements' })
    expect(within(lever).getAllByText(/Unité sans chef/).length).toBeGreaterThan(0)
    expect(within(lever).queryByText(/Profil porté hors de l’unité/)).toBeNull()
    await confirmer('Lever et publier', 'Validation par le chef du service en attendant', 'Lever les avertissements')
    // D-103 : étapes et formulaire figés, le public reste réglable par qui définit.
    expect(await screen.findByText('Version publiée : étapes et formulaire en lecture seule, créez une nouvelle version pour les modifier ; son public reste réglable (« Qui peut voir et lancer »).')).toBeTruthy()
    expect((await s.processus.lire(p.id))).toMatchObject({ etat: 'PUBLIEE', avertissementsLeves: true })
  })

  it('anomalies bloquantes à la publication ; version publiée : nouvelle version, versions, retrait ; suppression d’un brouillon', async () => {
    const s = services(PERSONNES.chef)
    await ouvrirDefinition(s, PERSONNES.chef, 'ACCES_VPN_PARTICIPANT')
    fireEvent.click(screen.getByRole('button', { name: 'Publier' }))
    await confirmer('Publier', undefined, 'Publier le processus')
    expect(await screen.findByText('Le processus présente des anomalies bloquantes : corrigez-les.')).toBeTruthy()
    expect(screen.getByText('Profil de remplacement absent')).toBeTruthy()
    fireEvent.click(screen.getByText('Profil de remplacement absent'))
    expect(await screen.findByRole('dialog', { name: 'Étape CONFIGURATION' })).toBeTruthy()
    fireEvent.click(within(screen.getByRole('dialog', { name: 'Étape CONFIGURATION' })).getByRole('button', { name: 'Annuler' }))
    fireEvent.click(screen.getByRole('button', { name: 'Masquer' }))
    fireEvent.click(screen.getByRole('button', { name: 'Supprimer le brouillon' }))
    const d = await screen.findByRole('alertdialog', { name: 'Supprimer le brouillon' })
    fireEvent.click(within(d).getByRole('checkbox'))
    fireEvent.click(within(d).getByRole('button', { name: 'Supprimer le brouillon' }))
    await waitFor(() => expect(screen.getAllByTestId('ici').at(-1)!.textContent).toBe('/des/processus'))
    cleanup()
    await ouvrirDefinition(s, PERSONNES.chef, 'OUVERTURE_FLUX_RESEAU')
    expect(screen.getByText(/lecture seule/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Nouvelle version' }))
    await waitFor(() => expect(screen.getAllByTestId('ici').at(-1)!.textContent).toMatch(/^\/des\/processus\/PRC-/))
    expect(await screen.findByLabelText('Version')).toBeTruthy()
    cleanup()
    await ouvrirDefinition(s, PERSONNES.chef, 'OUVERTURE_FLUX_RESEAU')
    fireEvent.click(screen.getByRole('button', { name: 'Nouvelle version' }))
    expect(await screen.findByText('Un brouillon existe déjà pour ce processus : ouvrez-le.')).toBeTruthy()
    fireEvent.click(screen.getAllByRole('button', { name: 'Fermer' })[0])
    fireEvent.change(screen.getByLabelText('Version'), { target: { value: (await s.processus.lister({ etat: 'BROUILLON' }))[0].id } })
    await waitFor(() => expect(screen.getAllByTestId('ici').at(-1)!.textContent).not.toMatch(/PRC-0001$/))
    cleanup()
    await ouvrirDefinition(s, PERSONNES.chef, 'OUVERTURE_FLUX_RESEAU')
    fireEvent.click(screen.getByRole('button', { name: 'Retirer' }))
    await confirmer('Retirer', 'Remplacé par le nouveau circuit de demande', 'Retirer la version publiée')
    await waitFor(async () => expect((await s.processus.lister({ etat: 'RETIREE' })).length).toBe(1))
    expect(await screen.findByText(/Remplacé par le nouveau circuit de demande/)).toBeTruthy()
  })

  it('membre : consultation seule de la version publiée, lancement depuis le modélisateur ; erreur de lecture', async () => {
    const s = services(PERSONNES.membre)
    await ouvrirDefinition(s, PERSONNES.membre, 'OUVERTURE_FLUX_RESEAU')
    expect(screen.queryByRole('button', { name: 'Ajouter une étape' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Nouvelle version' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Consulter RECETTE' }))
    const d = await screen.findByRole('dialog', { name: 'Étape RECETTE' })
    expect(within(d).getByText('Consultation seule.')).toBeTruthy()
    fireEvent.click(within(d).getByRole('tab', { name: 'Intervenant et validateur' }))
    expect((within(d).getAllByLabelText(/^Désignation/)[0] as HTMLSelectElement).disabled).toBe(true)
    fireEvent.click(within(d).getAllByRole('button', { name: 'Fermer' }).at(-1)!)
    fireEvent.click(screen.getByRole('button', { name: 'Lancer' }))
    expect(await screen.findByRole('dialog', { name: 'Lancer le processus' })).toBeTruthy()
    cleanup()
    rendrePage(<ModelisateurProcessusPage />, { chemin: '/des/processus/INCONNU', motif: '/des/processus/:id', services: s, session: session(PERSONNES.membre) })
    expect(await screen.findByText('Le processus n’a pas pu être lu.')).toBeTruthy()
  })
})

describe('D-87 : suivi d’une instance', () => {
  it('chef d’unité : étape à signer (empreinte, clé de sécurité), instance close ; historique', async () => {
    const s = services(PERSONNES.resp)
    const signer = vi.spyOn(s.processus, 'signer')
    await ouvrirInstance(s, PERSONNES.resp, 'Flux SFTP')
    const panneau = screen.getByRole('complementary', { name: /Recette du flux/ })
    expect(within(panneau).getByText('Attendue de : Gaston Ndzana Ebode')).toBeTruthy()
    expect(within(panneau).getAllByText(/^SHA-256 /).length).toBe(1)
    fireEvent.click(within(panneau).getByRole('button', { name: 'Signer' }))
    const d = await screen.findByRole('dialog', { name: 'Signer : Signature du procès-verbal' })
    await waitFor(() => expect((within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }) as HTMLButtonElement).disabled).toBe(false))
    fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    expect(await within(d).findByText('Signature confirmée', {}, { timeout: 4000 })).toBeTruthy()
    expect(signer).toHaveBeenCalledWith(expect.any(String), 'RECETTE', 'SIGNATURE_RECETTE', btoa('assertion-simulee'))
    fireEvent.click(within(d).getByRole('button', { name: 'Continuer' }))
    expect(await screen.findByText('Close')).toBeTruthy()
    expect(screen.getAllByText(/Signature apposée/).length).toBeGreaterThan(0)
    fireEvent.click(screen.getByRole('button', { name: 'Liste' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Ouvrir l’étape ANALYSE' }))
    expect(await screen.findByRole('complementary', { name: /Analyse de sécurité du flux/ })).toBeTruthy()
  })

  it('clé de sécurité indisponible (http) : message clair, signature impossible ; refus de la clé restitué', async () => {
    vi.stubGlobal('isSecureContext', false)
    const s = services(PERSONNES.resp)
    await ouvrirInstance(s, PERSONNES.resp, 'Flux SFTP')
    expect(screen.getByText('La signature exige une connexion sécurisée (HTTPS)')).toBeTruthy()
    expect((screen.getByRole('button', { name: 'Signer' }) as HTMLButtonElement).getAttribute('aria-disabled') === 'true' || (screen.getByRole('button', { name: 'Signer' }) as HTMLButtonElement).disabled).toBe(true)
    cleanup()
    vi.unstubAllGlobals()
    const s2 = services(PERSONNES.resp)
    vi.spyOn(s2.processus, 'signer').mockRejectedValueOnce(new ApiError(403, 'PIL_PROCESSUS_REAUTH_REFUSEE', 'x', 'cid-s'))
    await ouvrirInstance(s2, PERSONNES.resp, 'Flux SFTP')
    fireEvent.click(screen.getByRole('button', { name: 'Signer' }))
    const d = await screen.findByRole('dialog', { name: /^Signer/ })
    fireEvent.click(within(d).getByRole('button', { name: 'Utiliser ma clé de sécurité' }))
    expect(await within(d).findByText(/Votre clé de sécurité n’a pas été acceptée|refusée/, {}, { timeout: 4000 })).toBeTruthy()
  })

  it('chef d’unité : réaffectation, suspension, reprise, annulation (motifs)', async () => {
    const s = services(PERSONNES.resp)
    const i = await ouvrirInstance(s, PERSONNES.resp, 'Ouverture du port 443', '?etape=ANALYSE')
    fireEvent.click(screen.getByRole('button', { name: 'Réaffecter l’étape' }))
    const d = await screen.findByRole('dialog', { name: 'Réaffecter l’étape' })
    const choix = within(d).getByLabelText(/^Nouvel exécutant/) as HTMLSelectElement
    await waitFor(() => expect(choix.options.length).toBeGreaterThan(3))
    fireEvent.change(choix, { target: { value: 'u-res' } })
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Idriss assure l’analyse cette semaine' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Réaffecter l’étape' }))
    expect(await screen.findByText(/Étape réaffectée/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Suspendre' }))
    await confirmer('Suspendre', 'Gel des changements de fin de mois')
    expect(await screen.findByText(/Instance suspendue : aucun acte/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Reprendre' }))
    await confirmer('Reprendre')
    await waitFor(() => expect(screen.queryByText(/Instance suspendue : aucun acte/)).toBeNull())
    fireEvent.click(screen.getByRole('button', { name: 'Annuler l’instance' }))
    await confirmer('Annuler l’instance', 'Demande abandonnée par le participant')
    expect(await screen.findByText('Annulée', { selector: '.gm-badge, span' })).toBeTruthy()
    expect((await s.processus.instance(i.id)).etat).toBe('ANNULEE')
  })

  it('membre du pôle : prise de l’étape, champs, livrable déposé par morceaux, fin refusée puis soumise ; validation et renvoi par le chef', async () => {
    const magasin = magasinDeuxPorteurs()
    const s = services(PERSONNES.membre, magasin)
    const i = await ouvrirInstance(s, PERSONNES.membre, 'Ouverture du port 443', '?etape=ANALYSE')
    fireEvent.click(screen.getByRole('button', { name: 'Prendre l’étape' }))
    expect(await screen.findByRole('button', { name: 'Terminer l’étape' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Terminer l’étape' }))
    await confirmer('Terminer l’étape')
    expect(await screen.findByText('Des livrables obligatoires manquent :')).toBeTruthy()
    const panneau = screen.getByRole('complementary', { name: /Analyse de sécurité du flux/ })
    fireEvent.change(panneau.querySelector('input[type="file"]')!, { target: { files: [new File(['note'], 'note-analyse.pdf', { type: 'application/pdf' })] } })
    fireEvent.click(await within(panneau).findByRole('button', { name: 'Envoyer NOTE_ANALYSE' }))
    expect(await screen.findByText('Livrable note-analyse.pdf déposé.', {}, { timeout: 4000 })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Terminer l’étape' }))
    await confirmer('Terminer l’étape')
    await waitFor(async () => expect((await s.processus.instance(i.id)).etapes[1].etat).toBe('A_VALIDER'))
    cleanup()
    // Le chef renvoie puis valide (même organisation simulée).
    const chef = services(PERSONNES.resp, magasin)
    await ouvrirInstance(chef, PERSONNES.resp, 'Ouverture du port 443', '?etape=ANALYSE')
    fireEvent.click(screen.getByRole('button', { name: 'Renvoyer' }))
    await confirmer('Renvoyer', 'Préciser les règles de retour')
    expect(await screen.findByText('1 renvoi')).toBeTruthy()
    expect((await chef.processus.instance(i.id)).etapes[1].etat).toBe('ACTIVE')
    await s.processus.terminer(i.id, 'ANALYSE')
    cleanup()
    await ouvrirInstance(chef, PERSONNES.resp, 'Ouverture du port 443', '?etape=ANALYSE')
    fireEvent.click(screen.getByRole('button', { name: 'Valider l’étape' }))
    await confirmer('Valider l’étape')
    await waitFor(async () => expect((await chef.processus.instance(i.id)).etapes[2].etat).toBe('ACTIVE'))
  })

  it('exécutant : champs d’une étape (contrôle, enregistrement) ; refus restitué ; instance inconnue', async () => {
    const s = services(PERSONNES.membre)
    const l = (await s.processus.lister({ etat: 'PUBLIEE' })).find((p) => p.code === 'OUVERTURE_FLUX_RESEAU')!
    // D-99 : le lanceur ne se désigne pas lui-même ; le second porteur du profil réseau est désigné.
    const i = await s.processus.lancer(l.id, { intitule: 'Essai des champs', champs: {}, designations: { MISE_EN_OEUVRE: 'u-res-bis' } })
    rendrePage(<InstancePage />, { chemin: `/des/processus/instances/${i.id}`, motif: '/des/processus/instances/:id', services: s, session: session(PERSONNES.membre) })
    const panneau = await screen.findByRole('complementary', { name: /Demande d’ouverture du flux/ })
    expect((within(panneau).getByRole('button', { name: 'Enregistrer les champs' }) as HTMLButtonElement).disabled).toBe(true)
    fireEvent.change(within(panneau).getByLabelText(/^Port/), { target: { value: 'x' } })
    fireEvent.click(within(panneau).getByRole('button', { name: 'Enregistrer les champs' }))
    expect(within(panneau).getByText('Nombre attendu.')).toBeTruthy()
    fireEvent.change(within(panneau).getByLabelText(/^Port/), { target: { value: '8443' } })
    fireEvent.change(within(panneau).getByLabelText(/^Adresse source/), { target: { value: '10.9.9.9' } })
    const saisir = vi.spyOn(s.processus, 'saisirChamps')
    fireEvent.click(within(panneau).getByRole('button', { name: 'Enregistrer les champs' }))
    await waitFor(() => expect(saisir).toHaveBeenCalledWith(i.id, 'DEMANDE', { SOURCE: '10.9.9.9', PORT: 8443 }))
    expect(await screen.findByText(/Champs enregistrés|Champs renseignés/)).toBeTruthy()
    vi.spyOn(s.processus, 'terminer').mockRejectedValueOnce(new ApiError(422, 'PIL_PROCESSUS_CHAMPS_MANQUANTS', 'x', 'cid-c', { champs: ['DESTINATION'] }))
    fireEvent.click(screen.getByRole('button', { name: 'Terminer l’étape' }))
    await confirmer('Terminer l’étape')
    // QUO-07 : le champ manquant est nommé par son libellé.
    expect(await screen.findByText('Champs à renseigner : Adresse de destination')).toBeTruthy()
    cleanup()
    // QUO-12 : instance inexistante expliquée, sans « Réessayer ».
    rendrePage(<InstancePage />, { chemin: '/des/processus/instances/INCONNUE', motif: '/des/processus/instances/:id', services: s, session: session(PERSONNES.membre) })
    expect(await screen.findByText('Demande introuvable')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Réessayer' })).toBeNull()
  })
})

describe('D-87 : désignation hors de l’unité', () => {
  it('lancement : un seul porteur du profil de remplacement, choisi d’office', async () => {
    // D-99 : lancé par la technicienne (non porteuse) ; l'ingénieur réseau, seul porteur, est choisi d'office.
    const s = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-collab', nom: 'Nadège Mvondo Assiga', profils: ['ROLE_COLLABORATEUR'] }, pouvoirs: magasinPouvoirs() })
    catalogue(PERSONNES.collab, s)
    fireEvent.click(await screen.findByRole('button', { name: 'Lancer le processus Ouverture d’un flux réseau' }))
    const d = await screen.findByRole('dialog', { name: 'Lancer le processus' })
    const choix = within(d).getByLabelText(/^Intervenant de « Mise en œuvre/) as HTMLSelectElement
    await waitFor(() => expect(choix.value).toBe('u-res'))
    expect(within(d).getByText(/Seul porteur du profil/)).toBeTruthy()
  })

  it('profil porté ailleurs : information publiée, étape offerte aux porteurs, candidats groupés à la réaffectation', async () => {
    const magasin = magasinPouvoirs()
    magasin.definirProfils('U-101', ['ROLE_INGENIEUR_HOMOLOGATION', 'ROLE_SNA'])
    const s = services(PERSONNES.chef, magasin)
    const p = await s.processus.creer({ code: 'AVIS_SNA', unite: 'POLE_RESEAU', libelle: 'Avis des normes', description: '', categorie: '', delaiCibleJours: null,
      etapes: [{ ...etapeVierge(1), code: 'TRAITEMENT', libelle: 'Traitement', intervenant: { mode: 'PROFIL', profil: 'ROLE_SNA' } }] })
    await s.processus.publier(p.id, {})
    await ouvrirDefinition(s, PERSONNES.chef, p.code)
    expect(screen.getByText('Profil porté hors de l’unité').closest('.gm-alert, [role="status"], [role="alert"], div')).toBeTruthy()
    cleanup()
    const i = await s.processus.lancer(p.id, { intitule: 'Avis sur le terminal', champs: {}, designations: {} })
    expect(i.etapes[0].aPrendre).toEqual({ unite: null, profil: 'ROLE_SNA' })
    await ouvrirInstance(s, PERSONNES.chef, 'Avis sur le terminal', `?etape=${p.etapes[0].code}`)
    expect(screen.getAllByText(/tout le GIMAC/).length).toBeGreaterThan(0)
    fireEvent.click(screen.getByRole('button', { name: 'Réaffecter l’étape' }))
    const d = await screen.findByRole('dialog', { name: 'Réaffecter l’étape' })
    const choix = within(d).getByLabelText(/^Nouvel exécutant/) as HTMLSelectElement
    await waitFor(() => expect([...choix.querySelectorAll('optgroup')].map((g) => g.label)).toContain('Ailleurs dans le GIMAC'))
    expect(within(d).getByText(/Aucun porteur du profil .* dans l’unité/)).toBeTruthy()
    fireEvent.change(choix, { target: { value: 'u-sna' } })
    fireEvent.change(within(d).getByLabelText(/Motif/), { target: { value: 'Dossier connu' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Réaffecter l’étape' }))
    await waitFor(async () => expect((await s.processus.instance(i.id)).etapes[0].executant?.id).toBe('u-sna'))
  })
})

describe('PDF d’une instance', () => {
  it('télécharge la demande et ses signatures avec son nom de fichier', async () => {
    const s = services(PERSONNES.membre)
    const i = await ouvrirInstance(s, PERSONNES.membre, 'Flux SFTP')
    const creation = vi.fn(() => 'blob:pdf-test')
    const revocation = vi.fn()
    Object.assign(URL, { createObjectURL: creation, revokeObjectURL: revocation })
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
    fireEvent.click(screen.getByRole('button', { name: 'Télécharger le PDF' }))
    await waitFor(() => expect(creation).toHaveBeenCalled())
    expect(clic).toHaveBeenCalled()
    expect(revocation).toHaveBeenCalled()
    expect(i.id).toBeTruthy()
  })
})

describe('D-87 : Mon activité', () => {
  it('« Mes étapes » dans la journée ; tâche d’étape à prendre avec le profil requis', async () => {
    const s = services(PERSONNES.membre)
    rendrePage(<MaJourneePage />, { chemin: '/des/activite', motif: '/des/activite', services: s, session: session(PERSONNES.membre) })
    const carte = await screen.findByRole('heading', { name: 'Mes étapes de processus' })
    expect(carte).toBeTruthy()
    expect(await screen.findByText('Ouverture du port 443 pour la supervision : Analyse de sécurité du flux')).toBeTruthy()
    cleanup()
    rendrePage(<TachesPage />, { chemin: '/des/taches?vue=unite', motif: '/des/taches', services: s, session: session(PERSONNES.membre) })
    expect(await screen.findByText('Pour le rôle ROLE_POLE_RESEAU')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /Ouverture du port 443 pour la supervision : Analyse/ }))
    const f = await screen.findByRole('dialog', { name: /Ouverture du port 443/ })
    expect(within(f).getByText('Processus d’unité')).toBeTruthy()
    // D-138 : tâche pour un rôle, le destinataire est le rôle (badge), sans ligne « Profil requis » redondante.
    expect(within(f).getByText('Destinataire')).toBeTruthy()
  })
})

describe('recette du travail quotidien (QUO-02, QUO-04, QUO-12)', () => {
  const periode = { debut: '2026-12-21', fin: '2026-12-31' }
  const congé = async (s: Services) => (await s.processus.lancables({})).groupes.flatMap((g) => g.processus).find((p) => p.code === 'DEMANDE_CONGE')!

  it('QUO-04 : la pièce justificative s’ouvre en aperçu depuis l’instance ; QUO-12 : un tiers reçoit un refus expliqué', async () => {
    const magasin = magasinPouvoirs()
    const s = services(PERSONNES.collab, magasin)
    const p = await congé(s)
    const depot = await s.processus.televerserPieceLancement(new File(['%PDF-1.4'], 'note.pdf', { type: 'application/pdf' }), p.id)
    const i = await s.processus.lancer(p.id, { intitule: 'Congé de décembre', champs: {}, designations: {}, formulaire: { PERIODE: periode, MOTIF: 'Congé annuel.', JUSTIFICATIF: { documentId: depot.documentId } } })
    rendrePage(<InstancePage />, { chemin: `/des/processus/instances/${i.id}`, motif: '/des/processus/instances/:id', services: s, session: session(PERSONNES.collab) })
    const lancement = await screen.findByTestId('valeurs-lancement')
    fireEvent.click(within(lancement).getByRole('button', { name: 'Ouvrir note.pdf' }))
    expect(await screen.findByRole('dialog', { name: /note\.pdf/ })).toBeTruthy()
    cleanup()
    // Le responsable du pôle système ne lit pas la demande : refus expliqué, sans « Réessayer ».
    const tiers = { id: 'u-sys', nom: 'Chantal Ngo Bassong', role: 'ROLE_POLE_SYSTEME' } as unknown as Qui
    rendrePage(<InstancePage />, { chemin: `/des/processus/instances/${i.id}`, motif: '/des/processus/instances/:id', services: services(tiers, magasin), session: session(tiers) })
    expect(await screen.findByText('Vous ne pouvez pas consulter cette demande')).toBeTruthy()
    expect(screen.queryByText('Instance introuvable.')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Réessayer' })).toBeNull()
  })

  it('QUO-02 : la réaffectation ne propose jamais la demandeuse et n’annonce pas un faux « seul porteur »', async () => {
    const magasin = magasinPouvoirs()
    const s = services(PERSONNES.collab, magasin)
    const i = await s.processus.lancer((await congé(s)).id, { intitule: 'Congé de décembre', champs: {}, designations: {}, formulaire: { PERIODE: periode, MOTIF: 'Congé annuel.' } })
    const dg = { id: 'u-dg', nom: 'Jean-Baptiste Ella Mintsa', role: 'ROLE_DG' } as unknown as Qui
    rendrePage(<InstancePage />, { chemin: `/des/processus/instances/${i.id}?etape=ANALYSE`, motif: '/des/processus/instances/:id', services: services(dg, magasin), session: session(dg) })
    fireEvent.click(await screen.findByRole('button', { name: 'Réaffecter l’étape' }))
    const d = await screen.findByRole('dialog', { name: /Réaffecter l’étape/ })
    const choix = within(d).getByLabelText(/^Nouvel exécutant/) as HTMLSelectElement
    // D-138 : l'étape est adressée au rôle (aucun exécutant figé) ; jamais la demandeuse, jamais un faux « seul porteur ».
    await waitFor(() => expect(choix.options.length).toBeGreaterThan(1))
    expect([...choix.options].map((o) => o.value)).not.toContain('u-collab')
  })
})

describe('recette du travail quotidien (QUO-06)', () => {
  it('code de livrable proposé d’office unique dans tout le processus', () => {
    expect(codeLivrableLibre([])).toBe('LIVRABLE_1')
    // Livrable LIVRABLE_1 déjà dans une autre étape : la nouvelle étape reçoit LIVRABLE_2, jamais un doublon.
    expect(codeLivrableLibre(['LIVRABLE_1'])).toBe('LIVRABLE_2')
    expect(codeLivrableLibre(['LIVRABLE_2', 'BON_REMISE'])).toBe('LIVRABLE_1')
  })
})
