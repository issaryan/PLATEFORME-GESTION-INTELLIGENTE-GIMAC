import { afterEach, describe, expect, it } from 'vitest'
import { ApiError } from '@gimac/socle'
import { servicesSimules } from '.'
import { demandesDemo } from './processus'
import { magasinPouvoirs } from './pouvoirs'
import { UNITES_DEMO } from './pilotage'
import { INITIATEURS_DEFAUT, etapesEnCours, type InstanceProcessus } from '../../modele/processus'

// D-98 (docs/lot1/lancement-processus.md §1 à §4) : MS-PILOTAGE simulé. Initiateurs en données (défaut : membres de
// l'unité ; tout le GIMAC ; unités, profils, permissions), formulaire de lancement contrôlé champ par champ, processus que
// je peux lancer, « Mes demandes », suivi par l'initiateur jusqu'à la clôture, état gardé dans la session de démonstration.
const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: { status: number; code: string }) => `${e.status} ${e.code}`)
function monde() {
  const magasin = magasinPouvoirs()
  return (id: string) => servicesSimules({ latenceMs: 0, utilisateur: { id, nom: id }, pouvoirs: magasin }).processus
}
const fichier = (nom: string, type: string) => new File(['contenu'], nom, { type })
const VALEURS_IP = { EQUIPEMENT: 'PC-SNA-014', ADRESSE_ACTUELLE: '10.20.4.15', ADRESSE_DEMANDEE: '10.20.6.40', JUSTIFICATION: 'Changement de bureau.' }
const etape = (i: InstanceProcessus, c: string) => i.etapes.find((e) => e.code === c)!

afterEach(() => {
  sessionStorage.clear()
})

describe('D-98 : jeu de démonstration', () => {
  it('unité RH et deux demandes ouvertes à tout le GIMAC, étapes analyse, validation, exécution, clôture', () => {
    expect(UNITES_DEMO.find((u) => u.code === 'RH')?.parent).toBe('DG')
    const [conge, ip] = demandesDemo()
    expect([conge.code, conge.unite, ip.code, ip.unite]).toEqual(['DEMANDE_CONGE', 'RH', 'CHANGEMENT_ADRESSE_IP', 'POLE_RESEAU'])
    expect(conge.formulaire.champs.map((c) => [c.type, c.obligatoire])).toEqual([['PERIODE', true], ['TEXTE_LONG', true], ['PIECE_JOINTE', false]])
    expect(ip.formulaire.champs.map((c) => c.code)).toEqual(['EQUIPEMENT', 'ADRESSE_ACTUELLE', 'ADRESSE_DEMANDEE', 'JUSTIFICATION'])
    for (const d of [conge, ip]) expect(d.etapes.map((e) => e.code)).toEqual(['ANALYSE', 'VALIDATION', 'EXECUTION', 'CLOTURE'])
    expect(INITIATEURS_DEFAUT.regles).toEqual(['MEMBRES_UNITE'])
  })
})

