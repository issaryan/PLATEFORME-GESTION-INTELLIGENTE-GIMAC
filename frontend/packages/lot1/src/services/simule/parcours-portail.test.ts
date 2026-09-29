import { describe, expect, it } from 'vitest'
import { servicesSimules } from './index'

// Projection du portail du participant (qualité Externe, CR §3.4) : contrat attendu du service pour
// l'institution du jeton (INST-0042) : tâches cloisonnées, geste et écran de chaque tâche, aucun nom
// interne, coordonnées relisibles, attestations attendues après le procès-verbal signé des deux parties.
const svc = () => servicesSimules({ latenceMs: 0 })
const refus = (code: string) => expect.objectContaining({ code })
const fichier = (nom: string) => new File(['contenu'], nom, { type: 'application/pdf' })
const INTERNES = /Conducteur de projet \d|Chargé DDRM|Ingénieur|Opérateur production|Chef du département|"U-/

describe('parcoursSimule : portail du participant', () => {
  it('tâches cloisonnées à l’institution du jeton, chacune avec son geste et son écran', async () => {
    const { portailParcours } = svc()
    const t = await portailParcours.mesTaches()
    expect(new Set(t.taches.map((x) => x.institutionNom))).toEqual(new Set(['Établissement de paiement A']))
    const par = (rang: string) => t.taches.find((x) => x.rang === rang)
    expect(par('19')).toMatchObject({ liaisonId: 'LIA-2026-027', lien: '/portail/liaisons/LIA-2026-027/proces-verbal', actionsPermises: ['SIGNER_PROCES_VERBAL'] })
    expect(par('12')).toMatchObject({ liaisonId: 'LIA-2026-046', lien: '/portail/liaisons/LIA-2026-046/coordonnees', actionsPermises: ['COMMUNIQUER_COORDONNEES'] })
    expect(par('32')).toMatchObject({ liaisonId: 'LIA-2025-088', lien: '/portail/liaisons/LIA-2025-088/fiche-suivi', actionsPermises: ['RENSEIGNER_FICHE_SUIVI'] })
    expect(t.compteurs.find((c) => c.cle === 'tachesARealiser')?.valeur).toBe(t.taches.length)
  })

  it('projection du dossier et du procès-verbal : intervenants du GIMAC par leur seule fonction', async () => {
    const { portailParcours } = svc()
    for (const id of ['LIA-2026-030', 'LIA-2026-027', 'LIA-2025-088', 'LIA-2026-046']) expect(JSON.stringify(await portailParcours.dossier(id))).not.toMatch(INTERNES)
    const pv = await portailParcours.procesVerbal('LIA-2026-027')
    expect(pv.etabliPar).toBe('CONDUCTEUR')
    expect(JSON.stringify(await portailParcours.approbation('LIA-2025-088'))).not.toMatch(INTERNES)
  })

  it('coordonnées d’essai : relisibles après communication, points de terminaison compris', async () => {
    const { portailParcours } = svc()
    expect((await portailParcours.dossier('LIA-2026-030')).coordonneesTechniques).toMatchObject({ adresse: 'participant-030.essai', port: '8443' })
    expect((await portailParcours.dossier('LIA-2026-046')).coordonneesTechniques).toBeNull()
    await portailParcours.communiquerCoordonnees('LIA-2026-046', { adresse: 'essai.exemple', port: '9443', pointsTerminaison: 'https://essai.exemple/tx' })
    expect((await portailParcours.dossier('LIA-2026-046')).coordonneesTechniques).toMatchObject({ adresse: 'essai.exemple', port: '9443', pointsTerminaison: 'https://essai.exemple/tx' })
    expect((await portailParcours.mesTaches()).taches.some((x) => x.rang === '12')).toBe(false)
  })

  it('attestations attendues seulement après le procès-verbal signé des deux parties', async () => {
    const { portailParcours, decision } = svc()
    expect((await portailParcours.approbation('LIA-2026-027')).actionsPermises).toEqual([])
    await expect(portailParcours.deposerAttestation('LIA-2026-027', 'PCI_DSS', fichier('pci.pdf'))).rejects.toEqual(refus('CER_PV_NOT_SIGNED'))
    await decision.signerProcesVerbalGimac('LIA-2026-027', 'a')
    await portailParcours.signerProcesVerbal('LIA-2026-027', 'a')
    expect((await portailParcours.dossier('LIA-2026-027')).statut).toBe('CERTIFIEE')
    expect((await portailParcours.approbation('LIA-2026-027')).actionsPermises).toEqual(['DEPOSER_ATTESTATION'])
    await expect(portailParcours.deposerAttestation('LIA-2026-027', 'PCI_DSS', new File(['x'], 'pci.docx'))).rejects.toEqual(refus('CER_ATTESTATION_FORMAT'))
    await portailParcours.deposerAttestation('LIA-2026-027', 'PCI_DSS', fichier('pci.pdf'))
    expect((await portailParcours.approbation('LIA-2026-027')).attestations[0].preuve?.nom).toBe('pci.pdf')
  })

  it('le service supplémentaire n’empêche pas l’ouverture d’une liaison principale', async () => {
    const { parcours } = svc()
    expect((await parcours.dossier('LIA-2026-046')).liaisonOrigineId).toBe('LIA-2025-088')
    await expect(parcours.ouvrirLiaison({ institutionId: 'INST-0042', typeProjet: 'PAIEMENT_MOBILE', chefDeProjetId: 'U-COND-1' })).resolves.toMatchObject({ statut: 'CADREE' })
  })
})
