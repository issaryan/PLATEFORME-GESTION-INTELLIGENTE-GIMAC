import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import type { ApiError } from '@gimac/socle'
import { catalogue as libellesModelisateur } from '../../i18n/ecrans/modelisateur'
import { servicesSimules } from './index'

// Recette du lot HOM (docs/recette/homologation.md) : anomalies majeures reproduites sur les services simulés, qui suivent
// les règles de MS-HOMOLOGATION (publication refusée, RACI imposé, tâches à prendre, signatures, notifications).
type S = ReturnType<typeof servicesSimules>
const svc = (profils?: string[], id = 'U-TEST', nom = id) => servicesSimules({ latenceMs: 0, ...(profils ? { utilisateur: { id, nom, profils } } : {}) })
const code = (e: unknown) => (e as ApiError).code
const fichier = (nom = 'piece.pdf') => new File(['contenu'], nom, { type: 'application/pdf' })
const op = async (s: S, liaison: string, rang: string) => (await s.parcours.operations(liaison)).find((o) => o.rang === rang)!

// Démonstration continue : chaque acteur relit l'état laissé par le précédent (gm.demoContinue), comme la recette.
beforeEach(() => {
  localStorage.clear()
  localStorage.setItem('gm.demoContinue', '1')
})
afterEach(() => localStorage.clear())

describe('HOM-01 : publication refusée comme le service, versions de démonstration saines', () => {
  it('les versions de démonstration (publiées et brouillon repris) passent le contrôle du service', async () => {
    const h = svc().homologation
    for (const id of ['REF-CI-3', 'REF-PM-2', 'REF-DL-1', 'REF-AG-1']) {
      const r = await h.controlerReferentiel(id)
      expect({ id, anomalies: r.anomalies, manquants: r.manquants, conforme: r.conforme }).toEqual({ id, anomalies: [], manquants: [], conforme: true })
    }
  })

  it('un brouillon dérivé dont une opération perd son validateur est refusé (HOM_REFERENTIAL_GRAPH_INVALID), la version active reste publiée', async () => {
    const h = svc().homologation
    const b = await h.deriverBrouillon('REF-AG-1')
    const a3 = (await h.operationsReferentiel(b.id)).operations.find((o) => o.code === 'A3')!
    const { code: _c, successeurs: _s, ...saisie } = a3 as typeof a3 & { successeurs?: string[] }
    await h.enregistrerOperation(b.id, 'A3', { ...saisie, validateur: null })
    const erreur = await h.publierReferentiel(b.id).catch((e: ApiError) => e)
    expect(erreur).toMatchObject({ status: 409, code: 'HOM_REFERENTIAL_GRAPH_INVALID' })
    expect(((erreur as ApiError).details as { anomalies: { type: string; codes: string[] }[] }).anomalies).toContainEqual(expect.objectContaining({ type: 'VALIDATEUR_ABSENT', codes: ['A3'] }))
    expect((await h.controlerReferentiel(b.id)).conforme).toBe(false)
    expect((await h.lireReferentiel('REF-AG-1')).etat).toBe('PUBLIEE')
    expect((await h.lireReferentiel(b.id)).etat).toBe('BROUILLON')
  })

  it('une signature qui porte un livrable inconnu est une anomalie de publication (SIGNATURE_PORTEE_INVALIDE)', async () => {
    const h = svc().homologation
    const b = await h.deriverBrouillon('REF-AG-1')
    const a1 = (await h.operationsReferentiel(b.id)).operations.find((o) => o.code === 'A1')!
    const { code: _c, successeurs: _s, ...saisie } = a1 as typeof a1 & { successeurs?: string[] }
    await h.enregistrerOperation(b.id, 'A1', { ...saisie, signatures: [{ code: 'VISA', libelle: 'Visa', signataire: 'CONDUCTEUR', ordre: 1, porte: ['L9'] }] })
    expect((await h.controlerReferentiel(b.id)).anomalies).toContainEqual(expect.objectContaining({ type: 'SIGNATURE_PORTEE_INVALIDE', codes: ['A1', 'VISA', 'L9'] }))
    await expect(h.publierReferentiel(b.id)).rejects.toSatisfy((e) => code(e) === 'HOM_REFERENTIAL_GRAPH_INVALID')
  })
})

