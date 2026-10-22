import { describe, expect, it } from 'vitest'
import { ApiError } from '@gimac/socle'
import { CORRESPONDANT_99004, ESPACES_DEMO, espacesSimules } from './simule'
import { PERMISSION } from '../modele'

// SBX-ESPACE simulé au contrat des routes de la console (EspaceConsoleController, ProblemesEspace, EspaceInstitution).
const ADMIN = [PERMISSION.consultation, PERMISSION.administration]
const T = () => new Date('2026-10-22T09:00:00Z')
const service = (o: Partial<Parameters<typeof espacesSimules>[0]> = {}) => espacesSimules({ permissions: ADMIN, maintenant: T, ...o })
const CLE = 'cle-0001-abcdef'

async function refus(p: Promise<unknown>): Promise<ApiError> {
  try {
    await p
  } catch (e) {
    return e as ApiError
  }
  throw new Error('refus attendu')
}

describe('SBX-ESPACE simulé', () => {
  it('liste par curseur : pages pleines, curseur suivant, fin de liste, taille bornée', async () => {
    const s = service()
    const p1 = await s.lister({ taille: 2 })
    expect(p1.elements.map((x) => x.codeParticipant)).toEqual(['99001', '99002'])
    expect(p1.suivant).toBe('99002')
    const p2 = await s.lister({ apres: p1.suivant!, taille: 2 })
    expect(p2.elements.map((x) => x.codeParticipant)).toEqual(['99003'])
    expect(p2.suivant).toBeNull()
    expect((await s.lister({ taille: 0 })).elements).toHaveLength(1)
    expect((await s.lister({})).elements).toHaveLength(3)
    expect((await s.lister({ taille: 500 })).suivant).toBeNull()
  })

  it('lecture : sans SBX_HAB_CONSULTATION ou pour un compte d’institution, 404 indiscernable', async () => {
    expect((await refus(service({ permissions: [PERMISSION.administration] }).lister({}))).code).toBe('SBX_ESP_INTROUVABLE')
    expect((await refus(service({ institution: '99001' }).consulter('99001'))).status).toBe(404)
    expect((await refus(service().consulter('99999'))).code).toBe('SBX_ESP_INTROUVABLE')
    expect((await service().consulter('99003')).motifsSuspension).toEqual(['DECISION_GIMAC', 'STATUT_REGISTRE'])
  })

  it('ouverture : espace OUVERT avec son premier administrateur, clé rejouée sans second effet', async () => {
    const s = service()
    const o = await s.ouvrir({ codeParticipant: '99004', correspondantId: CORRESPONDANT_99004 }, CLE)
    expect(o).toMatchObject({ etatSaga: 'TERMINEE', codeParticipant: '99004', etatEspace: 'OUVERT', motifEchec: null })
    expect(await s.ouvrir({ codeParticipant: '99004', correspondantId: CORRESPONDANT_99004 }, CLE)).toEqual(o)
    expect(await s.consulter('99004')).toMatchObject({ codeEspace: 'ESP-99004', etat: 'OUVERT', administrateursActifs: 1, statutRegistre: 'EN_HOMOLOGATION', dateOuverture: '2026-10-22T09:00:00.000Z' })
    expect((await refus(s.ouvrir({ codeParticipant: '99004', correspondantId: CORRESPONDANT_99004 }, 'autre-cle-123'))).code).toBe('SBX_ESP_ESPACE_EXISTANT')
  })

  it('ouverture refusée : institution absente, statut non admis, correspondant inconnu, saisie invalide', async () => {
    const s = service()
    expect((await refus(s.ouvrir({ codeParticipant: '12345', correspondantId: CORRESPONDANT_99004 }, CLE))).code).toBe('SBX_ESP_INTROUVABLE')
    expect((await refus(s.ouvrir({ codeParticipant: '99003', correspondantId: CORRESPONDANT_99004 }, 'cle-0002-abcdef'))).code).toBe('SBX_ESP_STATUT_NON_ADMIS')
    expect((await refus(s.ouvrir({ codeParticipant: '99005', correspondantId: CORRESPONDANT_99004 }, 'cle-0003-abcdef'))).status).toBe(422)
    expect((await refus(s.ouvrir({ codeParticipant: '9900', correspondantId: CORRESPONDANT_99004 }, CLE))).status).toBe(400)
    expect((await refus(s.ouvrir({ codeParticipant: '99004', correspondantId: 'x' }, CLE))).status).toBe(400)
    expect((await refus(s.ouvrir({} as never, CLE))).status).toBe(400)
    // Statuts admis de SBX-ESPACE (ParametresEspace) : un adhérent n'est pas admis.
    const adherent = espacesSimules({ permissions: ADMIN, registre: [{ codeParticipant: '99006', statut: 'ADHERENT', versionStatut: 1, correspondants: [CORRESPONDANT_99004] }] })
    expect((await refus(adherent.ouvrir({ codeParticipant: '99006', correspondantId: CORRESPONDANT_99004 }, CLE))).code).toBe('SBX_ESP_STATUT_NON_ADMIS')
  })

  it('écriture : clé absente (400) ou invalide (422), droit d’administration (404), second facteur (422)', async () => {
    expect((await refus(service().agir('99001', 'suspension', ''))).code).toBe('SBX_ESP_CLE_IDEMPOTENCE_ABSENTE')
    expect((await refus(service().agir('99001', 'suspension', 'court'))).code).toBe('SBX_ESP_CLE_IDEMPOTENCE_INVALIDE')
    expect((await refus(service({ permissions: [PERMISSION.consultation] }).agir('99001', 'suspension', CLE))).code).toBe('SBX_ESP_INTROUVABLE')
    expect((await refus(service({ institution: '99001' }).agir('99001', 'suspension', CLE))).status).toBe(404)
    expect((await refus(service({ deuxFacteurs: false }).agir('99001', 'suspension', CLE))).code).toBe('SBX_ESP_SECOND_FACTEUR_EXIGE')
    expect((await refus(service().agir('99999', 'suspension', CLE))).status).toBe(404)
  })

  it('suspension, rétablissement et fermeture selon l’état et le registre', async () => {
    const s = service()
    const suspendu = await s.agir('99001', 'suspension', 'cle-s-000001')
    expect(suspendu).toMatchObject({ etat: 'SUSPENDU', motifsSuspension: ['DECISION_GIMAC'], dateSuspension: '2026-10-22T09:00:00.000Z' })
    // Suspendre à nouveau : motif déjà présent, date inchangée.
    expect(await s.agir('99001', 'suspension', 'cle-s-000002')).toMatchObject({ etat: 'SUSPENDU', motifsSuspension: ['DECISION_GIMAC'] })
    expect(await s.agir('99001', 'retablissement', 'cle-r-000001')).toMatchObject({ etat: 'OUVERT', motifsSuspension: [], dateSuspension: null })
    expect((await refus(s.agir('99001', 'retablissement', 'cle-r-000002'))).code).toBe('SBX_ESP_ETAT_INCOMPATIBLE')
    expect((await refus(s.agir('99001', 'fermeture', 'cle-f-000001'))).code).toBe('SBX_ESP_ETAT_INCOMPATIBLE')
    // 99002 : suspendu par le registre, qui s'oppose au rétablissement ; non retiré, il ne se ferme pas.
    expect((await refus(s.agir('99002', 'retablissement', 'cle-r-000003'))).code).toBe('SBX_ESP_REGISTRE_OPPOSE')
    expect((await refus(s.agir('99002', 'fermeture', 'cle-f-000002'))).code).toBe('SBX_ESP_NON_RETIRE')
    const ferme = await s.agir('99003', 'fermeture', 'cle-f-000003')
    expect(ferme).toMatchObject({ etat: 'FERME', dateFermeture: '2026-10-22T09:00:00.000Z' })
    expect((await refus(s.agir('99003', 'suspension', 'cle-s-000003'))).code).toBe('SBX_ESP_ETAT_INCOMPATIBLE')
    // Clé rejouée : résultat d'origine, même après d'autres actes.
    expect(await s.agir('99003', 'fermeture', 'cle-f-000003')).toEqual(ferme)
  })

  it('jeux de départ copiés : le service ne modifie jamais les données de démonstration', async () => {
    const s = espacesSimules({ permissions: ADMIN, latenceMs: 1 })
    await s.agir('99001', 'suspension', CLE)
    expect(ESPACES_DEMO[0].etat).toBe('OUVERT')
    expect((await s.consulter('99001')).dateSuspension).not.toBeNull()
  })
})
