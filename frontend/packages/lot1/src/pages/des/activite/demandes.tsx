import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CrudDialog, DataTable, InlineAlert, KeyValueList, Select, Skeleton, StatusBadge, type Column, type KeyValueItem } from '@gimac/design-system'
import { ETATS_DEMANDE, estCreation, estProfils, type Demande, type EtatDemande } from '../../../modele/pouvoirs'
import { useAnnulerDemande, useDemande, useMesDemandes } from '../../../hooks/pouvoirs'
import { etatListe } from '../liaisons/communs'
import { pourDialogue, useDates, useErreur, useLibelleUnite } from './communs'
import { useLibelleProfilEquipe } from './dialoguesEquipe'

// « Mes demandes » (D-81 §4, /des/equipe/demandes) : demandes adressées à l'administration, leur état, la
// décision et son motif ; annulation tant qu'elle est en attente. ?demande=<id> ouvre la fiche (lien des
// notifications iam.request-decided.v1).

const STATUT: Record<EtatDemande, 'warn' | 'ok' | 'crit' | 'neutral'> = { EN_ATTENTE: 'warn', ACCEPTEE: 'ok', REFUSEE: 'crit', ANNULEE: 'neutral' }
const PAR_PAGE = 20

export function BadgeDemande({ etat }: { etat: EtatDemande }) {
  const { t } = useTranslation()
  return <StatusBadge status={STATUT[etat]} label={t(`pvr.demande.etat.${etat}`)} />
}

/** Objet d'une demande en une ligne : la personne visée ou, pour une création, le nouveau compte. */
export function useObjetDemande() {
  const { t } = useTranslation()
  return (d: Demande) => (estCreation(d) ? t('pvr.demande.nouveauCompte', { nom: `${d.contenu.prenom} ${d.contenu.nom}` }) : d.cible?.nomAffiche ?? '')
}

/** Contenu d'une demande en paires clé-valeur. */
export function useContenuDemande() {
  const { t } = useTranslation()
  const libelleUnite = useLibelleUnite()
  const libelleProfil = useLibelleProfilEquipe()
  const profils = (l: string[]) => (l.length ? l.map((p) => libelleProfil(p)).join(', ') : t('pvr.aucun'))
  return (d: Demande): KeyValueItem[] => {
    if (estCreation(d)) {
      const c = d.contenu
      return [
        { key: t('pvr.compte.identite'), value: `${c.prenom} ${c.nom}` },
        { key: t('pvr.compte.identifiant'), value: c.identifiant, mono: true },
        { key: t('pvr.compte.email'), value: c.email },
        ...(c.telephone ? [{ key: t('pvr.compte.telephone'), value: c.telephone }] : []),
        { key: t('pvr.compte.fonction'), value: c.fonction },
        { key: t('pvr.compte.unite'), value: libelleUnite(c.unite) },
        { key: t('pvr.compte.profils'), value: profils(c.profils) },
      ]
    }
    if (estProfils(d)) return [{ key: t('pvr.profils.ajouter'), value: profils(d.contenu.ajouter) }, { key: t('pvr.profils.retirer'), value: profils(d.contenu.retirer) }]
    return [{ key: t('pvr.demande.statutDemande'), value: t((d.contenu as { actif: boolean }).actif ? 'pvr.statut.reactivation' : 'pvr.statut.desactivation') }]
  }
}

