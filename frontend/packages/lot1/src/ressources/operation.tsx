import { InlineAlert, StatusBadge, Tag, type Option } from '@gimac/design-system'
import type { ResourceDescriptorLot1 } from '@gimac/socle'
import type { OperationInstance } from '../modele/homologation'
import type { Services } from '../services/ports'
import { etatOperation } from '../dictionnaires'

// Descripteur des opérations d'une liaison (DES-09 ; HOM-07 à HOM-11). Déposer et clore mènent à
// l'assistant DES-10 ; attribuer et rouvrir sont confirmés ici avec motif.

type Traduire = (cle: string, valeurs?: Record<string, unknown>) => string

export const segmentOptions: Option[] = ['TRONC_COMMUN_AMONT', 'BRANCHE', 'TRONC_COMMUN_AVAL'].map((v) => ({ value: v, label: `segment.${v}` }))
export const partieOptions: Option[] = ['DES', 'PARTICIPANT'].map((v) => ({ value: v, label: `partie.${v}` }))

const cloture = (r: OperationInstance, etape: string) => `/des/liaisons/${r.liaisonId}/operations/${r.id}/cloture?etape=${etape}`

export function creerOperationDescriptor({ t, collaborateurs }: { t: Traduire; collaborateurs: Option[] }): ResourceDescriptorLot1<OperationInstance, Services> {
  return {
    key: 'operation',
    label: { singular: 'operation.singulier', plural: 'operation.pluriel', gender: 'f' },
    idField: 'code',
    titleField: 'libelle',
    statusField: 'etat',
    endpoints: { list: '/bff/des/homologation/liaisons/{liaisonId}/operations', get: '/bff/des/homologation/operations/{id}', update: '/bff/des/homologation/operations/{id}/responsable' },
    fields: [
      { name: 'code', label: 'operation.code', kind: 'mono', inList: true, inDetail: true, sortable: true, width: 110 },
      { name: 'libelle', label: 'operation.libelle', kind: 'text', inList: true, inDetail: true },
      { name: 'segment', label: 'operation.segment', kind: 'select', inList: true, inDetail: true, filterable: true, options: segmentOptions },
      { name: 'partie', label: 'operation.partie', kind: 'select', inList: true, inDetail: true, filterable: true, options: partieOptions },
      { name: 'etat', label: 'operation.etat', kind: 'status', inList: true, inDetail: true, filterable: true },
      { name: 'responsableId', label: 'operation.responsable', kind: 'select', inForm: 'edit', required: true, options: collaborateurs },
      { name: 'responsableNom', label: 'operation.responsable', kind: 'text', inList: true, inDetail: true },
      { name: 'echeance', label: 'operation.echeance', kind: 'date', inList: true, inDetail: true, sortable: true },
      {
        name: 'joursDepassement', label: 'operation.retard', kind: 'number', inList: true, inDetail: true,
        render: (_v, r) => (r.joursDepassement > 0 ? <StatusBadge status="warn" label={t('operation.retard.j', { count: r.joursDepassement })} /> : <span className="gm-field-help">{t('operation.retard.aucun')}</span>),
      },
      {
        name: 'predecesseursNonClos', label: 'operation.predecesseursNonClos', kind: 'custom', inDetail: true,
        render: (_v, r) =>
          r.predecesseursNonClos.length ? <span className="gm-row">{r.predecesseursNonClos.map((c) => <Tag key={c}>{c}</Tag>)}</span> : <span className="gm-field-help">{t('operation.predecesseurs.aucun')}</span>,
      },
    ],
    actions: [
      {
        key: 'cloturer', label: 'operation.action.cloturer', icon: 'pen-line', variant: 'primary', scope: 'row', permission: 'HOM_OPERATION_VALIDATE', confirm: 'none',
        isAllowed: (r) => r.actionsPermises.includes('VALIDER'), run: (ctx) => ctx.navigate(cloture(ctx.row, 'elements')),
      },
      {
        key: 'deposer', label: 'operation.action.deposer', icon: 'upload', scope: 'row', permission: 'HOM_EVIDENCE_SUBMIT', confirm: 'none',
        isAllowed: (r) => r.actionsPermises.includes('DEPOSER_PREUVE'), run: (ctx) => ctx.navigate(cloture(ctx.row, 'preuve')),
      },
      {
        key: 'attribuer', label: 'operation.action.attribuer', icon: 'user', scope: 'row', permission: 'HOM_OPERATION_ASSIGN', confirm: 'none',
        isAllowed: (r) => r.actionsPermises.includes('ATTRIBUER'), run: (ctx) => ctx.openForm('edit'),
      },
      {
        key: 'rouvrir', label: 'operation.action.rouvrir', icon: 'refresh', variant: 'danger', scope: 'row', permission: 'HOM_OPERATION_REOPEN', confirm: 'confirm', reason: true,
        isAllowed: (r) => r.actionsPermises.includes('ROUVRIR'),
        consequences: () => <InlineAlert level="warn">{t('operation.rouvrir.consequence')}</InlineAlert>,
        run: (ctx) => ctx.services.homologation.rouvrirOperation(ctx.row.id, ctx.motif ?? ''),
      },
    ],
    statusDictionary: etatOperation,
    permissions: { list: 'HOM_LIAISON_READ', read: 'HOM_LIAISON_READ', update: 'HOM_OPERATION_ASSIGN' },
    liveTopic: 'homologation',
  }
}
