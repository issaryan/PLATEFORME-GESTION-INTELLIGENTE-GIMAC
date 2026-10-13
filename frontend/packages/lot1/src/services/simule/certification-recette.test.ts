import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { OperationParcours } from '../../modele/parcours'
import { servicesSimules } from './index'
import { aujourdhui } from '../../modele/jours'

// Recette du lot CER (docs/recette/certification.md) : anomalies reproduites sur les services simulés, qui suivent les
// règles de MS-CERTIFICATION et de MS-HOMOLOGATION (circuit configuré, codes de refus, tâches des visas, horloge).
type S = ReturnType<typeof servicesSimules>
const svc = (profils?: string[], id = 'U-TEST') => servicesSimules({ latenceMs: 0, ...(profils ? { utilisateur: { id, nom: id, profils } } : {}) })
const refus = (code: string) => expect.objectContaining({ code })
// DDR-15 : la démonstration date ses gestes du jour courant de Douala ; ces attentes portent sur le 29/09/2026.
beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-09-29T09:00:00Z'))
})
afterEach(() => vi.useRealTimers())
const fichier = (nom = 'piece.pdf') => new File(['contenu'], nom, { type: 'application/pdf' })
const op = async (s: S, liaison: string, rang: string) => (await s.parcours.operations(liaison)).find((o) => o.rang === rang)!

async function clore(s: S, o: OperationParcours) {
  if (o.actionsPermises.includes('DEPOSER_LIVRABLE')) return s.parcours.deposerLivrable(o.id, fichier(), 'Livrable remis')
  await s.parcours.deposerPreuve(o.id, fichier(), { codeControle: o.cle ? 'A1B2C3' : undefined, valeurReference: o.valeurReference ? `REF-${o.rang}` : undefined })
  await s.parcours.validerOperation(o.id, 'assertion')
}

describe('CER-01 : circuit de signature du procès-verbal configuré et codes de refus de MS-CERTIFICATION', () => {
  it('circuit par défaut : GIMAC et participant, même ordre ; la vue du portail désigne le GIMAC par sa fonction', async () => {
    const s = svc()
    const pv = await s.decision.procesVerbal('LIA-2026-027')
    expect(pv.circuit?.map((e) => [e.code, e.fonction, e.ordre, e.portail, e.etat])).toEqual([['GIMAC', 'CHEF_DES', 1, false, 'ATTENDUE'], ['PARTICIPANT', 'PARTICIPANT', 1, true, 'ATTENDUE']])
    await s.decision.signerProcesVerbalGimac('LIA-2026-027', 'a')
    const portail = await s.portailParcours.procesVerbal('LIA-2026-027')
    expect(portail.circuit?.[0]).toMatchObject({ code: 'GIMAC', etat: 'SIGNEE', signature: { signataire: 'CHEF_DES' } })
  })

  it('circuit configuré sur l’opération (ordre imposé, participant ajouté) : CER_SIGNATURE_ORDRE avant le tour du GIMAC', async () => {
    const s = svc()
    await s.decision.etablirProcesVerbal('LIA-2026-025')
    const pv = await s.decision.procesVerbal('LIA-2026-025')
    expect(pv.circuit?.map((e) => [e.code, e.ordre, e.portail])).toEqual([['GIMAC', 2, false], ['PARTICIPANT', 1, true]])
    expect(pv.actionsPermises).not.toContain('SIGNER_GIMAC')
    await expect(s.decision.signerProcesVerbalGimac('LIA-2026-025', 'a')).rejects.toEqual(expect.objectContaining({ code: 'CER_SIGNATURE_ORDRE', status: 409, details: { attendues: ['PARTICIPANT'] } }))
    expect((await s.decision.procesVerbal('LIA-2026-025')).etat).toBe('ETABLI')
  })

  it('CER_SIGNATAIRE_NON_DESIGNE : une personne qui ne tient pas la fonction de l’étape ne signe pas', async () => {
    const s = svc(['ROLE_CONDUCTEUR_PROJET'], 'U-COND-1')
    expect((await s.decision.procesVerbal('LIA-2026-027')).actionsPermises).not.toContain('SIGNER_GIMAC')
    await expect(s.decision.signerProcesVerbalGimac('LIA-2026-027', 'a')).rejects.toEqual(expect.objectContaining({ code: 'CER_SIGNATAIRE_NON_DESIGNE', status: 403 }))
    const chef = svc(['ROLE_CHEF_DES'], 'U-CDES')
    expect((await chef.decision.procesVerbal('LIA-2026-027')).actionsPermises).toContain('SIGNER_GIMAC')
    await chef.decision.signerProcesVerbalGimac('LIA-2026-027', 'a')
    expect((await chef.decision.procesVerbal('LIA-2026-027')).etat).toBe('SIGNE_GIMAC')
  })

  it('CER_OPERATION_NON_OUVERTE et CER_SIGNATAIRE_NON_DESIGNE sur un visa', async () => {
    const conformite = svc(['ROLE_CONFORMITE'], 'U-CONF')
    await expect(conformite.decision.signerVisa('LIA-2026-019', 'CONFORMITE', true, 'conforme', 'a')).rejects.toEqual(refus('CER_OPERATION_NON_OUVERTE'))
    const s = svc()
    await s.decision.verifierAttestation('LIA-2026-019', 'PCI_DSS', true, '')
    await s.decision.archiverAttestations('LIA-2026-019')
    const dg = svc(['ROLE_DG'], 'U-DG')
    await dg.decision.verifierAttestation('LIA-2026-019', 'PCI_DSS', true, '')
    await dg.decision.archiverAttestations('LIA-2026-019')
    await expect(dg.decision.signerVisa('LIA-2026-019', 'CONFORMITE', true, 'conforme', 'a')).rejects.toEqual(expect.objectContaining({ code: 'CER_SIGNATAIRE_NON_DESIGNE', status: 403 }))
    await dg.decision.signerVisa('LIA-2026-019', 'DIRECTION_GENERALE', true, 'favorable', 'a')
  })
})

