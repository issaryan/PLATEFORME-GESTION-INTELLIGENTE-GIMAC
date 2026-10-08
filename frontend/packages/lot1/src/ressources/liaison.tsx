import type { ReactNode } from 'react'
import { InlineAlert, Link, ProgressRing, StatusBadge, type Option } from '@gimac/design-system'
import type { ResourceDescriptorLot1 } from '@gimac/socle'
import type { Liaison, LiaisonPilotage } from '../modele/homologation'
import type { Services } from '../services/ports'
import { etatLiaison } from '../dictionnaires'
import { taux } from '../adaptateurs/liaison'

// Descripteurs de la liaison (DES-08, DES-09 ; HOM-04, HOM-06, HOM-20, HOM-22). Libellés : clés i18n.
// Les actions n'agissent que par ctx.services ; leur disponibilité reflète actionsPermises du service.

type Traduire = (cle: string, valeurs?: Record<string, unknown>) => string
export type OptionsLiaison = {
  t: Traduire
  /** Conducteurs de projet habilités, proposés à la désignation. */
  conducteurs: Option[]
  /** HOM_LIAISON_OPEN et HOM_PROJECT_DESIGNATE réunies : l'ouverture (depuis le registre) est proposée. */
  ouverture?: boolean
}

export const typeProjetOptions: Option[] = ['CARTE_INTERBANCAIRE', 'PAIEMENT_MOBILE', 'DELEGATAIRE'].map((v) => ({ value: v, label: `typeProjet.${v}` }))

/** États à partir desquels le dossier de mise en service existe (navigation, pas une règle). */
const ETATS_CERTIFICATION = ['APTE_CERTIFICATION', 'MISE_EN_SERVICE_AUTORISEE', 'CERTIFIEE']

const chiffre = (c: Liaison['couvertureCertification'], t: Traduire): ReactNode =>
  c.calculable ? (
    <span className="gm-num">
      {c.taux} % <span className="gm-field-help">{`${c.numerateur}/${c.denominateur}`}</span>
    </span>
  ) : (
    <span className="gm-field-help">{t('couverture.nc')}</span>
  )

const anneau = (c: Liaison['couvertureCertification'], label: string): ReactNode => (
  <ProgressRing label={label} value={taux(c)} size={48} />
)

