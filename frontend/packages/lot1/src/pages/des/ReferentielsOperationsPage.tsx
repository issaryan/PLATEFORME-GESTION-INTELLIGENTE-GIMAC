import { useCallback, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Button, Card, EmptyState, ResourceDetail, ResourceForm, TreeView, type ResourceDescriptor, PageHeader } from '@gimac/design-system'
import { EtatsRequete, usePermissions } from '@gimac/socle'
import { useEffet } from '../../hooks/base'
import { useHistoriqueReferentiel, useReferentiel, useReferentiels } from '../../hooks/referentiels'
import { useDescripteurDS } from '../../ressources/descripteur'
import { useExecuteurActions } from '../../ressources/actions'
import { referentielOperationsDescriptor } from '../../ressources/referentiel'
import { versArbre } from '../../adaptateurs/referentiel'
import type { ReferentielVersion } from '../../modele/homologation'

// DES-12 Référentiels d'opérations (gabarit G5) : arbre des versions à gauche, version sélectionnée à droite.
// La version (?version=) et l'onglet (?onglet=) sont dans l'adresse.
export default function ReferentielsOperationsPage() {
  const { t } = useTranslation()
  const [params, setParams] = useSearchParams()
  const versionId = params.get('version') ?? ''
  const onglet = params.get('onglet') ?? 'operations'
  const [creation, setCreation] = useState(false)
  const executeur = useExecuteurActions()
  const versions = useReferentiels()
  const creer = referentielOperationsDescriptor.actions.find((a) => a.key === 'creer')!
  const choisir = (id: string) => {
    if (id.startsWith('type:')) return
    setCreation(false)
    setParams({ version: id })
  }
  const ouvrirFormulaire = useCallback(() => setCreation(true), [])
  const ouvrirOnglet = useCallback((k: string) => setParams((p) => ({ ...Object.fromEntries(p), onglet: k })), [setParams])

  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
      <PageHeader title={t('referentiel.titre')} actions={<Button variant="primary" icon="plus" onClick={() => executeur.lancer(creer, null as unknown as ReferentielVersion, t('referentiel.pluriel'), 'homologation', { openForm: ouvrirFormulaire })}>
          {t(creer.label)}
        </Button>} />
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(240px, 320px) minmax(0, 1fr)', gap: 'var(--gm-space-5)', alignItems: 'start' }}>
        <Card title={t('referentiel.arbre')}>
          <EtatsRequete
            etat={versions}
            emptyTitle={t('referentiel.aucun')}
            errorTitle={t('commun.erreur.titre')}
            retryLabel={t('commun.reessayer')}
            isEmpty={(v) => v.length === 0}
          >
            {(v) => {
              const noeuds = versArbre(v, t)
              return <TreeView label={t('referentiel.arbre')} nodes={noeuds} expanded={noeuds.map((n) => n.key)} selected={versionId || undefined} onSelect={choisir} />
            }}
          </EtatsRequete>
        </Card>
        <section aria-label={t('referentiel.detail')}>
          {versionId ? (
            <Version key={`${versionId}-${onglet}`} id={versionId} onglet={onglet} ouvrirOnglet={ouvrirOnglet} ouvrirFormulaire={ouvrirFormulaire} />
          ) : (
            <EmptyState icon="kanban" title={t('referentiel.vide.titre')} text={t('referentiel.vide.texte')} />
          )}
        </section>
      </div>
      {/* Création d'un brouillon : fenêtre modale ouverte sur l'arbre des versions. */}
      {creation ? <FormulaireBrouillon onFin={(id) => { setCreation(false); if (id) setParams({ version: id }) }} /> : null}
    </div>
  )
}

function Version({ id, onglet, ouvrirOnglet, ouvrirFormulaire }: { id: string; onglet: string; ouvrirOnglet: (k: string) => void; ouvrirFormulaire: () => void }) {
  const { t } = useTranslation()
  const q = useReferentiel(id)
  const historique = useHistoriqueReferentiel(id)
  const pilotes = useMemo(() => ({ openTab: ouvrirOnglet, openForm: ouvrirFormulaire }), [ouvrirOnglet, ouvrirFormulaire])
  const desc = useDescripteurDS(referentielOperationsDescriptor, pilotes)
  const permissions = usePermissions()
  return (
    <EtatsRequete etat={q} emptyTitle={t('erreur.introuvable')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
      {(v) => (
        <ResourceDetail<ReferentielVersion>
          descriptor={desc}
          row={v}
          mode="page"
          tab={onglet}
          permissions={permissions}
          timeline={historique.data ?? []}
          onAction={(a, row) => a.run?.({ row: row ?? v, descriptor: desc })}
        />
      )}
    </EtatsRequete>
  )
}

/** Création d'un brouillon : formulaire du catalogue piloté par le descripteur (champs inForm « create »). */
function FormulaireBrouillon({ onFin }: { onFin: (id?: string) => void }) {
  const { t } = useTranslation()
  const desc = useDescripteurDS(referentielOperationsDescriptor)
  const creer = useEffet((s, v: { typeProjet: string; libelle: string }) => s.homologation.creerBrouillon(v), [['homologation']])
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  return (
    <ResourceForm<ReferentielVersion>
      descriptor={desc as ResourceDescriptor<ReferentielVersion>}
      mode="create"
      title={t('referentiel.action.creer')}
      presentation="dialog"
      submitting={creer.isPending}
      errors={erreurs}
      serverError={creer.error ?? undefined}
      submitLabel={t('referentiel.action.creer')}
      onCancel={() => onFin()}
      onSubmit={(vals) => {
        const typeProjet = String(vals.typeProjet ?? '')
        const libelle = String(vals.libelle ?? '').trim()
        const e: Record<string, string> = {}
        if (!typeProjet) e.typeProjet = t('commun.champObligatoire')
        if (!libelle) e.libelle = t('commun.champObligatoire')
        setErreurs(e)
        if (Object.keys(e).length) return
        creer.mutate({ typeProjet, libelle }, { onSuccess: (v) => onFin((v as ReferentielVersion).id) })
      }}
    />
  )
}
