import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { DagView, EmptyState, GanttView, PageSection } from '@gimac/design-system'
import { useOperationsParcours } from '../../../../hooks/parcours'
import type { DossierLiaison, OperationParcours } from '../../../../modele/parcours'
import { grapheDuDossier, planningDuDossier } from '../../../../adaptateurs/parcoursVues'
import { Donnees } from '../../liaisons/communs'

// Onglets « Graphe » et « Planning » du dossier d'une liaison (demande du 19/10/2026, D-112) : où en est
// exactement la liaison, opération par opération dans les étapes de sa version (DagView, vue liste équivalente
// incluse), et le diagramme de Gantt des étapes du projet (GanttView, vue tableau équivalente incluse).

const aujourdhui = () => new Date().toISOString().slice(0, 10)

export function OngletGraphe({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const q = useOperationsParcours(dossier.id)
  return (
    <Donnees requete={q} estVide={(l) => l.length === 0} vide={t('parcoursOps.vide')}>
      {(ops) => <Graphe dossier={dossier} ops={ops} />}
    </Donnees>
  )
}

function Graphe({ dossier, ops }: { dossier: DossierLiaison; ops: OperationParcours[] }) {
  const { t } = useTranslation()
  const vue = useMemo(() => grapheDuDossier(dossier, ops, t, aujourdhui()), [dossier, ops, t])
  return (
    <PageSection title={t('parcoursVues.graphe.titre')} description={t('parcoursVues.graphe.aide')}>
      <DagView {...vue} />
    </PageSection>
  )
}

export function OngletPlanning({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const q = useOperationsParcours(dossier.id)
  return (
    <Donnees requete={q} estVide={(l) => l.length === 0} vide={t('parcoursOps.vide')}>
      {(ops) => <Planning dossier={dossier} ops={ops} />}
    </Donnees>
  )
}

function Planning({ dossier, ops }: { dossier: DossierLiaison; ops: OperationParcours[] }) {
  const { t } = useTranslation()
  const { vide, ...gantt } = useMemo(() => planningDuDossier(dossier, ops, t, aujourdhui()), [dossier, ops, t])
  return (
    <PageSection title={t('parcoursVues.planning.titre')} description={t('parcoursVues.planning.aide')}>
      {vide ? <EmptyState title={t('parcoursVues.planning.vide')} /> : <GanttView {...gantt} />}
    </PageSection>
  )
}
