import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { useLocation } from 'react-router'
import { ApiError } from '@gimac/socle'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import { magasinPouvoirs } from '../../../services/simule/pouvoirs'
import { PROFILS } from '../../../permissions'
import { ECRANS_DES } from '../../../fonctionnalites'
import { FONCTIONNALITES_INITIALES } from '../../../services/simule/fonctionnalites'
import LancerDemandePage from './LancerDemandePage'
import MesDemandesPage from './MesDemandesPage'
import InstancePage from './InstancePage'
import ModelisateurProcessusPage from './ModelisateurProcessusPage'
import { codeDepuisLibelle } from './ModaleLancement'
import { MesEtapes } from './MesEtapes'
import { GardePermission } from '../../../catalogue'
import { erreursFormulaire, valeursEnvoyees } from './FormulaireLancement'
import type { ChampFormulaire } from '../../../modele/processus'

// D-98 (docs/lot1/lancement-processus.md §3, §4) : « Lancer une demande » (catalogue par service destinataire, formulaire
// de lancement, erreurs du service champ par champ), « Mes demandes » (étape en cours, qui la tient, état de chaque étape),
// suivi de l'instance par l'initiateur (valeurs fournies), réglage du lancement dans le modélisateur ; entrées du catalogue.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

const SNA = { id: 'u-sna', nom: 'Rachel Biyong Mengue', role: 'ROLE_SNA' }
const RESEAU = { id: 'u-res', nom: 'Idriss Mahamat Saleh', role: 'ROLE_POLE_RESEAU' }
const RESP = { id: 'u-resp-info', nom: 'Gaston Ndzana Ebode', role: 'ROLE_RESPONSABLE_SERVICE' }
type Qui = typeof SNA
const services = (q: Qui, magasin = magasinPouvoirs()) => servicesSimules({ latenceMs: 0, utilisateur: { id: q.id, nom: q.nom, profils: [q.role] }, pouvoirs: magasin })
const session = (q: Qui) => ({ ...sessionTest(PROFILS[q.role], [q.role]), sujet: q.id, nom: q.nom })
function Ici() {
  const l = useLocation()
  return <span data-testid="ici">{l.pathname + l.search}</span>
}
const AUTRES = [
  { motif: '/des/demandes/:id', element: <><InstancePage /><Ici /></> },
  { motif: '/des/demandes', element: <><MesDemandesPage /><Ici /></> },
]
const saisir = (d: HTMLElement, libelle: RegExp, valeur: string) => fireEvent.change(within(d).getByLabelText(libelle), { target: { value: valeur } })

describe('D-98 : catalogue des fonctionnalités', () => {
  it('« Lancer une demande » et « Mes demandes » : module processus, ouvertes à PIL_ACTIVITY_OWN, avec leur écran', () => {
    for (const cle of ['lancerDemande', 'mesDemandes']) {
      const f = FONCTIONNALITES_INITIALES.find((x) => x.cle === cle)!
      expect(f).toMatchObject({ application: 'des', module: 'processus', groupe: 'processus', visible: true, permissions: { tous: [], unDe: ['PIL_ACTIVITY_OWN'] } })
      expect(ECRANS_DES[cle]).toBeDefined()
    }
  })
})

describe('D-98 : formulaire de lancement (règles de forme)', () => {
  const champs: ChampFormulaire[] = [
    { code: 'P', libelle: 'Période', type: 'PERIODE', choix: [], obligatoire: true, aide: null, typesPieces: [] },
    { code: 'N', libelle: 'Nombre', type: 'NOMBRE', choix: [], obligatoire: false, aide: null, typesPieces: [] },
    { code: 'C', libelle: 'J’atteste', type: 'CASE', choix: [], obligatoire: true, aide: null, typesPieces: [] },
    { code: 'F', libelle: 'Pièce', type: 'PIECE_JOINTE', choix: [], obligatoire: false, aide: null, typesPieces: ['application/pdf'] },
    { code: 'D', libelle: 'Date', type: 'DATE', choix: [], obligatoire: false, aide: null, typesPieces: [] },
  ]
  const t = (k: string) => k
  it('erreurs annoncées avant l’envoi, valeurs converties pour le service', () => {
    const e = erreursFormulaire(champs, { P: { debut: '31/12/2026', fin: '21/12/2026' }, N: 'douze', C: false, F: new File(['x'], 'a.txt', { type: 'text/plain' }), D: '32/13/2026' }, t)
    expect(e).toEqual({ P: 'dem.erreur.periodeOrdre', N: 'proc.erreur.nombre', C: 'dem.erreur.cocher', F: 'dem.erreur.typePiece', D: 'proc.erreur.date' })
    expect(valeursEnvoyees(champs, { P: { debut: '21/12/2026', fin: '31/12/2026' }, N: '12,5', C: true, F: new File(['x'], 'a.pdf'), D: '' }, { F: 'DOC-1' }))
      .toEqual({ P: { debut: '2026-12-21', fin: '2026-12-31' }, N: 12.5, C: true, F: { documentId: 'DOC-1' } })
    expect(codeDepuisLibelle('Adresse IP demandée')).toBe('ADRESSE_IP_DEMANDEE')
  })
})

