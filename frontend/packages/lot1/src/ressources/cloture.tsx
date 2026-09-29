import { P, type ResourceDescriptorLot1 } from '@gimac/socle'
import type { ClotureOperation } from '../modele/homologation'
import type { Services } from '../services/ports'
import { etatOperation } from '../dictionnaires'
import { ConsequenceValidation } from '../pages/des/compositions/cloture'

// DES-10 : clôture d'une opération (HOM-09, HOM-10). La ressource est l'état des quatre éléments renvoyé
// par le service ; l'interface n'évalue jamais la distinction entre validateur, responsable et déposant.
export type ClotureAvecFichier = ClotureOperation & { preuve: File | null }

const O = `${P.desHomologation}/operations`

export const clotureOperationDescriptor: ResourceDescriptorLot1<ClotureAvecFichier, Services> = {
  key: 'clotureOperation',
  label: { singular: 'cloture.singulier', plural: 'cloture.pluriel', gender: 'f' },
  idField: 'code',
  titleField: 'libelle',
  statusField: 'etat',
  endpoints: { list: O, get: `${O}/{id}/elements-de-cloture` },
  steps: [
    { key: 'preuve', labelKey: 'cloture.etape.preuve', fields: ['preuveAttendue', 'preuve', 'dernierDepot'], action: 'deposer' },
    { key: 'elements', labelKey: 'cloture.etape.elements', fields: ['elements'] },
    { key: 'signature', labelKey: 'cloture.etape.signature', fields: ['etat', 'restrictionValidation'], action: 'valider' },
  ],
  fields: [
    { name: 'code', label: 'operation.code', kind: 'mono', inDetail: true },
    { name: 'libelle', label: 'operation.libelle', kind: 'text', inDetail: true },
    { name: 'etat', label: 'operation.etat', kind: 'status', inDetail: true },
    { name: 'preuveAttendue', label: 'cloture.preuveAttendue', kind: 'text', inForm: 'edit', readOnly: true, inDetail: true },
    { name: 'preuve', label: 'cloture.fichier', kind: 'file', inForm: 'edit', help: 'cloture.fichier.aide' },
  ],
  actions: [
    {
      key: 'deposer', label: 'cloture.action.deposer', icon: 'upload', variant: 'primary', scope: 'page', permission: 'HOM_EVIDENCE_SUBMIT',
      confirm: 'confirm', isAllowed: (r) => r.actionsPermises.includes('DEPOSER_PREUVE'),
      // Le format est contrôlé par le navigateur, le contenu par MS-DOCUMENT (antivirus, masquage) ; refus 422 explicite.
      run: async ({ row, services, invalidate }) => {
        if (!row.preuve) return
        await services.homologation.deposerPreuve(row.id, row.preuve, row.preuveAttendue)
        invalidate()
      },
    },
    {
      key: 'valider', label: 'cloture.action.valider', icon: 'pen-line', variant: 'primary', scope: 'page', permission: 'HOM_OPERATION_VALIDATE',
      confirm: 'sign', reason: true, isAllowed: (r) => r.actionsPermises.includes('VALIDER'),
      sign: { purpose: 'OPERATION_VALIDATE', empreinte: (r) => r.empreinteAValider ?? '' },
      consequences: () => <ConsequenceValidation />,
      run: async ({ row, motif, assertion, services, invalidate }) => {
        await services.homologation.validerOperation(row.id, { motif: motif ?? '', empreinte: row.empreinteAValider ?? '' }, assertion ?? '')
        invalidate()
      },
    },
  ],
  statusDictionary: etatOperation,
  permissions: { list: 'HOM_LIAISON_READ', read: 'HOM_LIAISON_READ', update: 'HOM_EVIDENCE_SUBMIT' },
  liveTopic: 'homologation',
}
