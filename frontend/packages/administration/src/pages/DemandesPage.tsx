import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, CrudDialog, DataTable, InlineAlert, KeyValueList, PageHeader, Select, Skeleton, StatusBadge, type Column, type KeyValueItem } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { dateHeure, useMessageErreur } from '@gimac/compte'
import { ETATS_DEMANDE, estCreation, estProfils, type Demande, type EtatDemande } from '@gimac/lot1'
import { useDemandeATraiter, useDemandesATraiter, useEffet, useLibelleProfil, useUnitesOrganisation } from '../hooks'
import { Annonce } from '../composants/Annonce'
import { useActes } from '../composants/actes'
import { etatListe } from '../composants/formats'

// Demandes à traiter (D-81 §4, /console/demandes et /des/administration/demandes, IAM_ACCOUNT_READ) : demandes
// adressées par les chefs (création de compte, profils, désactivation ou réactivation). Accepter exige la
// permission de l'acte (IAM_ACCOUNT_CREATE, IAM_ACCOUNT_ROLES, IAM_ACCOUNT_STATUS), un motif et la clé de sécurité
// (FIDO2, X-Reauth-Assertion) ; MS-IAM exécute alors l'acte avec ses règles habituelles. Refuser exige un motif,
// transmis au demandeur. ?demande=<id> ouvre la fiche (lien des notifications iam.request-submitted.v1).

const PAR_PAGE = 20
const STATUT: Record<EtatDemande, 'warn' | 'ok' | 'crit' | 'neutral'> = { EN_ATTENTE: 'warn', ACCEPTEE: 'ok', REFUSEE: 'crit', ANNULEE: 'neutral' }
const ACTE: Record<Demande['type'], string> = { CREATION_COMPTE: 'IAM_ACCOUNT_CREATE', PROFILS: 'IAM_ACCOUNT_ROLES', STATUT: 'IAM_ACCOUNT_STATUS' }

function BadgeDemande({ etat }: { etat: EtatDemande }) {
  const { t } = useTranslation()
  return <StatusBadge status={STATUT[etat]} label={t(`console.demandes.etat.${etat}`)} />
}

function useDescription() {
  const { t, i18n } = useTranslation()
  const libelleProfil = useLibelleProfil()
  const unites = useUnitesOrganisation()
  const unite = (code: string) => (unites.data ?? []).find((u) => u.code === code)?.libelle ?? code
  const profils = (l: string[]) => (l.length ? l.map((p) => libelleProfil(p)).join(', ') : t('console.demandes.aucun'))
  const objet = (d: Demande) => (estCreation(d) ? t('console.demandes.nouveauCompte', { nom: `${d.contenu.prenom} ${d.contenu.nom}` }) : d.cible?.nomAffiche ?? '')
  const contenu = (d: Demande): KeyValueItem[] => {
    if (estCreation(d)) {
      const c = d.contenu
      return [
        { key: t('console.demandes.identite'), value: `${c.prenom} ${c.nom}` },
        { key: t('console.demandes.identifiant'), value: c.identifiant, mono: true },
        { key: t('console.demandes.email'), value: c.email },
        ...(c.telephone ? [{ key: t('console.demandes.telephone'), value: c.telephone }] : []),
        { key: t('console.demandes.fonction'), value: c.fonction },
        { key: t('console.demandes.unite'), value: unite(c.unite) },
        { key: t('console.demandes.responsable'), value: c.responsableId, mono: true },
        { key: t('console.demandes.profils'), value: profils(c.profils) },
      ]
    }
    if (estProfils(d)) return [{ key: t('console.demandes.ajouter'), value: profils(d.contenu.ajouter) }, { key: t('console.demandes.retirer'), value: profils(d.contenu.retirer) }]
    return [{ key: t('console.demandes.statut'), value: t((d.contenu as { actif: boolean }).actif ? 'console.demandes.reactivation' : 'console.demandes.desactivation') }]
  }
  const date = (iso: string) => dateHeure(iso, i18n.language)
  return { objet, contenu, date }
}

function Fiche({ id, onFermer, onAccepter, onRefuser }: { id: string; onFermer: () => void; onAccepter: (d: Demande) => void; onRefuser: (d: Demande) => void }) {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const message = useMessageErreur()
  const { objet, contenu, date } = useDescription()
  const demande = useDemandeATraiter(id)
  const d = demande.data
  const enAttente = d?.etat === 'EN_ATTENTE'
  return (
    <CrudDialog context="detail" size="md" id="gm-dlg-demande-admin" title={d ? t(`console.demandes.type.${d.type}`) : t('console.demandes.fiche')} subject={d ? objet(d) : undefined} onClose={onFermer}
      extraActions={d && enAttente ? (
        <>
          <Button variant="danger" icon="x" onClick={() => onRefuser(d)}>{t('console.demandes.refuser')}</Button>
          {aPermission(permissions, ACTE[d.type]) ? <Button variant="primary" icon="key-round" onClick={() => onAccepter(d)}>{t('console.demandes.accepter')}</Button> : null}
        </>
      ) : undefined}>
      {demande.isError ? <InlineAlert level="crit" title={message(demande.error).texte} correlationId={demande.error.correlationId || undefined} /> : !d ? <Skeleton height={200} /> : (
        <>
          {enAttente && !aPermission(permissions, ACTE[d.type]) ? <InlineAlert level="info" title={t('console.demandes.sansPermission', { permission: ACTE[d.type] })} /> : null}
          <KeyValueList items={[
            { key: t('console.demandes.etatColonne'), value: <BadgeDemande etat={d.etat} /> },
            { key: t('console.demandes.demandeur'), value: d.demandeur.nomAffiche },
            { key: t('console.demandes.deposeeLe'), value: date(d.creeeLe) },
            ...contenu(d),
            { key: t('console.demandes.motif'), value: d.motif },
            ...(d.decision ? [
              { key: t('console.demandes.decisionPar'), value: `${d.decision.par.nomAffiche}, ${date(d.decision.le)}` },
              ...(d.decision.motif ? [{ key: t('console.demandes.motifDecision'), value: d.decision.motif }] : []),
            ] : []),
          ]} />
        </>
      )}
    </CrudDialog>
  )
}