describe('D-98 : lancer une demande, la suivre', () => {
  it('un collaborateur du SNA lance une demande au pôle réseau depuis le catalogue groupé par service, puis la retrouve dans « Mes demandes »', async () => {
    const magasin = magasinPouvoirs()
    const s = services(SNA, magasin)
    rendrePage(<><LancerDemandePage /><Ici /></>, { chemin: '/des/demandes/lancer', motif: '/des/demandes/lancer', services: s, session: session(SNA), autres: AUTRES })
    expect(await screen.findByRole('heading', { level: 2, name: 'Pôle réseau' })).toBeTruthy()
    expect(screen.getByRole('heading', { level: 2, name: 'Service des ressources humaines' })).toBeTruthy()
    expect(screen.queryByText('Ouverture d’un flux réseau')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Lancer « Demande de changement d’adresse IP »' }))
    const d = await screen.findByRole('dialog', { name: 'Nouvelle demande' })
    expect(within(d).getByText(/adressée à : Pôle réseau/)).toBeTruthy()
    // Contrôle de forme : objet et champs obligatoires.
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    expect((await within(d).findAllByText('Champ obligatoire.')).length).toBeGreaterThan(0)
    saisir(d, /^Objet de la demande/, 'Poste d’accueil du rez-de-chaussée')
    saisir(d, /^Équipement/, 'PC-SNA-014')
    saisir(d, /^Adresse IP actuelle/, '10.20.4.15')
    saisir(d, /^Adresse IP demandée/, '10.20.6.40')
    saisir(d, /^Justification/, 'Changement de bureau.')
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    await waitFor(() => expect(screen.getByTestId('ici').textContent).toMatch(/^\/des\/demandes\/INS-/))
    expect(await screen.findByRole('heading', { level: 1, name: 'Poste d’accueil du rez-de-chaussée' })).toBeTruthy()
    const valeurs = screen.getByTestId('valeurs-lancement')
    expect(within(valeurs).getByText('10.20.6.40')).toBeTruthy()
    // L'initiateur revient à « Mes demandes » : étape en cours et qui la tient.
    fireEvent.click(screen.getByRole('button', { name: 'Mes demandes' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Mes demandes' })).toBeTruthy()
    // D-138 : étape adressée au rôle, « à prendre » par le profil du pôle.
    expect(await screen.findByText(/à prendre : /)).toBeTruthy()
    expect(screen.getByRole('list', { name: 'Étapes de la demande : 0 terminée(s) sur 4' })).toBeTruthy()
  })

  it('erreurs du service champ par champ (422 PIL_PROCESSUS_FORMULAIRE_INVALIDE) sous chaque champ', async () => {
    const s = services(SNA)
    vi.spyOn(s.processus, 'lancer').mockRejectedValue(new ApiError(422, 'PIL_PROCESSUS_FORMULAIRE_INVALIDE', 'À corriger', 'corr-1', { champs: { 'formulaire.ADRESSE_DEMANDEE': 'Adresse déjà attribuée' } }))
    rendrePage(<><LancerDemandePage /><Ici /></>, { chemin: '/des/demandes/lancer', motif: '/des/demandes/lancer', services: s, session: session(SNA), autres: AUTRES })
    fireEvent.click(await screen.findByRole('button', { name: 'Lancer « Demande de changement d’adresse IP »' }))
    const d = await screen.findByRole('dialog', { name: 'Nouvelle demande' })
    saisir(d, /^Objet de la demande/, 'Poste')
    saisir(d, /^Équipement/, 'PC')
    saisir(d, /^Adresse IP actuelle/, '10.0.0.1')
    saisir(d, /^Adresse IP demandée/, '10.0.0.2')
    saisir(d, /^Justification/, 'Test')
    fireEvent.click(within(d).getByRole('button', { name: 'Envoyer la demande' }))
    expect(await within(d).findByText('Adresse déjà attribuée')).toBeTruthy()
    expect(within(d).getByText('Le formulaire de lancement est à corriger.')).toBeTruthy()
  })

  it('« Mes demandes » suit l’avancement : l’étape passe au chef de pôle, puis la demande est close', async () => {
    const magasin = magasinPouvoirs()
    const s = services(SNA, magasin)
    const ip = (await s.processus.lancables({})).groupes[0].processus[0]
    const i = await s.processus.lancer(ip.id, { intitule: 'Imprimante du 2e étage', champs: {}, designations: {}, formulaire: { EQUIPEMENT: 'IMP-2', ADRESSE_ACTUELLE: '10.0.0.5', ADRESSE_DEMANDEE: '10.0.0.6', JUSTIFICATION: 'Plan d’adressage.' } })
    const res = services(RESEAU, magasin).processus
    await res.saisirChamps(i.id, 'ANALYSE', { ADRESSE_DISPONIBLE: 'Oui' })
    await res.terminer(i.id, 'ANALYSE')
    rendrePage(<MesDemandesPage />, { chemin: '/des/demandes', motif: '/des/demandes', services: s, session: session(SNA), autres: AUTRES })
    expect(await screen.findByText(/tenue par Gaston Ndzana Ebode/)).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Ouvrir l’étape Analyse technique (Terminée)' })).toBeTruthy()
    const chef = services(RESP, magasin).processus
    await chef.saisirChamps(i.id, 'VALIDATION', { DECISION: 'Accordée' })
    await chef.terminer(i.id, 'VALIDATION')
    await res.saisirChamps(i.id, 'EXECUTION', { ADRESSE_ATTRIBUEE: '10.0.0.6' })
    await res.terminer(i.id, 'EXECUTION')
    await res.terminer(i.id, 'CLOTURE')
    fireEvent.change(screen.getByLabelText('État'), { target: { value: 'CLOSE' } })
    expect(await screen.findByText(/Demande close le/)).toBeTruthy()
  })
})

describe('D-98 : réglage du lancement dans le modélisateur', () => {
  it('le chef du service ouvre la demande à tout le GIMAC et ajoute un champ au formulaire ; le PUT porte initiateurs et formulaire', async () => {
    const s = services(RESP)
    const modifier = vi.spyOn(s.processus, 'modifier')
    const brouillon = (await s.processus.lister({})).find((p) => p.etat === 'BROUILLON') ?? (await s.processus.creer({ code: 'DEMANDE_BADGE', unite: 'POLE_RESEAU', libelle: 'Demande de badge', description: '', categorie: '', delaiCibleJours: null, etapes: [] }))
    rendrePage(<ModelisateurProcessusPage />, { chemin: `/des/processus/${brouillon.id}`, motif: '/des/processus/:id', services: s, session: session(RESP) })
    await screen.findByRole('heading', { level: 1, name: brouillon.libelle })
    expect(await screen.findByText('Membres et responsables de l’unité Pôle réseau')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Qui peut voir et lancer' }))
    const d = await screen.findByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
    fireEvent.click(within(d).getByLabelText('Tout le GIMAC'))
    // D-100 : la règle par défaut s'intitule « Membres et responsables de l'unité ».
    expect((within(d).getByLabelText('Membres et responsables de l’unité') as HTMLInputElement).checked).toBe(true)
    fireEvent.click(within(d).getByRole('button', { name: 'Ajouter un champ' }))
    saisir(d, /^Libellé/, 'Numéro de bureau')
    expect((within(d).getByLabelText(/^Code/) as HTMLInputElement).value).toBe('NUMERO_DE_BUREAU')
    fireEvent.change(within(d).getByLabelText(/^Type/), { target: { value: 'LISTE' } })
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(await within(d).findByText('Indiquez au moins une valeur.')).toBeTruthy()
    saisir(d, /^Valeurs de la liste/, 'B12, B14')
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })).toBeNull())
    const corps = modifier.mock.calls.at(-1)![1]
    expect(corps.initiateurs?.regles).toEqual(['MEMBRES_UNITE', 'TOUT_LE_GIMAC'])
    expect(corps.formulaire?.champs).toEqual([{ code: 'NUMERO_DE_BUREAU', libelle: 'Numéro de bureau', type: 'LISTE', choix: ['B12', 'B14'], obligatoire: false, aide: null, typesPieces: [] }])
    expect(await screen.findByText(/Membres et responsables de l’unité Pôle réseau ; Tout le GIMAC/)).toBeTruthy()
  })
})


describe('D-98 : lien profond et « Mes étapes »', () => {
  const masque = { etat: 'pret' as const, fonctionnalites: FONCTIONNALITES_INITIALES.map((f) => (f.cle === 'processus' ? { ...f, visible: false } : f)) }

  it('l’instance s’ouvre depuis le lien d’une notification même si « Processus de l’unité » est masquée ; refus sans PIL_ACTIVITY_OWN', async () => {
    const magasin = magasinPouvoirs()
    const s = services(SNA, magasin)
    const ip = (await s.processus.lancables({})).groupes[0].processus[0]
    const i = await s.processus.lancer(ip.id, { intitule: 'Poste du hall', champs: {}, designations: {}, formulaire: { EQUIPEMENT: 'PC', ADRESSE_ACTUELLE: '10.0.0.1', ADRESSE_DEMANDEE: '10.0.0.2', JUSTIFICATION: 'Test' } })
    const page = <GardePermission permission="PIL_ACTIVITY_OWN"><InstancePage /></GardePermission>
    rendrePage(page, { chemin: `/des/processus/instances/${i.id}?etape=ANALYSE`, motif: '/des/processus/instances/:id', services: services(RESEAU, magasin), session: session(RESEAU), catalogue: masque })
    const panneau = await screen.findByRole('complementary', { name: /Analyse technique/ })
    expect(within(panneau).getByRole('button', { name: 'Terminer l’étape' })).toBeTruthy()
    // Retour vers « Processus de l'unité » absent : la fonctionnalité est masquée.
    expect(screen.queryByRole('button', { name: 'Instances' })).toBeNull()
    cleanup()
    rendrePage(page, { chemin: `/des/processus/instances/${i.id}`, motif: '/des/processus/instances/:id', services: s, session: { ...session(SNA), permissions: ['PIL_TEAM_READ'] }, catalogue: masque })
    expect(screen.getByText('Accès refusé')).toBeTruthy()
  })

  it('« Mes étapes » ne garde que les étapes qui attendent ma décision (attendMoi servi par le service)', async () => {
    const magasin = magasinPouvoirs()
    const sna = services(SNA, magasin)
    const ip = (await sna.processus.lancables({})).groupes[0].processus[0]
    const i = await sna.processus.lancer(ip.id, { intitule: 'Poste du hall', champs: {}, designations: {}, formulaire: { EQUIPEMENT: 'PC', ADRESSE_ACTUELLE: '10.0.0.1', ADRESSE_DEMANDEE: '10.0.0.2', JUSTIFICATION: 'Test' } })
    const res = services(RESEAU, magasin)
    const avant = (await res.processus.instances({ vue: 'miennes', etat: 'EN_COURS', taille: 50 })).contenu.find((x) => x.id === i.id)!
    expect(avant.etapesEnCours.map((e) => [e.code, e.attendMoi, e.moi?.executer])).toEqual([['ANALYSE', true, true]])
    await res.processus.saisirChamps(i.id, 'ANALYSE', { ADRESSE_DISPONIBLE: 'Oui' })
    await res.processus.terminer(i.id, 'ANALYSE')
    const apres = (await res.processus.instances({ vue: 'miennes', etat: 'EN_COURS', taille: 50 })).contenu.find((x) => x.id === i.id)!
    expect(apres.etapesEnCours.map((e) => [e.code, e.attendMoi])).toEqual([['VALIDATION', false]])
    rendrePage(<MesEtapes />, { chemin: '/des/activite', motif: '/des/activite', services: res, session: session(RESEAU) })
    const liste = await screen.findByRole('list', { name: 'Mes étapes de processus' })
    expect(within(liste).queryByText('Poste du hall : Validation du chef de pôle')).toBeNull()
    cleanup()
    rendrePage(<MesEtapes />, { chemin: '/des/activite', motif: '/des/activite', services: services(RESP, magasin), session: session(RESP) })
    expect(await screen.findByText('Poste du hall : Validation du chef de pôle')).toBeTruthy()
  })
})
