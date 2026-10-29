import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Card, DataTable, Drawer, ProgressRing, StatusBadge, Tag, type Column, type Status } from '@gimac/design-system'
import { dateHeure } from '@gimac/compte'
import { useEvenementsCharge, useNoeuds, usePodsCharge, useTaches } from '../hooks'
import type { Charge, EtatTache, EvenementCharge, NoeudCluster, Orchestrateur, PodCharge, TacheCluster } from '../modele/infrastructure'
import type { PeriodeSerie } from '../modele/plateforme'
import { Donnees } from './communs'
import { octets } from './formats'
import { GraphiqueMetrique, SelecteurPeriode } from './graphiquesSupervision'
import { STATUT_CHARGE } from './widgetsPlateforme'

// Exploitation Kubernetes, lecture (docs/exploitation/operations-infrastructure.md §3) : fiche d'une charge
// (courbes ; sous Kubernetes, ses pods et ses événements), nœuds du cluster, jobs et cronjobs. Sous Docker,
// seules les courbes s'affichent : les autres lectures n'ont pas d'équivalent (422 côté passerelle).

const STATUT_TACHE: Record<EtatTache, Status> = { EN_ATTENTE: 'neutral', EN_COURS: 'info', REUSSIE: 'ok', ECHEC: 'crit' }

function PodsCharge({ nom }: { nom: string }) {
  const { t, i18n } = useTranslation()
  const pods = usePodsCharge(nom)
  const colonnes: Column<PodCharge>[] = [
    { key: 'nom', header: t('k8s.pod'), render: (p) => <span className="gm-mono">{p.nom}</span> },
    {
      key: 'etat', header: t('k8s.etat'),
      render: (p) => <StatusBadge status={p.pret ? 'ok' : p.attente ? 'crit' : 'warn'} label={p.attente ?? p.phase ?? t('k8s.inconnu')} />,
    },
    { key: 'redemarrages', header: t('infra.redemarrages'), type: 'number', render: (p) => p.redemarrages },
    { key: 'noeud', header: t('k8s.noeud'), render: (p) => <span className="gm-mono">{p.noeud ?? '-'}</span> },
    { key: 'demarreLe', header: t('infra.demarreeLe'), render: (p) => dateHeure(p.demarreLe, i18n.language) },
  ]
  return (
    <Donnees requete={pods}>
      {(l) => <DataTable columns={colonnes} rows={l} rowKey={(p) => p.nom} density="compact" emptyText={t('k8s.aucunPod')} state={l.length ? 'ok' : 'empty'} />}
    </Donnees>
  )
}

function EvenementsCharge({ nom }: { nom: string }) {
  const { t, i18n } = useTranslation()
  const evenements = useEvenementsCharge(nom)
  const colonnes: Column<EvenementCharge>[] = [
    { key: 'instant', header: t('k8s.quand'), render: (e) => dateHeure(e.instant, i18n.language) },
    { key: 'type', header: t('k8s.type'), render: (e) => <StatusBadge status={e.type === 'Warning' ? 'warn' : 'info'} label={e.type ?? '-'} /> },
    { key: 'raison', header: t('k8s.raison'), render: (e) => <span className="gm-mono">{e.raison ?? '-'}</span> },
    { key: 'message', header: t('k8s.message'), render: (e) => e.message ?? '-' },
    { key: 'objet', header: t('k8s.objet'), render: (e) => <span className="gm-mono">{e.objet}</span> },
    { key: 'nombre', header: t('k8s.nombre'), type: 'number', render: (e) => e.nombre },
  ]
  return (
    <Donnees requete={evenements}>
      {(l) => <DataTable columns={colonnes} rows={l} rowKey={(e) => `${e.instant}-${e.objet}-${e.raison}`} density="compact" emptyText={t('k8s.aucunEvenement')} state={l.length ? 'ok' : 'empty'} />}
    </Donnees>
  )
}

/** Fiche d'une charge : courbes (période propre) ; sous Kubernetes, ses pods et ses événements. */
export function FicheCharge({ charge, orchestrateur, fermer }: { charge: Charge; orchestrateur: Orchestrateur; fermer: () => void }) {
  const { t } = useTranslation()
  const [periode, setPeriode] = useState<PeriodeSerie>('UNE_HEURE')
  const k8s = orchestrateur === 'KUBERNETES'
  const onglets = [{ key: 'courbes', label: t('graphiques.fiche.onglet') }, ...(k8s ? [{ key: 'pods', label: t('k8s.pods') }, { key: 'evenements', label: t('k8s.evenements') }] : [])]
  return (
    <Drawer
      type={t('infra.charge')}
      reference={charge.nom}
      status={STATUT_CHARGE[charge.etat]}
      statusLabel={t(`infra.etat.${charge.etat}`)}
      title={t('infra.courbes.titre', { nom: charge.nom })}
      size="lg"
      tabs={onglets}
      onClose={fermer}
      render={(onglet) =>
        onglet === 'pods' ? (
          <PodsCharge nom={charge.nom} />
        ) : onglet === 'evenements' ? (
          <EvenementsCharge nom={charge.nom} />
        ) : (
          <div className="gm-col" style={{ gap: 'var(--gm-space-4)' }}>
            <SelecteurPeriode valeur={periode} onChange={setPeriode} />
            <GraphiqueMetrique metrique="CHARGE_CPU" periode={periode} cible={charge.nom} titre={t('graphiques.court.CHARGE_CPU')} />
            <GraphiqueMetrique metrique="CHARGE_MEMOIRE" periode={periode} cible={charge.nom} titre={t('graphiques.court.CHARGE_MEMOIRE')} />
          </div>
        )
      }
    />
  )
}