describe('HOM-02 : GESTE_CERTIFICATION_ABSENT', () => {
  it('retirer l’opération qui établit le procès-verbal alors que le jalon CERTIFIEE est porté : anomalie et publication refusée', async () => {
    const h = svc().homologation
    const b = await h.deriverBrouillon('REF-AG-1')
    await h.supprimerOperation(b.id, 'A4')
    const r = await h.controlerReferentiel(b.id)
    expect(r.anomalies).toContainEqual(expect.objectContaining({ type: 'GESTE_CERTIFICATION_ABSENT', codes: ['CERTIFIEE', 'certification.pv-established.v1'] }))
    expect(r.anomalies?.find((a) => a.type === 'GESTE_CERTIFICATION_ABSENT')?.motif).toContain('Procès-verbal de certification établi')
    expect((await h.grapheReferentiel(b.id)).anomalies.map((a) => a.type)).toContain('GESTE_CERTIFICATION_ABSENT')
    await expect(h.publierReferentiel(b.id)).rejects.toSatisfy((e) => code(e) === 'HOM_REFERENTIAL_GRAPH_INVALID')
  })

  it('le libellé de l’anomalie existe en français, en anglais et en espagnol', () => {
    for (const l of ['fr', 'en', 'es'] as const) expect(libellesModelisateur[l]['modelisateur.anomalie.GESTE_CERTIFICATION_ABSENT']).toBeTruthy()
  })
})

/** Preuve ouverte du jeu : essais de LIA-2026-038 (rang 16), R Normes et Agréments, V le conducteur de démonstration. */
const preuveOuverte = (s: S) => op(s, 'LIA-2026-038', '16')

describe('HOM-03 : RACI imposé au dépôt et à la validation', () => {
  it('dépôt par un non-désigné refusé ; validation par un autre que le validateur refusée, boutons absents ; refus de R ; validation du V', async () => {
    const o = await preuveOuverte(svc())
    expect(o).toMatchObject({ etat: 'A_FAIRE', geste: 'DEPOSER_PREUVE', raci: { R: { fonction: 'SNA' }, V: { fonction: 'CONDUCTEUR', id: 'U-COND-1' } } })
    const systeme = svc(['ROLE_POLE_SYSTEME'], 'u-sys', 'Chantal Ngo Bassong')
    expect((await op(systeme, o.liaisonId, o.rang)).actionsPermises).not.toContain('DEPOSER_PREUVE')
    await expect(systeme.parcours.deposerPreuve(o.id, fichier(), {})).rejects.toSatisfy((e) => (e as ApiError).status === 403 && code(e) === 'HOM_DEPOT_NON_AUTORISE')

    const r = svc(['ROLE_SNA'], 'u-sna', 'Rachel Biyong Mengue')
    await r.parcours.deposerPreuve(o.id, fichier(), {})
    expect((await op(r, o.liaisonId, o.rang)).etat).toBe('EN_VALIDATION')

    const production = svc(['ROLE_PRODUCTION_MONETIQUE'], 'u-prod', 'Serge Ondoua Essomba')
    const vue = await op(production, o.liaisonId, o.rang)
    expect(vue.actionsPermises).not.toContain('VALIDER')
    expect(vue.actionsPermises).not.toContain('RENVOYER')
    await expect(production.parcours.validerOperation(o.id, 'assertion')).rejects.toSatisfy((e) => (e as ApiError).status === 403 && code(e) === 'HOM_VALIDATION_NON_VALIDATEUR')
    await expect(production.parcours.renvoyer(o.id, 'motif')).rejects.toSatisfy((e) => code(e) === 'HOM_VALIDATION_NON_VALIDATEUR')
    const responsable = svc(['ROLE_SNA'], 'u-sna')
    expect((await op(responsable, o.liaisonId, o.rang)).actionsPermises).not.toContain('VALIDER')
    await expect(responsable.parcours.validerOperation(o.id, 'assertion')).rejects.toSatisfy((e) => code(e) === 'HOM_VALIDATOR_IS_RESPONSIBLE')

    const conducteur = svc(['ROLE_CONDUCTEUR_PROJET'], 'U-201', 'Aïcha Mahamat Nour')
    expect((await op(conducteur, o.liaisonId, o.rang)).actionsPermises).toEqual(expect.arrayContaining(['VALIDER', 'RENVOYER']))
    await conducteur.parcours.validerOperation(o.id, 'assertion')
    expect((await op(conducteur, o.liaisonId, o.rang)).etat).toBe('FAITE')
  })
})

