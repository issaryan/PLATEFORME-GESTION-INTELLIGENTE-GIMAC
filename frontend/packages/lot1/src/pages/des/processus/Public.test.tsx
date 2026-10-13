import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { useLocation } from 'react-router'
import { ApiError } from '@gimac/socle'
import { rendrePage, sessionTest } from '../../../test/rendu'
import { servicesSimules } from '../../../services/simule'
import { magasinPouvoirs, type MagasinPouvoirs } from '../../../services/simule/pouvoirs'
import { publicsRestreintsDemo } from '../../../services/simule/processus'
import { PROFILS } from '../../../permissions'
import { INITIATEURS_DEFAUT, type Initiateurs } from '../../../modele/processus'
import LancerDemandePage from './LancerDemandePage'
import ModelisateurProcessusPage from './ModelisateurProcessusPage'
import ProcessusPage from './ProcessusPage'
import { ModaleLancement } from './ModaleLancement'
import { PROFILS_INTERVENANTS } from './communs'

// D-100 (docs/lot1/lancement-processus.md §10) : le public d'un processus, défini par son créateur, fait seul foi pour
// « Lancer une demande » et le lancement (services simulés alignés sur MS-PILOTAGE : aucun passe-droit d'administration,
// MEMBRES_UNITE avec les responsables, règle PERSONNES, anomalies et textes de refus) ; réglage « Qui peut voir et lancer »
// dans le modélisateur (personnes choisies dans l'annuaire, puces retirables), résumé « Visible et lançable par » dans le
// détail et la liste, annonce du public par défaut à la création.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
})

type Qui = { id: string; nom: string; role: string }
const ADMIN: Qui = { id: 'u-super-admin', nom: 'Serge Mouelle Kombi', role: 'ROLE_SUPER_ADMIN' }
const HOMOLOGATION: Qui = { id: 'U-101', nom: 'Joël Ekotto Mballa', role: 'ROLE_INGENIEUR_HOMOLOGATION' }
const SNA: Qui = { id: 'u-sna', nom: 'Rachel Biyong Mengue', role: 'ROLE_SNA' }
const RESEAU: Qui = { id: 'u-res', nom: 'Idriss Mahamat Saleh', role: 'ROLE_POLE_RESEAU' }
const RESP: Qui = { id: 'u-resp-info', nom: 'Gaston Ndzana Ebode', role: 'ROLE_RESPONSABLE_SERVICE' }
const CHEF_DES: Qui = { id: 'U-001', nom: 'Paul Mbarga Atangana', role: 'ROLE_CHEF_DES' }
const CHEF_DDRM: Qui = { id: 'U-020', nom: 'Bertrand Nguema Obiang', role: 'ROLE_CHEF_DDRM' }
const CONFORMITE: Qui = { id: 'u-conf', nom: 'Mireille Obame Nze', role: 'ROLE_CONFORMITE' }
const RSSI: Qui = { id: 'u-rssi', nom: 'Hervé Kamga Tchoupo', role: 'ROLE_RSSI' }

const services = (q: Qui, magasin: MagasinPouvoirs) => servicesSimules({ latenceMs: 0, utilisateur: { id: q.id, nom: q.nom, profils: [q.role] }, pouvoirs: magasin })
const session = (q: Qui) => ({ ...sessionTest(PROFILS[q.role], [q.role]), sujet: q.id, nom: q.nom })
const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: { status: number; code: string }) => `${e.status} ${e.code}`)
const codes = async (q: Qui, magasin: MagasinPouvoirs) => (await services(q, magasin).processus.lancables({})).groupes.flatMap((g) => g.processus.map((p) => p.code))
function Ici() {
  const l = useLocation()
  return <span data-testid="ici">{l.pathname + l.search}</span>
}

describe('D-100 : jeu de démonstration', () => {
  it('deux processus au public restreint : personnes désignées (pôle système), profils (conformité)', () => {
    const [vm, maj] = publicsRestreintsDemo()
    expect([vm.code, vm.unite, vm.initiateurs.regles, vm.initiateurs.personnes]).toEqual(['CREATION_MACHINE_VIRTUELLE', 'POLE_SYSTEME', ['PERSONNES'], ['u-res', 'u-sna']])
    expect([maj.code, maj.unite, maj.initiateurs.regles, maj.initiateurs.profils]).toEqual(['MISE_A_JOUR_PLATEFORME', 'CONFORMITE', ['PROFILS'], ['ROLE_CONFORMITE', 'ROLE_RSSI']])
    expect(INITIATEURS_DEFAUT.personnes).toEqual([])
    // Profils proposés : tous les profils internes du domaine, sans le profil externe d'institution.
    expect(PROFILS_INTERVENANTS).toContain('ROLE_SUPER_ADMIN')
    expect(PROFILS_INTERVENANTS).toContain('ROLE_ADMIN_SYSTEME')
    expect(PROFILS_INTERVENANTS).not.toContain('ROLE_CORRESPONDANT')
  })
})