/** Nœuds du cluster : part des nœuds prêts, puis le détail de chacun. */
export function CarteNoeuds() {
  const { t } = useTranslation()
  const noeuds = useNoeuds()
  const colonnes: Column<NoeudCluster>[] = [
    { key: 'nom', header: t('k8s.noeud'), render: (n) => <span className="gm-mono">{n.nom}</span> },
    {
      key: 'pret', header: t('k8s.etat'),
      render: (n) => (
        <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
          <StatusBadge status={n.pret ? 'ok' : 'crit'} label={t(n.pret ? 'k8s.pret' : 'k8s.nonPret')} />
          {n.nonPlanifiable ? <Tag>{t('k8s.cordon')}</Tag> : null}
        </span>
      ),
    },
    { key: 'roles', header: t('k8s.roles'), render: (n) => (n.roles.length ? n.roles.join(', ') : '-') },
    { key: 'versionKubelet', header: t('k8s.kubelet'), render: (n) => <span className="gm-mono">{n.versionKubelet ?? '-'}</span> },
    { key: 'cpuAllouable', header: t('k8s.cpuAllouable'), render: (n) => <span className="gm-mono">{n.cpuAllouable ?? '-'}</span> },
    { key: 'memoireAllouableOctets', header: t('k8s.memoireAllouable'), type: 'number', render: (n) => octets(n.memoireAllouableOctets) },
  ]
  return (
    <Card title={t('k8s.noeuds')}>
      <Donnees requete={noeuds}>
        {(l) => {
          const prets = l.filter((n) => n.pret).length
          return (
            <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-6)', flexWrap: 'wrap' }}>
              <ProgressRing
                label={t('k8s.noeudsPrets')}
                value={l.length ? Math.round((prets * 100) / l.length) : null}
                status={prets === l.length ? 'ok' : 'warn'}
                statusLabel={t('k8s.noeudsPrets.libelle', { prets, total: l.length })}
              />
              <div style={{ flex: '1 1 520px', minWidth: 0 }}>
                <DataTable columns={colonnes} rows={l} rowKey={(n) => n.nom} density="compact" emptyText={t('k8s.aucunNoeud')} state={l.length ? 'ok' : 'empty'} />
              </div>
            </div>
          )
        }}
      </Donnees>
    </Card>
  )
}

/** CronJobs et Jobs de l'espace de noms. */
export function CarteTaches() {
  const { t, i18n } = useTranslation()
  const taches = useTaches()
  const colonnes: Column<TacheCluster>[] = [
    { key: 'nom', header: t('k8s.tache'), render: (x) => <span className="gm-mono">{x.nom}</span> },
    { key: 'nature', header: t('k8s.nature'), render: (x) => <Tag>{x.nature === 'CRONJOB' ? 'CronJob' : 'Job'}</Tag> },
    {
      key: 'etat', header: t('k8s.etat'),
      render: (x) =>
        x.nature === 'CRONJOB' ? (
          <StatusBadge status={x.suspendue ? 'warn' : 'ok'} label={t(x.suspendue ? 'k8s.suspendue' : 'k8s.active')} />
        ) : x.etat ? (
          <StatusBadge status={STATUT_TACHE[x.etat]} label={t(`k8s.etatTache.${x.etat}`)} />
        ) : '-',
    },
    { key: 'planification', header: t('k8s.planification'), render: (x) => <span className="gm-mono">{x.planification ?? x.origine ?? '-'}</span> },
    { key: 'debut', header: t('k8s.debut'), render: (x) => dateHeure(x.debut, i18n.language) },
    { key: 'fin', header: t('k8s.fin'), render: (x) => dateHeure(x.fin, i18n.language) },
  ]
  return (
    <Card title={t('k8s.taches')}>
      <Donnees requete={taches}>
        {(l) => <DataTable columns={colonnes} rows={l} rowKey={(x) => `${x.nature}-${x.nom}`} density="compact" emptyText={t('k8s.aucuneTache')} state={l.length ? 'ok' : 'empty'} />}
      </Donnees>
    </Card>
  )
}