export default function DemandesPage() {
  const { t } = useTranslation()
  const { objet, date } = useDescription()
  const actes = useActes()
  const [params, setParams] = useSearchParams()
  const [etat, setEtat] = useState<EtatDemande | ''>('EN_ATTENTE')
  const [page, setPage] = useState(1)
  const demandes = useDemandesATraiter({ etat: etat || undefined, page: page - 1, taille: PAR_PAGE })
  const accepter = useEffet((s, v: { id: string; motif: string; assertion: string }) => s.pouvoirs.accepter(v.id, v.motif, v.assertion))
  const refuser = useEffet((s, v: { id: string; motif: string }) => s.pouvoirs.refuser(v.id, v.motif))
  const ouverte = params.get('demande')
  const ouvrir = (id?: string) => setParams(id ? { demande: id } : {})
  const lignes = demandes.data?.contenu ?? []
  const colonnes: Column<Demande>[] = [
    {
      key: 'type', header: t('console.demandes.demande'), render: (d) => (
        <button type="button" className="gm-link" style={{ background: 'none', border: 0, padding: 0, font: 'inherit', textAlign: 'left', cursor: 'pointer' }} onClick={() => ouvrir(d.id)}>
          {t(`console.demandes.type.${d.type}`)}
        </button>
      ),
    },
    { key: 'objet', header: t('console.demandes.objet'), render: objet },
    { key: 'demandeur', header: t('console.demandes.demandeur'), render: (d) => d.demandeur.nomAffiche },
    { key: 'etat', header: t('console.demandes.etatColonne'), render: (d) => <BadgeDemande etat={d.etat} /> },
    { key: 'creeeLe', header: t('console.demandes.deposeeLe'), render: (d) => date(d.creeeLe) },
  ]
  return (
    <div className="gm-col">
      <PageHeader title={t('console.demandes.titre')} subtitle={t('console.demandes.texte')} />
      <Annonce retour={actes.retour} />
      <DataTable columns={colonnes} rows={lignes} rowKey={(d) => d.id} total={demandes.data?.total} page={page} pageSize={PAR_PAGE} onPage={setPage}
        emptyText={t('console.demandes.vide')} onRetry={() => void demandes.refetch()}
        toolbar={(
          <div style={{ minWidth: 220 }}>
            <Select name="etat" label={t('console.demandes.filtreEtat')} value={etat} onChange={(v) => { setEtat(v as EtatDemande | ''); setPage(1) }}
              options={[{ value: '', label: t('console.demandes.tousEtats') }, ...ETATS_DEMANDE.map((e) => ({ value: e, label: t(`console.demandes.etat.${e}`) }))]} />
          </div>
        )}
        {...etatListe(demandes, lignes)} />
      {ouverte ? (
        <Fiche id={ouverte} onFermer={() => ouvrir()}
          onAccepter={(d) => {
            ouvrir()
            actes.sensible({
              titre: t('console.demandes.accepterTitre', { type: t(`console.demandes.type.${d.type}`) }),
              objet: [{ key: t('console.demandes.demande'), value: d.id, mono: true }, { key: t('console.demandes.objet'), value: objet(d) }, { key: t('console.demandes.demandeur'), value: d.demandeur.nomAffiche }],
              contenu: { demande: d.id, type: d.type, contenu: d.contenu },
              succes: t('console.demandes.acceptee', { objet: objet(d) }),
              executer: (motif, assertion) => accepter.mutateAsync({ id: d.id, motif, assertion }),
            })
          }}
          onRefuser={(d) => {
            ouvrir()
            actes.motive({
              titre: t('console.demandes.refuserTitre'), description: t('console.demandes.refuserTexte'), libelle: t('console.demandes.refuser'), danger: true,
              objet: [{ key: t('console.demandes.demande'), value: t(`console.demandes.type.${d.type}`) }, { key: t('console.demandes.objet'), value: objet(d) }],
              succes: t('console.demandes.refusee', { objet: objet(d) }),
              executer: (motif) => refuser.mutateAsync({ id: d.id, motif }),
            })
          }} />
      ) : null}
      {actes.dialogue}
    </div>
  )
}
