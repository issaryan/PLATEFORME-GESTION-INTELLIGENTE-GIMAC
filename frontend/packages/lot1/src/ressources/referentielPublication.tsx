import type { ResourceActionLot1 } from '@gimac/socle'
import type { ReferentielVersion } from '../modele/homologation'
import type { Services } from '../services/ports'
import { ControlePublication, ImpactPublication } from '../pages/des/compositions/referentiel'

// Publication d'une version de référentiel (HOM-01) : proposée seulement si actionsPermises contient PUBLIER ;
// confirmation avec l'impact sur les liaisons en cours. Partagée par la fiche de la version et le modélisateur.
export const actionPublier: ResourceActionLot1<ReferentielVersion, Services> = {
  key: 'publier', label: 'referentiel.action.publier', icon: 'check', variant: 'primary', scope: 'row', permission: 'HOM_REFERENTIEL_PUBLISH', confirm: 'confirm',
  isAllowed: (r) => r.actionsPermises.includes('PUBLIER'),
  consequences: (r) => (
    <>
      <ControlePublication versionId={r.id} />
      <ImpactPublication versionId={r.versionActiveId ?? r.id} />
    </>
  ),
  run: async ({ row, services, invalidate }) => {
    await services.homologation.publierReferentiel(row.id)
    invalidate()
  },
}
