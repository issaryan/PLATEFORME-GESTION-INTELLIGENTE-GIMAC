import { describe, expect, it } from 'vitest'
import { servicesSimules } from '.'
import { magasinPouvoirs } from './pouvoirs'

const formulaire = {
  TYPE_DEMANDEUR: 'SUPPORT_MONETIQUE', ELEMENT_CONFIGURATION: 'AMA', IDENTIFIANT_EC: 'EC-1', PRIORITE: 'URGENT', DECLENCHEUR: 'CLIENT',
  DEMANDEUR_CONTACT: 'david@gimac.test', RAISON: 'Accès lecture', ELEMENT_IMPACTE: 'AMA, BD', DESCRIPTION_IMPACT: 'Nouvel accès',
  RESPONSABLE_CONTACT: 'alice@gimac.test', PLAN_TEST: 'Vérifier', PLAN_RETOUR: 'Restaurer',
}

describe('processus DEMANDE_CHANGEMENT simulé', () => {
  it('conserve le modèle, la référence datée et produit le PDF signé', async () => {
    const magasin = magasinPouvoirs()
    magasin.organisation.push({ id: 'u-ssi', nomAffiche: 'Sara SSI', fonction: 'Agent SSI', unite: 'SSI', responsableId: null, actif: true })
    magasin.definirProfils('u-ssi', ['ROLE_AGENT_SSI'])
    const demandeur = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-collab', nom: 'David Demandeur', profils: ['ROLE_COLLABORATEUR'] }, pouvoirs: magasin })
    const resume = (await demandeur.processus.lancables({})).groupes.flatMap((g) => g.processus).find((p) => p.code === 'DEMANDE_CHANGEMENT')!
    const modele = await demandeur.processus.lire(resume.id)
    expect(modele.referenceModele).toBe('CHG-{AAAA}-{MM}-{JJ}-{NNNNNN}')
    expect(modele.etapes.map((e) => e.code)).toEqual(['IDENTIFICATION', 'RAISON', 'IMPACT', 'IMPLEMENTATION', 'SSI'])
    expect(modele.formulaire?.champs).toHaveLength(12)
    const d = await demandeur.processus.lancer(modele.id, { intitule: 'Accès lecture AMA', champs: {}, designations: { IMPLEMENTATION: 'u-collab' }, formulaire })
    expect(d.reference).toMatch(/^CHG-\d{4}-\d{2}-\d{2}-\d{6}$/)
    for (const etape of ['IDENTIFICATION', 'RAISON', 'IMPACT']) await demandeur.processus.terminer(d.id, etape)
    const ticket = await demandeur.processus.televerserLivrable(new File(['%PDF-1.4'], 'dossier.pdf', { type: 'application/pdf' }), d.id)
    await demandeur.processus.deposerLivrable(d.id, 'IMPLEMENTATION', 'DOSSIER_CHANGEMENT', ticket.documentId)
    await demandeur.processus.terminer(d.id, 'IMPLEMENTATION')
    const ssi = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-ssi', nom: 'Sara SSI', profils: ['ROLE_AGENT_SSI'] }, pouvoirs: magasin })
    await ssi.processus.saisirChamps(d.id, 'SSI', { DATE_DEPLOIEMENT: '2026-11-02', DATE_REVUE: '2026-11-03', EVALUATION: 'Conforme', DECISION: 'ACCEPTEE', DESCRIPTION: 'Favorable' })
    await ssi.processus.terminer(d.id, 'SSI')
    const signee = await ssi.processus.signer(d.id, 'SSI', 'DECISION_SSI', 'assertion')
    expect(signee.etat).toBe('CLOSE')
    const pdf = await ssi.processus.telechargerPdf(d.id)
    expect(pdf.type).toBe('application/pdf')
    expect(await pdf.text()).toContain(d.reference)
    expect(await pdf.text()).toContain('DECISION_SSI')
  })
})
