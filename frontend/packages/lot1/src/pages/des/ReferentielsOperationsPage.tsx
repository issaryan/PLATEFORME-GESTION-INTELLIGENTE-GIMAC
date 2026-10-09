import { useCallback, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Button, Card, EmptyState, ResourceDetail, ResourceForm, TreeView, type ResourceDescriptor, PageHeader } from '@gimac/design-system'
import { EtatsRequete, aPermission, usePermissions } from '@gimac/socle'
import { useEffet } from '../../hooks/base'
import { useNavigate } from 'react-router'
import { useCatalogueReferentiels, useHistoriqueReferentiel, useOptionsTypesProjet, useReferentiel, useReferentiels } from '../../hooks/referentiels'
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
  const onglet = params.get('onglet') ?? 'modelisateur'
  const [creation, setCreation] = useState(false)
  const executeur = useExecuteurActions()
  const navigate = useNavigate()
  const versions = useReferentiels()
  const types = useOptionsTypesProjet()
  const creer = referentielOperationsDescriptor.actions.find((a) => a.key === 'creer')!
  // « Nouveau brouillon » : seulement avec la permission de l'action (HOM_REFERENTIEL_EDIT), sinon 403 du service.
  const peutCreer = aPermission(usePermissions(), creer.permission ?? '')
  const choisir = (id: string) => {
    if (id.startsWith('type:')) return
    setCreation(false)
    setParams({ version: id })
  }
  const ouvrirFormulaire = useCallback(() => setCreation(true), [])
  const ouvrirOnglet = useCallback((k: string) => setParams((p) => ({ ...Object.fromEntries(p), onglet: k })), [setParams])

  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)' }}>
      <PageHeader title={t('referentiel.titre')} actions={<>
        {/* Types de projet et catalogue des fonctions (D-91). */}
        <Button variant="secondary" icon="workflow" onClick={() => navigate('/des/referentiels/types-projet')}>{t('typesProjet.titre')}</Button>
        {peutCreer ? <Button variant="primary" icon="plus" onClick={() => executeur.lancer(creer, null as unknown as ReferentielVersion, t('referentiel.pluriel'), 'homologation', { openForm: ouvrirFormulaire })}>
          {t(creer.label)}
        </Button> : null}
      </>} />
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
              const noeuds = versArbre(v, t, types.libelle)
              // Un type ajouté (création d'un type de projet) arrive déplié lui aussi : l'arbre est recréé quand la liste des types change.
              return <TreeView key={noeuds.map((n) => n.key).join('|')} label={t('referentiel.arbre')} nodes={noeuds} expanded={noeuds.map((n) => n.key)} selected={versionId || undefined} onSelect={choisir} />
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
      {creation && peutCreer ? <FormulaireBrouillon onFin={(id) => { setCreation(false); if (id) setParams({ version: id }) }} /> : null}
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
  const types = useOptionsTypesProjet()
  return (
    <EtatsRequete etat={q} emptyTitle={t('erreur.introuvable')} errorTitle={t('commun.erreur.titre')} retryLabel={t('commun.reessayer')}>
      {(v) => (
        <ResourceDetail<ReferentielVersion>
          descriptor={desc}
          row={{ ...v, libelle: titreVersion(v, t, types.libelle) }}
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

/**
 * Titre de la version : le libellé d'un brouillon ; une version publiée ou retirée est titrée par son type et son numéro,
 * jamais par l'ancien libellé du brouillon (« …, brouillon 4 ») qui restait affiché après la publication.
 */
export function titreVersion(v: ReferentielVersion, t: (k: string, o?: Record<string, unknown>) => string, type: (code: string) => string) {
  return v.etat === 'BROUILLON' || v.numero === null ? v.libelle : t('referentiel.titrePublie', { type: type(v.typeProjet), n: v.numero })
}

/** Création d'un brouillon : formulaire du catalogue piloté par le descripteur (champs inForm « create »). */
function FormulaireBrouillon({ onFin }: { onFin: (id?: string) => void }) {
  const { t } = useTranslation()
  const base = useDescripteurDS(referentielOperationsDescriptor)
  // Pilote facultatif : comptes du catalogue du modélisateur (D-77, §5) ; vide, le créateur pilote.
  const catalogue = useCatalogueReferentiels()
  // Types de projet servis (D-91), plus une liste figée.
  const types = useOptionsTypesProjet()
  const desc = useMemo(() => ({
    ...base,
    fields: base.fields.map((f) => (f.name === 'piloteId' ? { ...f, options: [{ value: '', label: t('referentiel.piloteParDefaut') }, ...(catalogue.data?.pilotes ?? []).map((x) => ({ value: x.id, label: x.nom }))] }
      : f.name === 'typeProjet' ? { ...f, options: types.options } : f)),
  }), [base, catalogue.data, t, types.options])
  const creer = useEffet((s, v: { typeProjet: string; libelle: string; piloteId?: string }) => s.homologation.creerBrouillon(v), [['homologation']])
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  return (
    <ResourceForm<ReferentielVersion>
      descriptor={desc as ResourceDescriptor<ReferentielVersion>}
      mode="create"
      title={t('referentiel.action.creer')}
      presentation="dialog"
      submitting={creer.isPending}
      errors={erreurs}
      serverError={creer.error ? { code: creer.error.code, message: t(`erreur.${creer.error.code}`, { defaultValue: creer.error.message }), correlationId: creer.error.correlationId } : undefined}
      submitLabel={t('referentiel.action.creer')}
      onCancel={() => onFin()}
      onSubmit={(vals) => {
        const typeProjet = String(vals.typeProjet ?? '')
        const libelle = String(vals.libelle ?? '').trim()
        const piloteId = String(vals.piloteId ?? '')
        const e: Record<string, string> = {}
        if (!typeProjet) e.typeProjet = t('commun.champObligatoire')
        if (!libelle) e.libelle = t('commun.champObligatoire')
        setErreurs(e)
        if (Object.keys(e).length) return
        creer.mutate(piloteId ? { typeProjet, libelle, piloteId } : { typeProjet, libelle }, { onSuccess: (v) => onFin((v as ReferentielVersion).id) })
      }}
    />
  )
}