describe('CER-02 : visas attendus de la conformité et de la DG', () => {
  it('compteur relié à la liaison, tâche et entrée de « Mes actions » une fois l’opération de l’approbation ouverte', async () => {
    const s = svc(['ROLE_CONFORMITE'], 'U-CONF')
    const compteur = (await s.parcours.tableauActeur(['CONFORMITE'])).compteurs.find((c) => c.cle === 'visasAttendus')
    expect(compteur).toMatchObject({ valeur: 1, lien: '/des/parcours/LIA-2026-019?onglet=decision' })
    expect((await s.parcours.tableauActeur(['DG'])).compteurs.find((c) => c.cle === 'visasAttendus')?.lien).toBe('/des/parcours/LIA-2026-019?onglet=decision')
    await s.decision.verifierAttestation('LIA-2026-019', 'PCI_DSS', true, '')
    await s.decision.archiverAttestations('LIA-2026-019')
    const tache = (await s.parcours.tableauActeur(['CONFORMITE'])).taches.find((t) => t.signature === 'CONFORMITE')
    expect(tache).toMatchObject({ liaisonId: 'LIA-2026-019', qualite: 'V', libelle: 'Signer : Visa de la conformité', lien: '/des/parcours/LIA-2026-019?onglet=decision&operation=LIA-2026-019%3A23' })
    expect((await s.parcours.tableauActeur(['DG'])).taches.map((t) => t.signature)).toEqual(['DIRECTION_GENERALE'])
    const mes = await s.parcours.mesOperations()
    expect(mes.aAgir.find((o) => o.signature === 'CONFORMITE')).toMatchObject({ liaisonId: 'LIA-2026-019', lien: tache?.lien })
    await s.decision.signerVisa('LIA-2026-019', 'CONFORMITE', true, 'conforme', 'a')
    expect((await s.parcours.mesOperations()).aAgir.some((o) => o.signature === 'CONFORMITE')).toBe(false)
  })
})

