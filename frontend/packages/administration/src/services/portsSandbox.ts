import type { CompteProtege, DemandePrestataire, EntreeRegistreSandbox, EtatPerimetre, PageRebutSandbox, PageSandbox, PerimetreSuspendu, TypeDelegation, VueDelegation } from '../modele/sandbox'

/**
 * Sandbox GIMAC dans la console (D-119, docs/lot1/sandbox-existant-adapte.md §6) : lectures de MS-IAM sous
 * IAM_ACCOUNT_READ. Les suspensions de périmètre et les protections sont posées par les sagas de SBX-ESPACE seulement.
 * Pages à partir de 0 côté serveur ; les écrans comptent à partir de 1.
 */
export interface ServiceSandboxIam {
  perimetres(page: number, taille: number): Promise<PageSandbox<PerimetreSuspendu>>
  perimetre(code: string): Promise<EtatPerimetre>
  protections(page: number, taille: number): Promise<PageSandbox<CompteProtege>>
  /** Délégations d'un type (toutes sans type), les plus récentes d'abord. */
  delegations(type: TypeDelegation | undefined, page: number, taille: number): Promise<PageSandbox<VueDelegation>>
  /** US-EXI-24 : demande d'accès de prestataire (IAM_ACCOUNT_SECURITY), en attente de l'approbation du RSSI. */
  demanderPrestataire(d: DemandePrestataire): Promise<VueDelegation>
  /** Approbation par le RSSI ; jamais par l'auteur de la demande (403 IAM_SBX_APPROBATION_PROPRE). */
  approuverPrestataire(id: string): Promise<VueDelegation>
  refuserPrestataire(id: string, motif: string): Promise<VueDelegation>
  revoquerPrestataire(id: string, motif: string): Promise<VueDelegation>
  /** US-EXI-10 : registre d'audit de MS-AUDIT filtré sur la source sandbox et l'institution (SBX_TRA_CONSULTATION). */
  registre(institution: string | undefined, page: number, taille: number): Promise<PageSandbox<EntreeRegistreSandbox>>
  /** US-EXI-10 : export au format ouvert (CSV, UTF-8). */
  exporterRegistre(institution: string | undefined): Promise<Blob>
  /** US-EXI-10 : enveloppes écartées au rebut avec leur motif, seuil d'alerte. */
  rebut(page: number, taille: number): Promise<PageRebutSandbox>
}