describe('D-100 : public strict (services simulés, comme MS-PILOTAGE)', () => {
  it('un administrateur (REF_METIER_ADMIN) ne voit ni ne lance les processus réservés ; il les lit toujours', async () => {
    const magasin = magasinPouvoirs()
    for (const admin of [ADMIN, HOMOLOGATION]) {
      const l = await codes(admin, magasin)
      expect(l).not.toContain('CREATION_MACHINE_VIRTUELLE')
      expect(l).not.toContain('MISE_A_JOUR_PLATEFORME')
      expect(l).not.toContain('OUVERTURE_FLUX_RESEAU')
      expect(l).toContain('DEMANDE_CONGE')
    }
    const s = services(ADMIN, magasin).processus
    const vm = (await s.lister({ etat: 'PUBLIEE' })).find((p) => p.code === 'CREATION_MACHINE_VIRTUELLE')!
    expect(vm.droits.lancer).toBe(false)
    const lu = await s.lire(vm.id)
    expect(lu.initiateurs?.personnesDetail).toEqual([{ id: 'u-res', nomAffiche: 'Idriss Mahamat Saleh' }, { id: 'u-sna', nomAffiche: 'Rachel Biyong Mengue' }])
    const refus = await s.lancer(vm.id, { intitule: 'VM', champs: {}, designations: {}, formulaire: {} }).catch((e: ApiError) => e)
    expect(refus).toBeInstanceOf(ApiError)
    expect((refus as ApiError).code).toBe('PIL_PROCESSUS_NON_INITIATEUR')
    expect((refus as ApiError).message).toBe('Ce processus ne peut être lancé que par les 2 personnes désignées par son créateur')
    expect((refus as ApiError).details).toMatchObject({ regles: ['PERSONNES'], personnes: ['u-res', 'u-sna'] })
  })

  it('les personnes désignées voient et lancent ; les porteurs des profils désignés aussi ; les autres non', async () => {
    const magasin = magasinPouvoirs()
    expect(await codes(SNA, magasin)).toContain('CREATION_MACHINE_VIRTUELLE')
    expect(await codes(RESEAU, magasin)).toContain('CREATION_MACHINE_VIRTUELLE')
    expect(await codes(RESP, magasin)).not.toContain('CREATION_MACHINE_VIRTUELLE')
    for (const q of [CONFORMITE, RSSI]) expect(await codes(q, magasin)).toContain('MISE_A_JOUR_PLATEFORME')
    expect(await codes(SNA, magasin)).not.toContain('MISE_A_JOUR_PLATEFORME')
    const s = services(SNA, magasin).processus
    const vm = (await s.lancables({ q: 'machine' })).groupes[0].processus[0]
    expect((await s.lire(vm.id)).droits.lancer).toBe(true)
    // D-106 (§12) : au seul titre du public, la version publiée seule, sans historique de la définition ni contrôle.
    const lue = await s.lire(vm.id)
    expect(lue.versions.map((v) => v.id)).toEqual([vm.id])
    expect(lue.historique).toEqual([])
    expect(lue.droits).toMatchObject({ modifier: false, publier: false, retirer: false, nouvelleVersion: false, modifierPublic: false })
    expect(await code(s.controler(vm.id))).toBe('403 PIL_PROCESSUS_INTERDIT')
    const i = await s.lancer(vm.id, { intitule: 'Serveur de recette', champs: {}, designations: {}, formulaire: { SYSTEME: 'Linux', PROCESSEURS: 4, MEMOIRE: 16 } })
    expect(i.lanceur.id).toBe('u-sna')
    // Désignée mais partie : plus rien.
    magasin.organisation.find((m) => m.id === 'u-res')!.actif = false
    expect(await codes(RESEAU, magasin)).not.toContain('CREATION_MACHINE_VIRTUELLE')
  })

  it('MEMBRES_UNITE : membres et responsables (unité dirigée ou parente dirigée), pas les autres chefs', async () => {
    const magasin = magasinPouvoirs()
    for (const q of [RESEAU, RESP, CHEF_DES]) expect(await codes(q, magasin)).toContain('OUVERTURE_FLUX_RESEAU')
    expect(await codes(CHEF_DDRM, magasin)).not.toContain('OUVERTURE_FLUX_RESEAU')
    const flux = (await services(RESP, magasin).processus.lancables({ q: 'flux' })).groupes[0].processus[0]
    const refus = await services(CHEF_DDRM, magasin).processus.lancer(flux.id, { intitule: 'x', champs: {}, designations: {} }).catch((e: ApiError) => e)
    expect((refus as ApiError).message).toBe('Ce processus ne peut être lancé que par les membres et les responsables de l\'unité POLE_RESEAU')
  })

  it('règle PERSONNES : saisie contrôlée (400), anomalies INITIATEURS_INCOMPLETS et PERSONNE_INCONNUE à la publication', async () => {
    const magasin = magasinPouvoirs()
    const chef = services(RESP, magasin).processus
    const base = { code: 'DEMANDE_PUBLIC', unite: 'POLE_RESEAU', libelle: 'Demande à public', description: '', categorie: '', delaiCibleJours: null, etapes: publicsRestreintsDemo()[0].etapes.map((e) => ({ ...e, intervenant: { mode: 'PROFIL' as const, profil: 'ROLE_POLE_RESEAU' } })) }
    const avec = (i: Partial<Initiateurs>) => ({ ...base, initiateurs: { ...INITIATEURS_DEFAUT, ...i } })
    expect(await code(chef.creer(avec({ regles: ['PERSONNES'], personnes: ['u-res', ' '] })))).toBe('400 PIL_REQUETE_INVALIDE')
    const trop = await chef.creer(avec({ regles: ['PERSONNES'], personnes: Array.from({ length: 51 }, (_, n) => `compte-${n}`) })).catch((e: ApiError) => e)
    expect((trop as ApiError).details).toMatchObject({ champ: 'initiateurs.personnes' })
    const p = await chef.creer(avec({ regles: ['PERSONNES'] }))
    const anomalies = async () => (await chef.controler(p.id)).anomalies.filter((a) => a.etape === null).map((a) => [a.code, a.element, a.gravite])
    expect(await anomalies()).toEqual([['INITIATEURS_INCOMPLETS', 'initiateurs.personnes', 'BLOQUANTE']])
    magasin.organisation.find((m) => m.id === 'u-collab')!.actif = false
    const m = await chef.modifier(p.id, avec({ regles: ['PERSONNES'], personnes: ['u-res', 'u-res', 'u-collab', 'inconnu'] }))
    expect(m.initiateurs?.personnes).toEqual(['u-res', 'u-collab', 'inconnu'])
    expect(m.initiateurs?.personnesDetail?.map((d) => d.nomAffiche)).toEqual(['Idriss Mahamat Saleh', 'Nadège Mvondo Assiga', null])
    expect(await anomalies()).toEqual([['PERSONNE_INCONNUE', 'initiateurs.personnes', 'BLOQUANTE'], ['PERSONNE_INCONNUE', 'initiateurs.personnes', 'BLOQUANTE']])
    expect(await code(chef.publier(p.id, {}))).toBe('422 PIL_PROCESSUS_ANOMALIES')
    await chef.modifier(p.id, avec({ regles: ['PERSONNES'], personnes: ['u-res'] }))
    expect(await code(chef.publier(p.id, { leverAvertissements: true, motif: 'Public réglé' }))).toBe('ok')
  })
})

