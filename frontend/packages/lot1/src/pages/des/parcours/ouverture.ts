import { aPermission, usePermissions, useSession } from '@gimac/socle'
import { PERM } from '../../../permissions'

// D-146 (décision du responsable du 13/11/2026) : le parcours de certification commence par le conducteur de projet.
// Ouvre une liaison le chef de département, qui désigne le conducteur (HOM_PROJECT_DESIGNATE, D-58), ou le conducteur
// de projet lui-même (HOM_PROJECT_CONDUCT), pour le seul projet qu'il conduit : MS-HOMOLOGATION lui refuse de désigner
// un autre conducteur. L'écran propose ; la passerelle et le service revérifient.

export type ConducteurImpose = { id: string; nom: string }

export function useOuvertureLiaison(): { peutOuvrir: boolean; conducteurImpose: ConducteurImpose | null } {
  const permissions = usePermissions()
  const session = useSession()
  const peutOuvrir = aPermission(permissions, PERM.liaisonOuvrir) && aPermission(permissions, PERM.projetDesigner, PERM.projetConduire)
  const impose = peutOuvrir && !aPermission(permissions, PERM.projetDesigner)
  return { peutOuvrir, conducteurImpose: impose ? { id: session?.sujet ?? '', nom: session?.nom ?? '' } : null }
}

/** Conducteurs proposés : la liste des habilités, ou le seul conducteur qui crée son projet. */
export function optionsConducteurs(impose: ConducteurImpose | null, habilites: { id: string; nom: string }[] | undefined) {
  return impose ? [{ value: impose.id, label: impose.nom }] : (habilites ?? []).map((c) => ({ value: c.id, label: c.nom }))
}
