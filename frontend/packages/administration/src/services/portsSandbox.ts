import type { CompteProtege, EtatPerimetre, PageSandbox, PerimetreSuspendu, TypeDelegation, VueDelegation } from '../modele/sandbox'

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
}