describe('D-100 : « Lancer une demande »', () => {
  it('l’administrateur n’y voit pas la création d’une machine virtuelle ; la personne désignée la voit, signalée comme ouverte à des personnes désignées', async () => {
    const magasin = magasinPouvoirs()
    rendrePage(<LancerDemandePage />, { chemin: '/des/demandes/lancer', motif: '/des/demandes/lancer', services: services(ADMIN, magasin), session: session(ADMIN) })
    expect(await screen.findByRole('heading', { level: 2, name: 'Service des ressources humaines' })).toBeTruthy()
    expect(screen.queryByText('Création d’une machine virtuelle')).toBeNull()
    expect(screen.queryByText('Mise à jour de la plateforme')).toBeNull()
    cleanup()
    rendrePage(<LancerDemandePage />, { chemin: '/des/demandes/lancer', motif: '/des/demandes/lancer', services: services(SNA, magasin), session: session(SNA) })
    expect(await screen.findByRole('button', { name: 'Lancer « Création d’une machine virtuelle »' })).toBeTruthy()
    expect(screen.getByText('Ouvert à des personnes désignées')).toBeTruthy()
  })
})

describe('D-100 : réglage « Qui peut voir et lancer » dans le modélisateur', () => {
  it('le chef choisit des personnes dans l’annuaire (puces retirables) ; le PUT porte la règle PERSONNES ; le résumé les nomme', async () => {
    const magasin = magasinPouvoirs()
    const s = services(RESP, magasin)
    const modifier = vi.spyOn(s.processus, 'modifier')
    const brouillon = await s.processus.creer({ code: 'DEMANDE_RESERVEE', unite: 'POLE_RESEAU', libelle: 'Demande réservée', description: '', categorie: '', delaiCibleJours: null, etapes: [] })
    rendrePage(<ModelisateurProcessusPage />, { chemin: `/des/processus/${brouillon.id}`, motif: '/des/processus/:id', services: s, session: session(RESP) })
    await screen.findByRole('heading', { level: 1, name: 'Demande réservée' })
    expect(await within(screen.getByTestId('public-processus')).findByText('Membres et responsables de l’unité Pôle réseau')).toBeTruthy()
    // Accès direct au réglage depuis le résumé.
    fireEvent.click(screen.getByRole('button', { name: 'Régler qui peut voir et lancer « Demande réservée »' }))
    const d = await screen.findByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
    expect(within(d).getByText(/Seules ces personnes voient le processus dans « Lancer une demande »/)).toBeTruthy()
    fireEvent.click(within(d).getByLabelText('Membres et responsables de l’unité'))
    fireEvent.click(within(d).getByLabelText('Personnes désignées'))
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    expect(await within(d).findByText('Choisissez au moins une personne.')).toBeTruthy()
    const zone = within(d).getByTestId('initiateurs-personnes')
    fireEvent.click(await within(zone).findByRole('button', { name: /Idriss Mahamat Saleh/ }))
    fireEvent.click(within(zone).getByRole('button', { name: /Rachel Biyong Mengue/ }))
    fireEvent.click(within(zone).getByRole('button', { name: /Chantal Ngo Bassong/ }))
    expect(within(zone).getByText('3 personnes choisies', { exact: false })).toBeTruthy()
    // Puce retirable : Chantal Ngo Bassong est retirée.
    fireEvent.click(within(zone).getByRole('button', { name: /Retirer Chantal Ngo Bassong/ }))
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer' }))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })).toBeNull())
    const corps = modifier.mock.calls.at(-1)![1]
    expect(corps.initiateurs).toMatchObject({ regles: ['PERSONNES'], personnes: ['u-res', 'u-sna'], unites: [], profils: [], permissions: [] })
    // Un autre enregistrement (propriétés) renvoie le public sans le détail des noms, vue du service.
    fireEvent.click(screen.getByRole('button', { name: 'Propriétés' }))
    const proprietes = await screen.findByRole('dialog', { name: 'Propriétés' })
    fireEvent.change(within(proprietes).getByLabelText(/^Catégorie/), { target: { value: 'Accès' } })
    fireEvent.click(within(proprietes).getByRole('button', { name: 'Enregistrer' }))
    await waitFor(() => expect(modifier.mock.calls.length).toBeGreaterThan(1))
    expect(modifier.mock.calls.at(-1)![1].initiateurs).not.toHaveProperty('personnesDetail')
    expect(modifier.mock.calls.at(-1)![1].initiateurs?.personnes).toEqual(['u-res', 'u-sna'])
    expect(await screen.findByText('Personnes désignées (Idriss Mahamat Saleh, Rachel Biyong Mengue)')).toBeTruthy()
  })

  it('lien direct ?reglage=public : le réglage s’ouvre ; création (?cree=1) : public par défaut annoncé, invitation à le régler', async () => {
    const magasin = magasinPouvoirs()
    const s = services(RESP, magasin)
    const brouillon = await s.processus.creer({ code: 'DEMANDE_NEUVE', unite: 'POLE_RESEAU', libelle: 'Demande neuve', description: '', categorie: '', delaiCibleJours: null, etapes: [] })
    rendrePage(<ModelisateurProcessusPage />, { chemin: `/des/processus/${brouillon.id}?reglage=public`, motif: '/des/processus/:id', services: s, session: session(RESP) })
    const d = await screen.findByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })).toBeNull())
    cleanup()
    rendrePage(<ModelisateurProcessusPage />, { chemin: `/des/processus/${brouillon.id}?cree=1`, motif: '/des/processus/:id', services: s, session: session(RESP) })
    expect(await screen.findByText('Processus créé')).toBeTruthy()
    expect(await screen.findByText(/Par défaut, il est visible et lançable par : Membres et responsables de l’unité Pôle réseau/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Régler qui peut voir et lancer' }))
    expect(await screen.findByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })).toBeTruthy()
  })

  it('version publiée : le public se consulte en lecture seule, personnes nommées ; résumé abrégé au-delà de trois personnes', async () => {
    const i: Initiateurs = {
      regles: ['PERSONNES', 'PROFILS'], unites: [], profils: ['ROLE_CONFORMITE'], permissions: [],
      personnes: ['a', 'b', 'c', 'd', 'e'], personnesDetail: ['Ana', 'Bob', 'Cyr', 'Dan', 'Eve'].map((n, k) => ({ id: 'abcde'[k], nomAffiche: n })),
    }
    rendrePage(<ModaleLancement initiateurs={i} lecture enCours={false} onFermer={() => undefined} onEnregistrer={() => undefined} />, { chemin: '/', motif: '/' })
    const d = await screen.findByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
    const liste = within(d).getByRole('list', { name: 'Personnes désignées' })
    expect(within(liste).getAllByRole('listitem').map((x) => x.textContent)).toEqual(['Ana', 'Bob', 'Cyr', 'Dan', 'Eve'])
    cleanup()
    const magasin = magasinPouvoirs()
    const s = services(RESP, magasin)
    const p = await s.processus.creer({ code: 'DEMANDE_LARGE', unite: 'POLE_RESEAU', libelle: 'Demande large', description: '', categorie: '', delaiCibleJours: null, etapes: [],
      initiateurs: { ...INITIATEURS_DEFAUT, regles: ['PERSONNES'], personnes: ['u-res', 'u-sna', 'u-sys', 'u-conf', 'u-rssi'] } })
    rendrePage(<ModelisateurProcessusPage />, { chemin: `/des/processus/${p.id}`, motif: '/des/processus/:id', services: s, session: session(RESP) })
    expect(await screen.findByText('Personnes désignées (Idriss Mahamat Saleh, Rachel Biyong Mengue, Chantal Ngo Bassong et 2 autres)')).toBeTruthy()
  })
})