describe('D-98 : qui peut lancer', () => {
  it('processus que je peux lancer, groupés par unité ; refus explicite hors des initiateurs ; définition lisible de qui la lance', async () => {
    const qui = monde()
    const sna = qui('u-sna')
    const l = await sna.lancables({})
    // D-100 : l'analyste du SNA est l'une des deux personnes désignées de la création d'une machine virtuelle.
    expect(l.groupes.map((g) => [g.unite, g.processus.map((p) => p.code)])).toEqual([['POLE_RESEAU', ['CHANGEMENT_ADRESSE_IP']], ['POLE_SYSTEME', ['CREATION_MACHINE_VIRTUELLE']], ['RH', ['DEMANDE_CONGE']]])
    expect(l.groupes[0].processus[0]).toMatchObject({ droits: { lancer: true }, nombreChampsFormulaire: 4, initiateurs: { regles: ['TOUT_LE_GIMAC'] } })
    expect((await sna.lancables({ q: 'congé' })).groupes.map((g) => g.unite)).toEqual(['RH'])
    const reseau = (await qui('u-res').lancables({})).groupes.find((g) => g.unite === 'POLE_RESEAU')!
    expect(reseau.processus.map((p) => p.code)).toEqual(['CHANGEMENT_ADRESSE_IP', 'OUVERTURE_FLUX_RESEAU'])
    const ip = await sna.lire(l.groupes[0].processus[0].id)
    expect(ip.formulaire?.champs).toHaveLength(4)
    const flux = (await qui('u-res').lister({ etat: 'PUBLIEE' })).find((p) => p.code === 'OUVERTURE_FLUX_RESEAU')!
    expect(await code(sna.lancer(flux.id, { intitule: 'Flux', champs: {}, designations: { MISE_EN_OEUVRE: 'u-res' } }))).toBe('403 PIL_PROCESSUS_NON_INITIATEUR')
  })

  it('modélisateur : initiateurs et formulaire gardés avec la définition et contrôlés à la publication', async () => {
    const chef = monde()('u-resp-info')
    const [, ip] = demandesDemo()
    const p = await chef.creer({ code: 'DEMANDE_TEST', unite: 'POLE_RESEAU', libelle: 'Demande test', description: '', categorie: '', delaiCibleJours: null, etapes: ip.etapes,
      initiateurs: { regles: ['UNITES', 'PERMISSIONS'], unites: [], profils: [], permissions: [] }, formulaire: { champs: [{ code: 'TYPE', libelle: 'Type', type: 'LISTE', choix: [], obligatoire: true, aide: null, typesPieces: [] }] } })
    expect(p.initiateurs?.regles).toEqual(['UNITES', 'PERMISSIONS'])
    const c = await chef.controler(p.id)
    expect(c.publiable).toBe(false)
    expect(c.anomalies.filter((a) => a.etape === null).map((a) => [a.code, a.element])).toEqual([
      ['INITIATEURS_INCOMPLETS', 'initiateurs.unites'], ['INITIATEURS_INCOMPLETS', 'initiateurs.permissions'], ['CHAMP_CHOIX_ABSENT', 'formulaire.TYPE'],
    ])
    const m = await chef.modifier(p.id, { code: p.code, unite: p.unite, libelle: p.libelle, description: '', categorie: '', delaiCibleJours: null, etapes: p.etapes })
    expect(m.initiateurs?.regles).toEqual(['UNITES', 'PERMISSIONS'])
  })
})

describe('D-98 : formulaire de lancement', () => {
  it('422 champ par champ (details.champs « formulaire.CODE »), pièce jointe déposée par l’initiateur', async () => {
    const sna = monde()('u-sna')
    const lancables = (await sna.lancables({})).groupes.flatMap((g) => g.processus)
    const [ipResume, conge] = ['CHANGEMENT_ADRESSE_IP', 'DEMANDE_CONGE'].map((c) => lancables.find((p) => p.code === c)!)
    const refus = await sna.lancer(ipResume.id, { intitule: 'Poste', champs: {}, designations: {}, formulaire: { EQUIPEMENT: 'PC', COULEUR: 'bleu' } }).catch((e: ApiError) => e)
    expect(refus).toBeInstanceOf(ApiError)
    expect((refus as ApiError).code).toBe('PIL_PROCESSUS_FORMULAIRE_INVALIDE')
    expect(Object.keys((refus as ApiError).details?.champs as object).sort()).toEqual(['formulaire.ADRESSE_ACTUELLE', 'formulaire.ADRESSE_DEMANDEE', 'formulaire.COULEUR', 'formulaire.JUSTIFICATION'])
    const texte = await sna.televerserPieceLancement(fichier('note.txt', 'text/plain'), conge.id)
    const periode = { debut: '2026-12-21', fin: '2026-12-31' }
    expect(await code(sna.lancer(conge.id, { intitule: 'Congé', champs: {}, designations: {}, formulaire: { PERIODE: periode, MOTIF: 'Fêtes', JUSTIFICATIF: { documentId: texte.documentId } } }))).toBe('422 PIL_PROCESSUS_FORMULAIRE_INVALIDE')
    expect(await code(sna.lancer(conge.id, { intitule: 'Congé', champs: {}, designations: {}, formulaire: { PERIODE: { debut: '2026-12-31', fin: '2026-12-21' }, MOTIF: 'Fêtes' } }))).toBe('422 PIL_PROCESSUS_FORMULAIRE_INVALIDE')
    const pdf = await sna.televerserPieceLancement(fichier('billet.pdf', 'application/pdf'), conge.id)
    const i = await sna.lancer(conge.id, { intitule: 'Congé de fin d’année', champs: {}, designations: {}, formulaire: { PERIODE: periode, MOTIF: 'Fêtes', JUSTIFICATIF: { documentId: pdf.documentId } } })
    expect(i.formulaire?.map((c) => c.valeur)).toEqual([periode, 'Fêtes', expect.objectContaining({ documentId: pdf.documentId, nomFichier: 'billet.pdf', typeMime: 'application/pdf' })])
    // La vérification des droits revient au gestionnaire RH, seul porteur du profil dans l'unité.
    expect(etape(i, 'ANALYSE').executant?.nomAffiche).toBe('Thierry Bekolo Ngoa')
  })
})