export function DetailDemande({ id, onFermer, onFait }: { id: string; onFermer: () => void; onFait: (m: string) => void }) {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const contenu = useContenuDemande()
  const objet = useObjetDemande()
  const demande = useDemande(id)
  const annuler = useAnnulerDemande()
  const d = demande.data
  return (
    <CrudDialog context="detail" size="md" id="gm-dlg-demande" title={d ? t(`pvr.demande.type.${d.type}`) : t('pvr.demande.fiche')} subject={d ? objet(d) : undefined} onClose={onFermer}
      error={pourDialogue(erreur(annuler.error))}
      extraActions={d?.etat === 'EN_ATTENTE' ? (
        <Button variant="danger" icon="x" disabled={annuler.isPending} onClick={() => annuler.mutate(d.id, { onSuccess: () => onFait(t('pvr.demande.annulee')) })}>{t('pvr.demande.annuler')}</Button>
      ) : undefined}>
      {demande.isError ? <InlineAlert level="crit" correlationId={demande.error.correlationId || undefined}>{erreur(demande.error)?.titre}</InlineAlert> : !d ? <Skeleton height={160} /> : (
        <KeyValueList items={[
          { key: t('pvr.demande.etat'), value: <BadgeDemande etat={d.etat} /> },
          { key: t('pvr.demande.deposeeLe'), value: dates.instant(d.creeeLe) },
          ...contenu(d),
          { key: t('pvr.demande.motif'), value: d.motif },
          ...(d.decision ? [
            { key: t('pvr.demande.decisionPar'), value: `${d.decision.par.nomAffiche}, ${dates.instant(d.decision.le)}` },
            ...(d.decision.motif ? [{ key: t('pvr.demande.motifDecision'), value: d.decision.motif }] : []),
          ] : []),
        ]} />
      )}
    </CrudDialog>
  )
}

/** Onglet « Mes demandes » : liste filtrée par état, paginée par le service (pages à partir de 0). */
export function MesDemandes({ ouverte, ouvrir, annoncer }: { ouverte: string | null; ouvrir: (id: string | undefined) => void; annoncer: (m: string) => void }) {
  const { t } = useTranslation()
  const dates = useDates()
  const objet = useObjetDemande()
  const [etat, setEtat] = useState<EtatDemande | ''>('')
  const [page, setPage] = useState(1)
  const demandes = useMesDemandes({ etat: etat || undefined, page: page - 1, taille: PAR_PAGE })
  const lignes = demandes.data?.contenu ?? []
  const colonnes: Column<Demande>[] = [
    {
      key: 'type', header: t('pvr.demande.typeColonne'), render: (d) => (
        <button type="button" className="gm-link" style={{ background: 'none', border: 0, padding: 0, font: 'inherit', textAlign: 'left', cursor: 'pointer' }} onClick={() => ouvrir(d.id)}>
          {t(`pvr.demande.type.${d.type}`)}
        </button>
      ),
    },
    { key: 'objet', header: t('pvr.demande.objet'), render: objet },
    { key: 'etat', header: t('pvr.demande.etat'), render: (d) => <BadgeDemande etat={d.etat} /> },
    { key: 'creeeLe', header: t('pvr.demande.deposeeLe'), render: (d) => dates.instant(d.creeeLe) },
    { key: 'decision', header: t('pvr.demande.decision'), render: (d) => d.decision?.motif ?? (d.decision ? d.decision.par.nomAffiche : '') },
  ]
  return (
    <>
      <DataTable columns={colonnes} rows={lignes} rowKey={(d) => d.id} total={demandes.data?.total} page={page} pageSize={PAR_PAGE} onPage={setPage}
        emptyText={t('pvr.demande.vide')} emptyHelp={t('pvr.demande.videAide')}
        toolbar={(
          <div style={{ minWidth: 220 }}>
            <Select name="etat" label={t('pvr.demande.filtreEtat')} value={etat} onChange={(v) => { setEtat(v as EtatDemande | ''); setPage(1) }}
              options={[{ value: '', label: t('pvr.demande.tousEtats') }, ...ETATS_DEMANDE.map((e) => ({ value: e, label: t(`pvr.demande.etat.${e}`) }))]} />
          </div>
        )}
        {...etatListe(demandes, lignes)} onRetry={() => void demandes.refetch()} />
      {ouverte ? <DetailDemande id={ouverte} onFermer={() => ouvrir(undefined)} onFait={(m) => { ouvrir(undefined); annoncer(m) }} /> : null}
    </>
  )
}