describe('HOM-16 : notification « Validation attendue » au validateur', () => {
  it('le dépôt d’une preuve notifie le validateur désigné, pas le déposant', async () => {
    const sna = svc(['ROLE_SNA'], 'u-sna')
    const o = await preuveOuverte(sna)
    await sna.parcours.deposerPreuve(o.id, fichier(), {})
    expect(sna.evenementsParcours.pourMoi().filter((n) => n.type === 'VALIDATION')).toEqual([])
    const conducteur = svc(['ROLE_CONDUCTEUR_PROJET'], 'U-201')
    expect(conducteur.evenementsParcours.pourMoi()).toContainEqual(expect.objectContaining({ type: 'VALIDATION', operationId: o.id }))
  })
})

describe('HOM-05 et HOM-04 : signatures configurées avant la clôture, fonction à affecter prise depuis les tâches', () => {
  it('le compte rendu de la réunion de lancement attend le visa du conducteur ; la fonction Pôle réseau est offerte puis prise', async () => {
    const ddrm = svc(['ROLE_CHARGE_DDRM'], 'sim-charge')
    const o7 = await op(ddrm, 'LIA-2026-041', '7')
    expect(o7.signatures?.map((x) => [x.code, x.signataire, x.etat])).toEqual([['VISA_CONDUITE', 'CONDUCTEUR', 'ATTENDUE']])
    await ddrm.parcours.deposerLivrable(o7.id, fichier('compte-rendu.pdf'), 'Compte rendu')
    // HOM-05 : le dépôt ne clôt pas l'opération : elle attend sa signature configurée.
    expect((await op(ddrm, 'LIA-2026-041', '7')).etat).toBe('EN_SIGNATURE')
    expect((await op(ddrm, 'LIA-2026-041', '9')).etat).toBe('VERROUILLEE')
    const conducteur = svc(['ROLE_CONDUCTEUR_PROJET'], 'U-201')
    expect((await op(conducteur, 'LIA-2026-041', '7')).actionsPermises).toContain('SIGNER')
    await conducteur.parcours.signerOperation('LIA-2026-041', '7', 'VISA_CONDUITE', 'assertion')
    expect((await op(conducteur, 'LIA-2026-041', '7')).etat).toBe('FAITE')

    // HOM-04 : l'opération du pôle réseau (à affecter) ouverte, sa fonction est une tâche à prendre de MS-PILOTAGE.
    const reseau = svc(['ROLE_POLE_RESEAU'], 'u-res', 'Idriss Mahamat Saleh')
    await reseau.parcours.operations('LIA-2026-041')
    const aPrendre = (await reseau.pilotage.taches({ vue: 'unite' })).contenu.filter((t) => t.origine.reference === 'LIA-2026-041/AFFECTER/POLE_RESEAU')
    expect(aPrendre).toHaveLength(1)
    expect(aPrendre[0]).toMatchObject({ titre: 'Prendre la fonction Pôle réseau (LIA-2026-041)', etat: 'A_PRENDRE', permissionsRequises: ['HOM_FUNCTION_NETWORK'] })
    await reseau.pilotage.prendre(aPrendre[0]!.id)
    const gouvernance = (await reseau.parcours.dossier('LIA-2026-041')).intervenants?.find((i) => i.fonction === 'POLE_RESEAU')!
    expect(gouvernance).toMatchObject({ aAffecter: false, personne: { id: 'u-res', nom: 'Idriss Mahamat Saleh' } })
    const ouvertes = (await reseau.parcours.operations('LIA-2026-041')).filter((x) => x.raci.R?.fonction === 'POLE_RESEAU' && x.etat !== 'FAITE')
    expect(ouvertes.length).toBeGreaterThan(0)
    expect(ouvertes.every((x) => x.raci.R?.id === 'u-res')).toBe(true)
    // Fonction pourvue : plus de tâche à prendre pour elle.
    const apres = svc(['ROLE_POLE_RESEAU'], 'u-res')
    await apres.parcours.operations('LIA-2026-041')
    expect((await apres.pilotage.taches({ vue: 'unite' })).contenu.filter((t) => t.origine.reference === 'LIA-2026-041/AFFECTER/POLE_RESEAU' && t.etat === 'A_PRENDRE')).toEqual([])
  })
})

describe('HOM-17 : « Chefs des deux départements » reliée aux deux profils de chef', () => {
  it('catalogue des fonctions : ROLE_CHEF_DES et ROLE_CHEF_DDRM', async () => {
    const f = (await svc().homologation.fonctionsReferentiel()).find((x) => x.code === 'CHEFS_DEPARTEMENT')!
    expect(f.profils).toEqual(['ROLE_CHEF_DES', 'ROLE_CHEF_DDRM'])
  })
})