export function creerLiaisonDescriptor({ t, conducteurs, ouverture }: OptionsLiaison): ResourceDescriptorLot1<Liaison, Services> {
  return {
    key: 'liaison',
    label: { singular: 'liaison.singulier', plural: 'liaison.pluriel', gender: 'f' },
    idField: 'id',
    titleField: 'institutionNom',
    statusField: 'etat',
    endpoints: { list: '/bff/des/homologation/liaisons', get: '/bff/des/homologation/liaisons/{id}', create: '/bff/des/homologation/liaisons', update: '/bff/des/homologation/liaisons/{id}/chef-de-projet' },
    fields: [
      {
        name: 'institutionNom', label: 'liaison.institution', kind: 'text', inList: true, inDetail: true, sortable: true,
        render: (_v, r) => <Link href={`/des/liaisons/${r.id}`}>{r.institutionNom}</Link>,
      },
      { name: 'id', label: 'liaison.reference', kind: 'mono', inDetail: true },
      { name: 'typeProjet', label: 'liaison.typeProjet', kind: 'select', options: typeProjetOptions, inList: true, inDetail: true, filterable: true,
        render: (_v, r) => t(`typeProjet.${r.typeProjet}`, { defaultValue: String(r.typeProjet) }) },
      { name: 'chefDeProjetId', label: 'liaison.chefDeProjet', kind: 'select', required: true, options: conducteurs, inForm: 'edit', help: 'liaison.chefDeProjet.aide' },
      { name: 'chefDeProjetNom', label: 'liaison.chefDeProjet', kind: 'text', inList: true, inDetail: true },
      { name: 'banquePartenaireNom', label: 'liaison.portage', kind: 'text', inDetail: true },
      { name: 'etat', label: 'liaison.etat', kind: 'status', inList: true, inDetail: true, filterable: true },
      { name: 'phaseCourante', label: 'liaison.phase', kind: 'text', inList: true, inDetail: true, render: (_v, r) => t(`phase.${r.phaseCourante}`, { defaultValue: r.phaseCourante }) },
      // Liste : taux et ratio en chiffres tabulaires ; fiche : anneau de couverture (le catalogue n'a pas
      // d'anneau compact sans légende pour une cellule de tableau).
      { name: 'couvertureCertification', label: 'couverture.certification', kind: 'custom', inList: true, render: (_v, r) => chiffre(r.couvertureCertification, t) },
      { name: 'couvertureMiseEnService', label: 'couverture.miseEnService', kind: 'custom', inList: true, render: (_v, r) => chiffre(r.couvertureMiseEnService, t) },
      { name: 'couvertureCertification', label: 'couverture.certification', kind: 'custom', inDetail: true, render: (_v, r) => anneau(r.couvertureCertification, t('couverture.certification')) },
      { name: 'couvertureMiseEnService', label: 'couverture.miseEnService', kind: 'custom', inDetail: true, render: (_v, r) => anneau(r.couvertureMiseEnService, t('couverture.miseEnService')) },
      {
        name: 'operationsEnRetard', label: 'liaison.retards', kind: 'number', inList: true, sortable: true,
        render: (_v, r) => (r.operationsEnRetard > 0 ? <StatusBadge status="warn" label={t('liaison.retards.n', { count: r.operationsEnRetard })} /> : <span className="gm-field-help">{t('liaison.retards.aucun')}</span>),
      },
      { name: 'versionNumero', label: 'liaison.version', kind: 'number', inDetail: true },
      { name: 'dateOuverture', label: 'liaison.dateOuverture', kind: 'date', inDetail: true, sortable: true },
      { name: 'motifEtat', label: 'liaison.motifEtat', kind: 'text', inDetail: true },
    ],
    actions: [
      // Étape 3 (D-57) : une liaison s'ouvre depuis la fiche d'une institution adhérente ou en homologation.
      ...(ouverture
        ? [{
            key: 'ouvrir', label: 'liaison.action.ouvrir', icon: 'plus', variant: 'primary' as const, scope: 'page' as const, permission: 'HOM_LIAISON_OPEN', confirm: 'none' as const,
            run: (ctx: { navigate: (to: string) => void }) => ctx.navigate('/des/institutions?statut=ADHERENT'),
          }]
        : []),
      {
        key: 'chefDeProjet', label: 'liaison.action.designerChef', icon: 'user', scope: 'row', permission: 'HOM_PROJECT_DESIGNATE', confirm: 'none',
        isAllowed: (r) => r.actionsPermises.includes('DESIGNER_CHEF_DE_PROJET'), run: (ctx) => ctx.openForm('edit'),
      },
      {
        key: 'degel', label: 'liaison.action.degeler', icon: 'lock', variant: 'danger', scope: 'row', permission: 'HOM_LIAISON_UNFREEZE', confirm: 'confirm', reason: true,
        isAllowed: (r) => r.actionsPermises.includes('DEGELER'),
        consequences: (r) => <InlineAlert level="info">{t('liaison.degel.consequence', { motif: r.motifEtat ?? '' })}</InlineAlert>,
        run: (ctx) => ctx.services.homologation.degelerLiaison(ctx.row.id, ctx.motif ?? ''),
      },
      {
        key: 'exporterPreuves', label: 'liaison.action.exporterPreuves', icon: 'download', scope: 'row', permission: 'HOM_EVIDENCE_FILE_EXPORT', confirm: 'confirm',
        isAllowed: (r) => r.actionsPermises.includes('EXPORTER_PREUVES'),
        run: async (ctx) => {
          await ctx.services.homologation.exporterDossierDePreuves(ctx.row.id)
          ctx.openTab('preuves')
        },
      },
      {
        key: 'miseEnService', label: 'liaison.action.miseEnService', icon: 'arrow-right', scope: 'row', permission: 'CER_DOSSIER_READ', confirm: 'none',
        isAllowed: (r) => ETATS_CERTIFICATION.includes(r.etat),
        run: (ctx) => ctx.navigate(`/des/liaisons/${ctx.row.id}/mise-en-service`),
      },
    ],
    statusDictionary: etatLiaison,
    permissions: { list: 'HOM_LIAISON_LIST', read: 'HOM_LIAISON_READ', create: 'HOM_LIAISON_OPEN', update: 'HOM_PROJECT_DESIGNATE' },
    liveTopic: 'homologation',
  }
}

/** Tableau de bord de pilotage (HOM-20) : mêmes colonnes et délai consommé de la phase courante. */
export function creerPilotageDescriptor(o: OptionsLiaison): ResourceDescriptorLot1<LiaisonPilotage, Services> {
  const base = creerLiaisonDescriptor(o) as unknown as ResourceDescriptorLot1<LiaisonPilotage, Services>
  return {
    ...base,
    key: 'liaisonPilotage',
    actions: base.actions.filter((a) => a.key === 'miseEnService'),
    fields: [
      ...base.fields.filter((f) => f.inList),
      {
        name: 'delaiConsomme', label: 'pilotage.delai', kind: 'custom', inList: true, sortable: true,
        render: (_v, r) => (
          <StatusBadge status={r.delaiConsomme > r.delaiStandard ? 'warn' : 'neutral'} label={o.t('pilotage.delai.valeur', { consomme: r.delaiConsomme, standard: r.delaiStandard })} />
        ),
      },
    ],
    permissions: { list: 'HOM_DASHBOARD_READ', read: 'HOM_LIAISON_READ' },
  }
}