describe('D-98 : suivi par l’initiateur', () => {
  it('un collaborateur d’un autre service lance une demande au pôle réseau, l’intervenant agit, l’initiateur voit l’avancement jusqu’à la clôture', async () => {
    const qui = monde()
    const sna = qui('u-sna')
    const ip = (await sna.lancables({})).groupes[0].processus[0]
    const lancee = await sna.lancer(ip.id, { intitule: 'Poste d’accueil', champs: {}, designations: {}, formulaire: VALEURS_IP })
    expect(lancee.lanceur.id).toBe('u-sna')
    expect(etape(lancee, 'ANALYSE').moi.executer).toBe(false)
    let d = (await sna.demandes({})).contenu[0]
    expect([d.intitule, d.etat, etapesEnCours(d.etapes).map((e) => [e.code, e.executant?.nomAffiche])]).toEqual(['Poste d’accueil', 'EN_COURS', [['ANALYSE', 'Idriss Mahamat Saleh']]])
    // L'intervenant du pôle voit les valeurs fournies et les seules actions permises.
    const res = qui('u-res')
    let i = await res.instance(lancee.id)
    expect(i.formulaire?.find((c) => c.code === 'ADRESSE_DEMANDEE')?.valeur).toBe('10.20.6.40')
    expect(etape(i, 'ANALYSE').moi).toMatchObject({ executer: true, prendre: false, valider: false })
    await res.saisirChamps(lancee.id, 'ANALYSE', { ADRESSE_DISPONIBLE: 'Oui' })
    i = await res.terminer(lancee.id, 'ANALYSE')
    d = (await sna.demandes({})).contenu[0]
    expect(d.etapes.map((e) => e.etat)).toEqual(['TERMINEE', 'ACTIVE', 'EN_ATTENTE', 'EN_ATTENTE'])
    expect(etapesEnCours(d.etapes)[0].executant?.nomAffiche).toBe('Gaston Ndzana Ebode')
    expect(d.derniereEvolution?.action).toBe('ETAPE_ACTIVEE')
    const chef = qui('u-resp-info')
    await chef.saisirChamps(lancee.id, 'VALIDATION', { DECISION: 'Accordée' })
    await chef.terminer(lancee.id, 'VALIDATION')
    await res.saisirChamps(lancee.id, 'EXECUTION', { ADRESSE_ATTRIBUEE: '10.20.6.40' })
    await res.terminer(lancee.id, 'EXECUTION')
    i = await res.terminer(lancee.id, 'CLOTURE')
    expect(i.etat).toBe('CLOSE')
    expect((await sna.demandes({ etat: 'CLOSE' })).contenu.map((x) => [x.intitule, x.avancement.terminees])).toEqual([['Poste d’accueil', 4]])
    expect((await sna.demandes({ etat: 'EN_COURS', q: 'accueil' })).total).toBe(0)
    expect((await sna.instance(lancee.id)).etat).toBe('CLOSE')
  })

  it('session de démonstration : l’état survit au changement de profil (rechargement), les tâches des étapes renaissent', async () => {
    sessionStorage.setItem('gm.sessionDemo', JSON.stringify({ identifiant: '' }))
    const avant = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-sna', nom: 'Rachel Biyong Mengue' }, pouvoirs: magasinPouvoirs() })
    const ip = (await avant.processus.lancables({})).groupes[0].processus[0]
    const lancee = await avant.processus.lancer(ip.id, { intitule: 'Imprimante du 2e étage', champs: {}, designations: {}, formulaire: VALEURS_IP })
    // Autre profil, autre magasin (rechargement de l'application) : l'instance et la tâche de l'intervenant sont là.
    const apres = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-res', nom: 'Idriss Mahamat Saleh' }, pouvoirs: magasinPouvoirs() })
    const i = await apres.processus.instance(lancee.id)
    expect(etape(i, 'ANALYSE').moi.executer).toBe(true)
    const taches = await apres.pilotage.taches({ vue: 'moi', page: 0, taille: 50 })
    expect(taches.contenu.some((t) => t.titre === 'Imprimante du 2e étage : Analyse technique')).toBe(true)
  })
})