describe('D-100 : catalogue « Processus de l’unité »', () => {
  it('chaque ligne résume son public ; accès direct au réglage pour qui définit ; la création annonce le public par défaut', async () => {
    const magasin = magasinPouvoirs()
    const s = services(CHEF_DES, magasin)
    rendrePage(<ProcessusPage />, { chemin: '/des/processus', motif: '/des/processus', services: s, session: session(CHEF_DES), autres: [{ motif: '/des/processus/:id', element: <Ici /> }] })
    expect(await screen.findByTestId('public-CREATION_MACHINE_VIRTUELLE')).toBeTruthy()
    expect(screen.getByTestId('public-CREATION_MACHINE_VIRTUELLE').textContent).toBe('Visible et lançable par : Personnes désignées (Idriss Mahamat Saleh, Rachel Biyong Mengue)')
    expect(screen.getByTestId('public-CHANGEMENT_ADRESSE_IP').textContent).toBe('Visible et lançable par : Tout le GIMAC')
    // D-103 : bouton « Public » vers le réglage pour qui définit, brouillon comme version publiée.
    expect(screen.getByRole('button', { name: 'Régler qui peut voir et lancer « Création d’une machine virtuelle »' })).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Nouveau processus' }))
    const d = await screen.findByRole('dialog', { name: 'Nouveau processus' })
    expect(within(d).getByText(/Par défaut, le processus sera visible et lançable par les membres et les responsables de/)).toBeTruthy()
    fireEvent.click(within(d).getByRole('button', { name: 'Annuler' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Régler qui peut voir et lancer « Accès VPN d’un participant »' }))
    await waitFor(() => expect(screen.getByTestId('ici').textContent).toMatch(/^\/des\/processus\/PRC-\d+\?reglage=public$/))
  })
})

// D-103 (docs/lot1/lancement-processus.md §11) : le public d'une version publiée se règle sans nouvelle version, par qui
// définit les processus de l'unité, motif obligatoire ; effet immédiat sur « Lancer une demande » ; étapes, formulaire et
// séparation des fonctions restent figés ; version retirée en lecture seule (services simulés alignés sur MS-PILOTAGE).
const flux = async (q: Qui, magasin: MagasinPouvoirs) => (await services(q, magasin).processus.lister({ etat: 'PUBLIEE' })).find((p) => p.code === 'OUVERTURE_FLUX_RESEAU')!
const personnes = (l: string[]): Initiateurs => ({ regles: ['PERSONNES'], unites: [], profils: [], permissions: [], personnes: l })

describe('D-103 : public modifiable sur une version publiée (services simulés)', () => {

  it('le chef du pôle ouvre le flux à une personne d’un autre service : elle le voit et le lance aussitôt, les membres non', async () => {
    const magasin = magasinPouvoirs()
    const chef = services(RESP, magasin).processus
    const p = await flux(RESP, magasin)
    expect(p.droits).toMatchObject({ modifier: false, modifierPublic: true, retirer: true })
    expect((await flux(RESEAU, magasin)).droits.modifierPublic).toBe(false)
    expect(await codes(SNA, magasin)).not.toContain('OUVERTURE_FLUX_RESEAU')
    expect(await codes(RESEAU, magasin)).toContain('OUVERTURE_FLUX_RESEAU')
    const avant = await chef.lire(p.id)
    const lu = await chef.modifierPublic(p.id, { initiateurs: personnes(['u-sna']), motif: '  Ouverture à la SNA  ' })
    expect([lu.id, lu.version, lu.etat]).toEqual([p.id, avant.version, 'PUBLIEE'])
    expect(lu.initiateurs).toMatchObject({ regles: ['PERSONNES'], personnes: ['u-sna'], initiateurIntervenant: !!avant.initiateurs?.initiateurIntervenant })
    expect(lu.historique.at(-1)).toMatchObject({
      action: 'PUBLIC_MODIFIE', acteur: { id: 'u-resp-info' },
      detail: `Public de la version ${avant.version} modifié : Ouverture à la SNA ; avant : MEMBRES_UNITE ; après : PERSONNES (Rachel Biyong Mengue)`,
    })
    expect(lu.etapes).toEqual(avant.etapes)
    expect(await codes(SNA, magasin)).toContain('OUVERTURE_FLUX_RESEAU')
    expect(await codes(RESEAU, magasin)).not.toContain('OUVERTURE_FLUX_RESEAU')
    expect((await services(SNA, magasin).processus.lire(p.id)).droits).toMatchObject({ lancer: true, modifierPublic: false })
    // Même public : rien n'est consigné.
    const n = (await chef.modifierPublic(p.id, { initiateurs: personnes(['u-sna']), motif: 'Sans changement' })).historique.length
    expect(n).toBe(lu.historique.length)
    // Brouillon : sans motif, public incomplet admis (contrôlé à la publication).
    const b = await chef.creer({ code: 'DEMANDE_D103', unite: 'POLE_RESEAU', libelle: 'Demande D-103', description: '', categorie: '', delaiCibleJours: null, etapes: [] })
    expect(b.droits.modifierPublic).toBe(true)
    const r = await chef.modifierPublic(b.id, { initiateurs: personnes([]) })
    expect([r.etat, r.initiateurs?.regles, r.historique.at(-1)?.detail]).toEqual(['BROUILLON', ['PERSONNES'], 'Public de la version 1 modifié ; avant : MEMBRES_UNITE ; après : PERSONNES'])
    expect(await code(chef.modifierPublic(b.id, { initiateurs: personnes([]), motif: 'ab' }))).toBe('400 PIL_REQUETE_INVALIDE')
  })

  it('refus : sans droit (403), saisie (400), anomalies bloquantes (422), version retirée (409)', async () => {
    const magasin = magasinPouvoirs()
    const chef = services(RESP, magasin).processus
    const p = await flux(RESP, magasin)
    const refus = (e: unknown) => [(e as ApiError).status, (e as ApiError).code, (e as ApiError).details]
    expect(await code(services(RESEAU, magasin).processus.modifierPublic(p.id, { initiateurs: personnes(['u-sna']), motif: 'Ouverture' }))).toBe('403 PIL_PROCESSUS_INTERDIT')
    expect(refus(await chef.modifierPublic(p.id, { initiateurs: personnes(['u-sna']) }).catch((e) => e))).toEqual([400, 'PIL_REQUETE_INVALIDE', { champ: 'motif' }])
    expect(refus(await chef.modifierPublic(p.id, { initiateurs: personnes(['u-sna']), motif: 'x'.repeat(501) }).catch((e) => e))).toEqual([400, 'PIL_REQUETE_INVALIDE', { champ: 'motif' }])
    expect(refus(await chef.modifierPublic(p.id, { motif: 'Ouverture' } as never).catch((e) => e))).toEqual([400, 'PIL_REQUETE_INVALIDE', { champ: 'initiateurs' }])
    expect(refus(await chef.modifierPublic(p.id, { initiateurs: { ...personnes([]), regles: ['INCONNUE' as never] }, motif: 'Ouverture' }).catch((e) => e))[2]).toEqual({ champ: 'initiateurs.regles[0]' })
    expect(refus(await chef.modifierPublic(p.id, { initiateurs: { ...personnes([]), regles: ['UNITES'], unites: ['pole'] }, motif: 'Ouverture' }).catch((e) => e))[2]).toEqual({ champ: 'initiateurs.unites[0]' })
    expect(refus(await chef.modifierPublic(p.id, { initiateurs: { ...personnes([]), regles: ['PERMISSIONS'], permissions: Array.from({ length: 21 }, (_, k) => `P_${k}`) }, motif: 'Ouverture' }).catch((e) => e))[2]).toEqual({ champ: 'initiateurs.permissions' })
    const anomalie = async (i: Initiateurs) => {
      const e = await chef.modifierPublic(p.id, { initiateurs: i, motif: 'Ouverture' }).catch((x: ApiError) => x)
      return [(e as ApiError).code, ...((e as ApiError).details as { anomalies: { code: string; element: string; gravite: string }[] }).anomalies.map((a) => `${a.code} ${a.element} ${a.gravite}`)]
    }
    expect(await anomalie(personnes([]))).toEqual(['PIL_PROCESSUS_ANOMALIES', 'INITIATEURS_INCOMPLETS initiateurs.personnes BLOQUANTE'])
    expect(await anomalie(personnes(['inconnu']))).toEqual(['PIL_PROCESSUS_ANOMALIES', 'PERSONNE_INCONNUE initiateurs.personnes BLOQUANTE'])
    expect(await anomalie({ ...personnes([]), regles: ['UNITES'], unites: ['UNITE_FANTOME'] })).toEqual(['PIL_PROCESSUS_ANOMALIES', 'UNITE_INCONNUE initiateurs.unites BLOQUANTE'])
    expect(await anomalie({ ...personnes([]), regles: ['PROFILS'], profils: ['ROLE_FANTOME'] })).toEqual(['PIL_PROCESSUS_ANOMALIES', 'PROFIL_INCONNU initiateurs.profils BLOQUANTE'])
    expect((await chef.lire(p.id)).initiateurs?.regles).toEqual(['MEMBRES_UNITE'])
    // Nouvelle version publiée : l'ancienne, retirée, a un public figé.
    const v2 = await chef.nouvelleVersion(p.id)
    await chef.publier(v2.id, { leverAvertissements: true, motif: 'Version suivante' })
    const v1 = await chef.lire(p.id)
    expect([v1.etat, v1.droits.modifierPublic]).toEqual(['RETIREE', false])
    expect(await code(chef.modifierPublic(p.id, { initiateurs: personnes(['u-sna']), motif: 'Ouverture' }))).toBe('409 PIL_PROCESSUS_NON_MODIFIABLE')
  })
})

describe('D-103 : réglage du public d’une version publiée dans le modélisateur', () => {
  it('le chef ajoute une personne avec un motif : PUT …/public, formulaire et séparation des fonctions en lecture, annonce ; la personne le voit', async () => {
    const magasin = magasinPouvoirs()
    const s = services(RESP, magasin)
    const modifierPublic = vi.spyOn(s.processus, 'modifierPublic')
    const modifier = vi.spyOn(s.processus, 'modifier')
    const p = await flux(RESP, magasin)
    rendrePage(<ModelisateurProcessusPage />, { chemin: `/des/processus/${p.id}`, motif: '/des/processus/:id', services: s, session: session(RESP) })
    await screen.findByRole('heading', { level: 1, name: 'Ouverture d’un flux réseau' })
    expect(screen.getByText(/son public reste réglable/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Ajouter une étape' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Régler qui peut voir et lancer « Ouverture d’un flux réseau »' }))
    const d = await screen.findByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
    expect(within(d).getByText('Version publiée : seul le public se règle ici')).toBeTruthy()
    expect(within(d).getByText('Le changement vaut pour les prochains lancements ; les demandes déjà lancées ne changent pas.')).toBeTruthy()
    expect((within(d).getByLabelText('L’initiateur peut aussi exécuter, valider ou signer les étapes de sa propre demande') as HTMLInputElement).disabled).toBe(true)
    expect((within(d).getByLabelText('Membres et responsables de l’unité') as HTMLInputElement).disabled).toBe(false)
    expect(within(d).queryByRole('button', { name: 'Ajouter un champ' })).toBeNull()
    const enregistrer = within(d).getByRole('button', { name: 'Enregistrer le public' }) as HTMLButtonElement
    expect([enregistrer.disabled, enregistrer.title]).toEqual([true, 'Le public n’a pas changé.'])
    fireEvent.click(within(d).getByLabelText('Personnes désignées'))
    const zone = within(d).getByTestId('initiateurs-personnes')
    fireEvent.click(await within(zone).findByRole('button', { name: /Rachel Biyong Mengue/ }))
    // Motif obligatoire, 10 caractères au moins.
    expect([enregistrer.disabled, enregistrer.title]).toEqual([true, 'Le motif est obligatoire (10 caractères au moins)'])
    fireEvent.change(within(d).getByLabelText(/^Motif/), { target: { value: 'Demandes de flux de la SNA' } })
    expect(enregistrer.disabled).toBe(false)
    fireEvent.click(enregistrer)
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })).toBeNull())
    expect(modifier).not.toHaveBeenCalled()
    expect(modifierPublic.mock.calls[0]).toEqual([p.id, { initiateurs: { regles: ['MEMBRES_UNITE', 'PERSONNES'], unites: [], profils: [], permissions: [], personnes: ['u-sna'] }, motif: 'Demandes de flux de la SNA' }])
    expect(await screen.findByText('Public enregistré : il vaut pour les prochains lancements.')).toBeTruthy()
    expect(await within(screen.getByTestId('public-processus')).findByText(/Rachel Biyong Mengue/)).toBeTruthy()
    expect(await codes(SNA, magasin)).toContain('OUVERTURE_FLUX_RESEAU')
  })

  it('anomalie bloquante (personne partie entre-temps) : restituée dans la modale, rien ne change', async () => {
    const magasin = magasinPouvoirs()
    const s = services(RESP, magasin)
    const p = await flux(RESP, magasin)
    rendrePage(<ModelisateurProcessusPage />, { chemin: `/des/processus/${p.id}?reglage=public`, motif: '/des/processus/:id', services: s, session: session(RESP) })
    const d = await screen.findByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
    fireEvent.click(within(d).getByLabelText('Personnes désignées'))
    fireEvent.click(await within(within(d).getByTestId('initiateurs-personnes')).findByRole('button', { name: /Rachel Biyong Mengue/ }))
    fireEvent.change(within(d).getByLabelText(/^Motif/), { target: { value: 'Demandes de flux de la SNA' } })
    magasin.organisation.find((m) => m.id === 'u-sna')!.actif = false
    fireEvent.click(within(d).getByRole('button', { name: 'Enregistrer le public' }))
    expect(await within(d).findByText('Le public comporte des anomalies bloquantes : rien n’a été changé.')).toBeTruthy()
    expect(within(within(d).getByTestId('anomalies-public')).getByText(/Personne désignée inconnue, externe ou inactive : Rachel Biyong Mengue/)).toBeTruthy()
    expect((await s.processus.lire(p.id)).initiateurs?.regles).toEqual(['MEMBRES_UNITE'])
  })

  it('sans droit de définir : public en lecture, sans motif ; composant seul : public réglable, formulaire en lecture', async () => {
    const magasin = magasinPouvoirs()
    const p = await flux(RESEAU, magasin)
    rendrePage(<ModelisateurProcessusPage />, { chemin: `/des/processus/${p.id}`, motif: '/des/processus/:id', services: services(RESEAU, magasin), session: session(RESEAU) })
    const voir = await screen.findByRole('button', { name: 'Voir qui peut voir et lancer « Ouverture d’un flux réseau »' })
    // D-106 (§12) : sans droit de définir, ni contrôle ni nouvelle version.
    expect(screen.queryByRole('button', { name: 'Contrôler' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Nouvelle version' })).toBeNull()
    fireEvent.click(voir)
    const d = await screen.findByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
    expect((within(d).getByLabelText('Membres et responsables de l’unité') as HTMLInputElement).disabled).toBe(true)
    expect(within(d).queryByRole('button', { name: /^Enregistrer/ })).toBeNull()
    expect(within(d).queryByLabelText(/^Motif/)).toBeNull()
    cleanup()
    // Composant seul : formulaire en lecture, public réglable sans motif (brouillon d'un service antérieur).
    const enregistrer = vi.fn()
    rendrePage(<ModaleLancement initiateurs={INITIATEURS_DEFAUT} lecture lecturePublic={false} enCours={false} onFermer={() => undefined} onEnregistrer={enregistrer} />, { chemin: '/', motif: '/' })
    const m = await screen.findByRole('dialog', { name: 'Qui peut voir et lancer ce processus' })
    expect(within(m).queryByLabelText(/^Motif/)).toBeNull()
    fireEvent.click(within(m).getByLabelText('Tout le GIMAC'))
    fireEvent.click(within(m).getByRole('button', { name: 'Enregistrer' }))
    await waitFor(() => expect(enregistrer).toHaveBeenCalled())
    expect(enregistrer.mock.calls[0][0].regles).toEqual(['MEMBRES_UNITE', 'TOUT_LE_GIMAC'])
    expect(enregistrer.mock.calls[0][2]).toBe('')
  })
})
