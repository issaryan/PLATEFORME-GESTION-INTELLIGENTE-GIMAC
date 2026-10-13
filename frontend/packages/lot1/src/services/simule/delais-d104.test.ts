import { describe, expect, it } from 'vitest'
import { servicesSimules } from '.'
import { magasinPouvoirs } from './pouvoirs'
import { delaiMaxEffectif, etapeVierge, type EtapeProcessus, type SaisieProcessus } from '../../modele/processus'
import { delaiOperation, erreursSaisie, operationVierge } from '../../adaptateurs/modelisateur'
import { erreursEtape } from '../../pages/des/processus/ModaleEtape'
import { delais } from '../../pages/des/processus/communs'
import { ajouterJours, aujourdhui } from '../../modele/jours'

// D-106 : délai cible et délai maximal des processus internes et des opérations de certification (saisie, contrôle
// maximum >= cible, échéances : la tâche porte le maximum, l'échéance proche part de la cible).
const code = (p: Promise<unknown>) => p.then(() => 'ok', (e: { status: number; code: string; details?: { champ?: string } }) => `${e.status} ${e.code} ${e.details?.champ ?? ''}`.trim())
const t = (k: string, o?: Record<string, unknown>) => `${k}${o ? JSON.stringify(o) : ''}`
const services = (id: string) => servicesSimules({ latenceMs: 0, utilisateur: { id, nom: id }, pouvoirs: magasinPouvoirs() })
const saisie = (etapes: EtapeProcessus[], o: Partial<SaisieProcessus> = {}): SaisieProcessus => ({ code: 'DELAIS_TEST', unite: 'POLE_RESEAU', libelle: 'Délais', description: '', categorie: '', delaiCibleJours: 5, etapes, ...o })

describe('D-106 : délais des processus internes', () => {
  it('délai maximal effectif et affichage « cible à maximum »', () => {
    expect(delaiMaxEffectif(3, null)).toBe(3)
    expect(delaiMaxEffectif(3, undefined)).toBe(3)
    expect(delaiMaxEffectif(3, 7)).toBe(7)
    expect(delais(t, 3, null)).toBe('proc.carte.delai{"n":3}')
    expect(delais(t, 3, 7)).toBe('proc.carte.delais{"cible":3,"max":7}')
  })

  it('contrôle de la modale : maximum entier, du délai cible à 365 jours', () => {
    const e = { ...etapeVierge(1), libelle: 'A', delaiJours: 4 }
    expect(erreursEtape({ ...e, delaiMaxJours: 6 }, []).delaiMaxJours).toBeUndefined()
    expect(erreursEtape({ ...e, delaiMaxJours: null }, []).delaiMaxJours).toBeUndefined()
    expect(erreursEtape({ ...e, delaiMaxJours: 3 }, []).delaiMaxJours).toBe('proc.erreur.delaiMax')
    expect(erreursEtape({ ...e, delaiMaxJours: 366 }, []).delaiMaxJours).toBe('proc.erreur.delaiMax')
  })

  it('service simulé : refus 400 comme MS-PILOTAGE, maximum conservé, échéances de l’étape activée', async () => {
    const chef = services('U-001').processus
    const a: EtapeProcessus = { ...etapeVierge(1), code: 'A', libelle: 'A', delaiJours: 2, delaiMaxJours: 1 }
    expect(await code(chef.creer(saisie([a])))).toBe('400 PIL_REQUETE_INVALIDE etapes[0].delaiMaxJours')
    expect(await code(chef.creer(saisie([], { delaiMaxJours: 4 })))).toBe('400 PIL_REQUETE_INVALIDE delaiMaxJours')
    const p = await chef.creer(saisie([{ ...a, delaiMaxJours: 6 }], { delaiMaxJours: 9 }))
    expect(p.delaiMaxJours).toBe(9)
    expect(p.etapes[0].delaiMaxJours).toBe(6)
    // Démonstration : « Ouverture d'un flux réseau » porte 10 à 15 jours, son étape d'analyse 2 à 4 jours.
    const magasin = magasinPouvoirs()
    magasin.organisation.push({ id: 'u-res-bis', nomAffiche: 'Armel Nkoulou Beti', fonction: 'Technicien réseau', unite: 'POLE_RESEAU', responsableId: 'u-resp-info', actif: true })
    magasin.definirProfils('u-res-bis', ['ROLE_POLE_RESEAU'])
    const membre = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-res', nom: 'u-res' }, pouvoirs: magasin }).processus
    const flux = (await membre.lister({ etat: 'PUBLIEE' })).find((x) => x.code === 'OUVERTURE_FLUX_RESEAU')!
    expect(flux).toMatchObject({ delaiCibleJours: 10, delaiMaxJours: 15 })
    const i = await membre.lancer(flux.id, { intitule: 'Flux', champs: { SOURCE: '10.0.0.1' }, designations: { MISE_EN_OEUVRE: 'u-res-bis' } })
    expect(i.delais).toMatchObject({ cibleJours: 10, maxJours: 15, echeanceCible: ajouterJours(aujourdhui(), 10), echeanceMax: ajouterJours(aujourdhui(), 15), enRetard: false, aLApproche: false })
    const demande = i.etapes[0]
    expect(demande.echeanceCible).toBe(ajouterJours(aujourdhui(), 1))
    expect(demande.echeance).toBe(ajouterJours(aujourdhui(), 1))
    expect(demande).toMatchObject({ enRetard: false, aLApproche: false })
  })
})

describe('D-106 : délais des opérations de certification', () => {
  it('modale : maximum facultatif, au moins le délai cible ; affichage', () => {
    const o = { ...operationVierge('CADRAGE', 'AMONT'), code: 'X1', libelle: 'X', responsable: 'DDRM' as const, validateur: 'CONDUCTEUR' as const, delaiJours: 5 }
    expect(erreursSaisie({ ...o, delaiMaxJours: 8 }, true, []).delaiMaxJours).toBeUndefined()
    expect(erreursSaisie({ ...o, delaiMaxJours: 4 }, true, []).delaiMaxJours).toBe('modelisateur.erreur.delaiMax')
    expect(delaiOperation(t, 5, null)).toBe('modelisateur.carte.delai{"n":5}')
    expect(delaiOperation(t, 5, 8)).toBe('modelisateur.carte.delais{"cible":5,"max":8}')
  })
})
