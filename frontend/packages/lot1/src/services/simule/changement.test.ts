import { describe, expect, it, vi } from 'vitest'
import { servicesSimules } from '.'
import { magasinPouvoirs } from './pouvoirs'

const formulaire = {
  TYPE_DEMANDEUR: 'SUPPORT_MONETIQUE', ELEMENT_CONFIGURATION: 'AMA', IDENTIFIANT_EC: 'EC-1', PRIORITE: 'URGENT', DECLENCHEUR: 'CLIENT',
  DEMANDEUR_CONTACT: 'david@gimac.test', RAISON: 'Accès lecture', ELEMENT_IMPACTE: 'AMA, BD', DESCRIPTION_IMPACT: 'Nouvel accès',
  RESPONSABLE_CONTACT: 'alice@gimac.test', PLAN_TEST: 'Vérifier', PLAN_RETOUR: 'Restaurer',
}

describe('processus DEMANDE_CHANGEMENT simulé', () => {
  it('conserve le modèle et la référence, refuse l’agent SSI, produit le PDF signé par le RSSI', async () => {
    const magasin = magasinPouvoirs()
    magasin.organisation.push({ id: 'u-ssi', nomAffiche: 'Sara SSI', fonction: 'RSSI', unite: 'SSI', responsableId: null, actif: true })
    magasin.definirProfils('u-ssi', ['ROLE_RSSI'])
    magasin.organisation.push({ id: 'u-agent-ssi', nomAffiche: 'Alain SSI', fonction: 'Agent SSI', unite: 'SSI', responsableId: null, actif: true })
    magasin.definirProfils('u-agent-ssi', ['ROLE_AGENT_SSI'])
    // u-ssi seul RSSI de l'organisation simulée : l'étape SSI lui revient d'office.
    for (const p of magasin.organisation) if (p.id !== 'u-ssi' && magasin.profilsDe(p.id).includes('ROLE_RSSI')) magasin.definirProfils(p.id, magasin.profilsDe(p.id).filter((x) => x !== 'ROLE_RSSI'))
    const demandeur = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-collab', nom: 'David Demandeur', profils: ['ROLE_COLLABORATEUR'] }, pouvoirs: magasin })
    const resume = (await demandeur.processus.lancables({})).groupes.flatMap((g) => g.processus).find((p) => p.code === 'DEMANDE_CHANGEMENT')!
    const modele = await demandeur.processus.lire(resume.id)
    expect(modele.referenceModele).toBe('CHG-{AAAA}-{N}')
    expect(modele.etapes.map((e) => e.code)).toEqual(['IDENTIFICATION', 'RAISON', 'IMPACT', 'IMPLEMENTATION', 'SSI'])
    expect(modele.formulaire?.champs).toHaveLength(12)
    const d = await demandeur.processus.lancer(modele.id, { intitule: 'Accès lecture AMA', champs: {}, designations: { IMPLEMENTATION: 'u-collab' }, formulaire })
    expect(d.reference).toMatch(/^[A-Z]{2,5}-\d{4}-\d{1,6}$/)
    for (const etape of ['IDENTIFICATION', 'RAISON', 'IMPACT']) await demandeur.processus.terminer(d.id, etape)
    const ticket = await demandeur.processus.televerserLivrable(new File(['%PDF-1.4'], 'dossier.pdf', { type: 'application/pdf' }), d.id)
    await demandeur.processus.deposerLivrable(d.id, 'IMPLEMENTATION', 'DOSSIER_CHANGEMENT', ticket.documentId)
    await demandeur.processus.terminer(d.id, 'IMPLEMENTATION')
    const agent = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-agent-ssi', nom: 'Alain SSI', profils: ['ROLE_AGENT_SSI'] }, pouvoirs: magasin })
    const vue = await demandeur.processus.instance(d.id)
    // D-138 : l'étape SSI est adressée au rôle RSSI (même avec un seul porteur), jamais figée sur la personne.
    expect(vue.etapes[4]).toMatchObject({ etat: 'ACTIVE', executant: null, aPrendre: { profil: 'ROLE_RSSI' } })
    await expect(agent.processus.saisirChamps(d.id, 'SSI', { DECISION: 'ACCEPTEE' })).rejects.toMatchObject({ status: 403, code: 'PIL_PROCESSUS_NON_INTERVENANT' })
    const ssi = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-ssi', nom: 'Sara SSI', profils: ['ROLE_RSSI'] }, pouvoirs: magasin })
    await ssi.processus.saisirChamps(d.id, 'SSI', { DATE_DEPLOIEMENT: '2026-11-02', DATE_REVUE: '2026-11-03', EVALUATION: 'Conforme', DECISION: 'ACCEPTEE', DESCRIPTION: 'Favorable' })
    await ssi.processus.terminer(d.id, 'SSI')
    await expect(agent.processus.signer(d.id, 'SSI', 'DECISION_SSI', 'assertion')).rejects.toMatchObject({ status: 403 })
    const signee = await ssi.processus.signer(d.id, 'SSI', 'DECISION_SSI', 'assertion')
    expect(signee.etat).toBe('CLOSE')
    // D-139 : exemple de démonstration rendu par la chaîne réelle, dans la langue demandée ; illisible : 503.
    const exemple = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response('%PDF-1.7 démonstration', { status: 200 }))
    const pdf = await ssi.processus.telechargerPdf(d.id, 'en')
    expect(pdf.type).toBe('application/pdf')
    expect(await pdf.text()).toContain('%PDF-1.7')
    expect(String(exemple.mock.calls[0][0])).toContain('instance-demonstration-en.pdf')
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(new Response('', { status: 500 }))
    await expect(ssi.processus.telechargerPdf(d.id)).rejects.toMatchObject({ status: 503, code: 'PIL_PDF_INDISPONIBLE' })
    await expect(ssi.processus.telechargerPdf('inconnue')).rejects.toMatchObject({ status: 404 })
  })

  it('D-142 : la mise en œuvre peut être confiée à toute personne interne active, hors Production monétique', async () => {
    const magasin = magasinPouvoirs()
    const demandeur = servicesSimules({ latenceMs: 0, utilisateur: { id: 'u-collab', nom: 'David Demandeur', profils: ['ROLE_COLLABORATEUR'] }, pouvoirs: magasin })
    const resume = (await demandeur.processus.lancables({})).groupes.flatMap((g) => g.processus).find((p) => p.code === 'DEMANDE_CHANGEMENT')!
    const modele = await demandeur.processus.lire(resume.id)
    expect(modele.etapes[3].intervenant).toMatchObject({ mode: 'AU_LANCEMENT', remplacement: null })
    // L'ingénieur du pôle réseau, sans le profil Production monétique, est désigné : la demande se lance.
    const d = await demandeur.processus.lancer(modele.id, { intitule: 'Accès lecture AMA', champs: {}, designations: { IMPLEMENTATION: 'u-res' }, formulaire })
    expect(d.designations.IMPLEMENTATION).toMatchObject({ id: 'u-res' })
  })
})