describe('données de démonstration cohérentes', () => {
  it('CER-07 : deux pièces différentes n’ont jamais la même empreinte', async () => {
    const s = svc()
    await s.decision.etablirProcesVerbal('LIA-2026-025')
    const a = (await s.decision.procesVerbal('LIA-2026-025')).empreinte
    const b = (await s.decision.procesVerbal('LIA-2026-027')).empreinte
    expect(a).toMatch(/^[0-9a-f]{64}$/)
    expect(a).not.toBe(b)
    const dossiers = await Promise.all(['LIA-2026-019', 'LIA-2026-012'].map(async (id) => (await s.decision.approbation(id)).dossierChangement?.empreinte))
    expect(dossiers[0]).not.toBe(dossiers[1])
  })

  it('CER-08 et CER-16 : signature d’opération datée sur l’horloge de la démonstration, par le conducteur de la liaison', async () => {
    // Un même service tient les deux fonctions signataires (Normes et Agréments, conducteur) pour enchaîner les signatures.
    const cond = svc(['ROLE_SNA', 'ROLE_CONDUCTEUR_PROJET'], 'U-COND-2')
    const o = await op(cond, 'LIA-2026-036', '11')
    expect(o.signatures?.find((x) => x.code === 'VISA_CONDUITE')?.attendu?.nom).toBe('Brice Ondo Mba')
    await cond.parcours.signerOperation('LIA-2026-036', '11', 'VISA_NORMES', 'a')
    await cond.parcours.signerOperation('LIA-2026-036', '11', 'VISA_CONDUITE', 'a')
    const signee = (await op(cond, 'LIA-2026-036', '11')).signatures?.find((x) => x.code === 'VISA_CONDUITE')
    expect(signee).toMatchObject({ etat: 'SIGNEE', signeParNom: 'Brice Ondo Mba', signeLe: expect.stringMatching(new RegExp(`^${aujourdhui()}T`)) })
  })

  it('CER-11 : statut déduit de l’étape courante avant tout jalon (intégration puis essais)', async () => {
    const s = svc()
    expect((await s.parcours.dossier('LIA-2026-040')).statut).toBe('EN_INTEGRATION')
    for (let i = 0; i < 20 && (await s.parcours.dossier('LIA-2026-040')).etapeCourante === 'INTEGRATION'; i++) {
      const o = (await s.parcours.operations('LIA-2026-040')).find((x) => x.etat === 'A_FAIRE' && x.etape === 'INTEGRATION')
      if (!o) break
      await clore(s, o)
    }
    expect((await s.parcours.dossier('LIA-2026-040'))).toMatchObject({ etapeCourante: 'TESTS', statut: 'EN_TEST' })
  })

  it('CER-05 : type créé dans la plateforme servi avec son libellé au DES et au portail (Vues.Dossier, LiaisonVue)', async () => {
    const s = svc(['ROLE_CHEF_DDRM'], 'U-020')
    const ouverte = await s.parcours.ouvrirLiaison({ institutionId: 'INST-0042', typeProjet: 'AGREGATEUR', chefDeProjetId: 'U-COND-1', banquePartenaireId: null })
    expect(ouverte).toMatchObject({ typeProjet: 'AGREGATEUR', typeProjetLibelle: 'Agrégateur de paiement' })
    expect(await s.parcours.dossier(ouverte.id)).toMatchObject({ typeProjet: 'AGREGATEUR', typeProjetLibelle: 'Agrégateur de paiement' })
    expect((await s.portailParcours.mesLiaisons()).find((l) => l.id === ouverte.id)).toMatchObject({ typeProjetLibelle: 'Agrégateur de paiement' })
    expect(await s.portailParcours.dossier(ouverte.id)).toMatchObject({ typeProjetLibelle: 'Agrégateur de paiement' })
  })

  it('CER-20 : plus aucun geste proposé une fois l’approbation prononcée', async () => {
    expect((await svc().decision.approbation('LIA-2025-088'))).toMatchObject({ etat: 'APPROUVEE', actionsPermises: [] })
  })
})
