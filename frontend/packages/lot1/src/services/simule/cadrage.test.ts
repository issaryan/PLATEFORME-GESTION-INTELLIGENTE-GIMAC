import { describe, expect, it } from 'vitest'
import { servicesSimules } from './index'

// Serveur de substitution de MS-HOMOLOGATION pour le cadrage (étape 3, D-57, D-58) : mêmes refus que
// OpenLiaisonService, « Mes projets », conducteurs, désignation, bascule de l'institution en homologation.
const refus = (code: string, details?: Record<string, unknown>) => expect.objectContaining(details ? { code, details } : { code })
const ouverture = { institutionId: 'INST-0105', typeProjet: 'PAIEMENT_MOBILE' as const, chefDeProjetId: 'U-202' }
const attendre = (ms: number) => new Promise((r) => setTimeout(r, ms))

describe('homologation simulée : cadrage', () => {
  it('ouvre la liaison au cadrage, puis l’institution passe en homologation', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const l = await s.homologation.ouvrirLiaison({ ...ouverture, banquePartenaireId: 'INST-0017' })
    expect(l).toMatchObject({
      etat: 'OUVERTE', phaseCourante: 'CADRAGE', institutionNom: 'Mobile Money Sanaga', chefDeProjetNom: 'Brice Ondo Mba', banquePartenaireNom: 'Banque B',
      versionId: 'REF-PM-2', perimetre: ['TRANSVERSE', 'TRANSFERT', 'PAIEMENT'], actionsPermises: ['DESIGNER_CHEF_DE_PROJET'],
    })
    expect(l.intervenants?.map((i) => i.role)).toEqual(['INSTITUTION', 'BANQUE_PARTENAIRE', 'CHEF_DE_PROJET'])
    expect((await s.homologation.lireLiaison(l.id)).id).toBe(l.id)
    await attendre(5)
    const d = await s.participant.dossier('INST-0105')
    expect(d.institution.statut).toBe('EN_HOMOLOGATION')
    expect(d.transitions[0]).toMatchObject({ automatique: true, motif: `Ouverture de la liaison ${l.id}` })
    // Une seconde liaison d'un autre type : l'institution, déjà en homologation, reste éligible.
    const l2 = await s.homologation.ouvrirLiaison({ ...ouverture, typeProjet: 'CARTE_INTERBANCAIRE' })
    expect(l2.banquePartenaireId).toBeNull()
    expect(l2.intervenants?.map((i) => i.role)).toEqual(['INSTITUTION', 'CHEF_DE_PROJET'])
  })
  it('refus : institution non éligible (statut en détail), inconnue, doublon', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const candidat = (await s.participant.listerInstitutions({ page: 1, taille: 1, statut: ['CANDIDAT'] })).contenu[0].id
    await expect(s.homologation.ouvrirLiaison({ ...ouverture, institutionId: candidat })).rejects.toEqual(refus('HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE', { statut: 'CANDIDAT' }))
    await expect(s.homologation.ouvrirLiaison({ ...ouverture, institutionId: 'INST-0061' })).rejects.toEqual(refus('HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE', { statut: 'SUSPENDU' }))
    await expect(s.homologation.ouvrirLiaison({ ...ouverture, institutionId: 'inconnue' })).rejects.toEqual(refus('HOM_LIAISON_INSTITUTION_NOT_ELIGIBLE', { statut: null }))
    await expect(s.homologation.ouvrirLiaison({ ...ouverture, institutionId: 'INST-0088' })).rejects.toEqual(refus('HOM_LIAISON_DUPLICATE'))
  })
  it('refus : conducteur non habilité, porteur qui n’est pas une banque, aucun référentiel publié', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    await expect(s.homologation.ouvrirLiaison({ ...ouverture, chefDeProjetId: 'U-101' })).rejects.toEqual(refus('HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED'))
    await expect(s.homologation.ouvrirLiaison({ ...ouverture, banquePartenaireId: 'INST-0042' })).rejects.toEqual(refus('HOM_LIAISON_CARRIER_NOT_BANK'))
    await expect(s.homologation.ouvrirLiaison({ ...ouverture, banquePartenaireId: 'inconnue' })).rejects.toEqual(refus('HOM_LIAISON_CARRIER_NOT_BANK'))
    await expect(s.homologation.ouvrirLiaison({ ...ouverture, typeProjet: 'DELEGATAIRE' })).rejects.toEqual(refus('HOM_REFERENTIAL_NONE_PUBLISHED'))
  })
  it('« Mes projets », filtres du service et conducteurs', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const mes = await s.homologation.listerLiaisons({ page: 1, taille: 25, filtres: { mesProjets: true } })
    expect(mes.contenu.map((l) => l.id)).toEqual(['LIA-2026-017', 'LIA-2026-030'])
    const cadrage = await s.homologation.listerLiaisons({ page: 1, taille: 25, filtres: { mesProjets: true, etat: 'OUVERTE' } })
    expect(cadrage.contenu.map((l) => l.phaseCourante)).toEqual(['CADRAGE'])
    expect((await s.homologation.listerLiaisons({ page: 1, taille: 25, filtres: { institutionId: 'INST-0017' } })).total).toBe(1)
    expect((await s.homologation.listerLiaisons({ page: 1, taille: 25, filtres: { mesProjets: undefined } })).total).toBe(4)
    expect((await s.homologation.conducteurs()).map((c) => c.nom)).toEqual(['Aïcha Mahamat Nour', 'Brice Ondo Mba', 'Estelle Ngo Bassong'])
  })
  it('désigne le conducteur (intervenants à jour), refuse un non-habilité et une liaison qui ne le permet plus', async () => {
    const s = servicesSimules({ latenceMs: 0 })
    const l = await s.homologation.designerChefDeProjet('LIA-2026-030', 'U-203', 'Réorganisation du portefeuille')
    expect(l).toMatchObject({ chefDeProjetId: 'U-203', chefDeProjetNom: 'Estelle Ngo Bassong' })
    expect(l.intervenants?.find((i) => i.role === 'CHEF_DE_PROJET')?.nom).toBe('Estelle Ngo Bassong')
    await expect(s.homologation.designerChefDeProjet('LIA-2026-030', 'U-001')).rejects.toEqual(refus('HOM_LIAISON_PROJECT_LEAD_NOT_HABILITATED'))
    await expect(s.homologation.designerChefDeProjet('LIA-2025-102', 'U-201')).rejects.toEqual(refus('HOM_LIAISON_STATE'))
    // Chaque instance a son propre état : la désignation ne fuit pas.
    expect((await servicesSimules({ latenceMs: 0 }).homologation.lireLiaison('LIA-2026-030')).chefDeProjetId).toBe('U-201')
  })
})
