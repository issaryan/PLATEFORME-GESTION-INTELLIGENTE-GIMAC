import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Button, DataTable, InlineAlert, Note, StatCard, StatusBadge, type Column } from '@gimac/design-system'
import { useChronologieParcours, useOperationsParcours } from '../../../../hooks/parcours'
import { useFonctionsCatalogue } from '../../../../hooks/referentiels'
import { ETAPES, type DepotLivrable, type DossierLiaison, type EtatLivrable, type LivrableAttendu, type OperationParcours, type SignatureOperation } from '../../../../modele/parcours'
import { Donnees } from '../../liaisons/communs'
import { BadgeLivrable, Chronologie, Piece } from '../operations/Preuves'
import { DetailPreuve, type PreuveDetaillee } from '../operations/DetailPreuve'
import { libelleOperation } from '../../../../adaptateurs/modelisateur'
import { useFormatDate } from '../communs'

// Onglets du dossier (D-109, docs/lot1/referentiel-preuves-signatures.md) : « Livrables et preuves » (chaque étape et
// chaque opération avec ses preuves attendues et leur état, pièces téléchargeables à empreinte vérifiée), « Signatures »
// (attendues et recueillies : fonction, ordre, date, signataire) et « Chronologie » (événements réels du parcours).
// Une ligne mène au tiroir de l'opération dans l'onglet « Opérations ».

/** Ouverture d'une opération dans l'onglet « Opérations » (tiroir, lien profond). */
function useOuvrir() {
  const [, setParams] = useSearchParams()
  return (operationId: string) => setParams({ onglet: 'operations', operation: operationId }, { replace: false })
}

/** Étapes de la version dans leur ordre, les onze étapes canoniques à défaut. */
function useEtapes(dossier: DossierLiaison) {
  const { t } = useTranslation()
  const version = useMemo(() => [...(dossier.etapesVersion ?? [])].sort((a, b) => a.ordre - b.ordre), [dossier.etapesVersion])
  const ordre = version.length ? version.map((e) => e.code) : ETAPES
  const libelle = (code: string) => version.find((e) => e.code === code)?.libelle ?? t(`parcours.etape.${code}`, { defaultValue: code })
  return { ordre, libelle }
}

const etapeDe = (o: OperationParcours) => o.etapeCode ?? o.etape

type LignePreuve = { cle: string; op: OperationParcours; livrable: LivrableAttendu; code: string; libelle: string; consigne?: string | null; service?: string; etat: EtatLivrable; obligatoire: boolean; depot?: DepotLivrable | null }

/** Lignes de preuves d'une opération : une par livrable, une par service au grain du service. */
export function lignesPreuves(o: OperationParcours): LignePreuve[] {
  return (o.livrablesAttendus ?? []).flatMap((l) =>
    l.grain === 'SERVICE' && l.services?.length
      ? l.services.map((s) => ({ cle: `${o.id}/${l.code}/${s.serviceCode}`, op: o, livrable: l, code: l.code, libelle: l.libelle, consigne: l.consigne, service: s.serviceCode, etat: s.etat, obligatoire: l.obligatoire, depot: s.depot }))
      : [{ cle: `${o.id}/${l.code}`, op: o, livrable: l, code: l.code, libelle: l.libelle, consigne: l.consigne, etat: l.etat ?? 'ATTENDUE', obligatoire: l.obligatoire, depot: l.depot }],
  )
}

export function OngletPreuves({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const q = useOperationsParcours(dossier.id)
  return (
    <Donnees requete={q} estVide={(l) => l.length === 0} vide={t('parcoursOps.vide')}>
      {(ops) => <Preuves dossier={dossier} ops={ops} />}
    </Donnees>
  )
}

function Preuves({ dossier, ops }: { dossier: DossierLiaison; ops: OperationParcours[] }) {
  const { t } = useTranslation()
  const ouvrir = useOuvrir()
  const [detail, setDetail] = useState<PreuveDetaillee | null>(null)
  const { ordre, libelle } = useEtapes(dossier)
  const toutes = ops.flatMap(lignesPreuves)
  const compte = (e: EtatLivrable[]) => toutes.filter((x) => e.includes(x.etat) && x.obligatoire).length
  const colonnes: Column<LignePreuve>[] = [
    {
      key: 'operation', header: t('parcoursOps.col.operation'), width: 260,
      render: (x) => <Button size="sm" variant="ghost" onClick={() => ouvrir(x.op.id)}>{`${x.op.rang} · ${libelleOperation(t, x.op)}`}</Button>,
    },
    {
      key: 'libelle', header: t('preuves.attendu'),
      render: (x) => (
        <span className="gm-col" style={{ gap: 2, alignItems: 'flex-start' }}>
          <span><span className="gm-mono">{x.service ? `${x.code} · ${x.service}` : x.code}</span> {x.libelle}{x.obligatoire ? '' : ` (${t('parcoursOps.livrable.facultatif')})`}</span>
          {x.consigne ? <span className="gm-field-help">{x.consigne}</span> : null}
        </span>
      ),
    },
    { key: 'etat', header: t('parcoursOps.col.etat'), width: 160, render: (x) => <BadgeLivrable etat={x.etat} /> },
    {
      key: 'piece', header: t('preuves.piece'), width: 240,
      render: (x) => (x.depot?.sansObjet ? <span className="gm-field-help">{x.depot.motif}</span> : x.depot?.documentRef ? <Piece operationId={x.op.id} depot={x.depot} /> : ''),
    },
    {
      key: 'detail', header: '', type: 'actions', width: 120,
      render: (x) => (
        <Button size="sm" variant="ghost" icon="eye" aria-label={t('preuves.detail.voirPour', { code: x.service ? `${x.code} · ${x.service}` : x.code, operation: x.op.rang })}
          onClick={() => setDetail({ op: x.op, livrable: x.livrable, service: x.service, etat: x.etat, depot: x.depot })}>
          {t('preuves.detail.voir')}
        </Button>
      ),
    },
  ]
  const etapes = [...ordre, ...ops.map(etapeDe).filter((e) => !ordre.includes(e))].filter((e, i, l) => l.indexOf(e) === i && ops.some((o) => etapeDe(o) === e))
  return (
    <div className="gm-col">
      <div className="gm-stat-grid">
        <StatCard label={t('preuves.compteur.attendues')} value={compte(['ATTENDUE', 'REFUSEE'])} status={compte(['ATTENDUE', 'REFUSEE']) ? 'warn' : 'ok'} statusLabel={t(compte(['ATTENDUE', 'REFUSEE']) ? 'preuves.etat.ATTENDUE' : 'preuves.compteur.aucune')} />
        <StatCard label={t('preuves.compteur.deposees')} value={compte(['DEPOSEE'])} />
        <StatCard label={t('preuves.compteur.validees')} value={compte(['VALIDEE', 'SANS_OBJET'])} />
        <StatCard label={t('preuves.compteur.reprises')} value={compte(['REPRISE_SANS_PIECE', 'NON_EXIGEE'])} />
      </div>
      <Note>{t('preuves.regle')}</Note>
      {etapes.map((e) => {
        const lignes = ops.filter((o) => etapeDe(o) === e).flatMap(lignesPreuves)
        const manquantes = lignes.filter((x) => x.obligatoire && (x.etat === 'ATTENDUE' || x.etat === 'REFUSEE')).length
        return (
          <section key={e} className="gm-col" style={{ gap: 'var(--gm-space-2)' }} aria-labelledby={`preuves-etape-${e}`}>
            <div className="gm-row" style={{ justifyContent: 'space-between' }}>
              <h3 id={`preuves-etape-${e}`} className="gm-type-h3" style={{ margin: 0 }}>{libelle(e)}</h3>
              <StatusBadge status={manquantes ? 'warn' : 'ok'} label={manquantes ? t('preuves.etape.manquantes', { count: manquantes }) : t('preuves.etape.complete')} />
            </div>
            <DataTable columns={colonnes} rows={lignes} rowKey={(x) => x.cle} density="compact" state={lignes.length ? 'ok' : 'empty'} emptyText={t('preuves.aucunLivrable')} />
          </section>
        )
      })}
      {detail ? <DetailPreuve preuve={detail} onFermer={() => setDetail(null)} /> : null}
    </div>
  )
}

type LigneSignature = SignatureOperation & { op: OperationParcours; cle: string }

export function OngletSignatures({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const q = useOperationsParcours(dossier.id)
  return (
    <Donnees requete={q} estVide={(l) => l.length === 0} vide={t('parcoursOps.vide')}>
      {(ops) => <Signatures ops={ops} />}
    </Donnees>
  )
}

function Signatures({ ops }: { ops: OperationParcours[] }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const ouvrir = useOuvrir()
  const f = useFonctionsCatalogue()
  const lignes: LigneSignature[] = ops.flatMap((o) => [...(o.signatures ?? [])].sort((a, b) => a.ordre - b.ordre).map((s) => ({ ...s, op: o, cle: `${o.id}/${s.code}` })))
  if (!lignes.length) return <InlineAlert level="info">{t('preuves.aucuneSignature')}</InlineAlert>
  const ton = (s: LigneSignature) => (s.etat === 'SIGNEE' ? 'ok' : s.etat === 'ATTENDUE' ? (s.suivante ? 'warn' : 'neutral') : 'neutral')
  const colonnes: Column<LigneSignature>[] = [
    { key: 'operation', header: t('parcoursOps.col.operation'), width: 260, render: (s) => <Button size="sm" variant="ghost" onClick={() => ouvrir(s.op.id)}>{`${s.op.rang} · ${libelleOperation(t, s.op)}`}</Button> },
    { key: 'ordre', header: t('parcoursOps.signature.ordre'), type: 'number', width: 72 },
    { key: 'libelle', header: t('parcoursOps.signature.libelle') },
    { key: 'signataire', header: t('parcoursOps.signature.signataire'), render: (s) => s.libelleSignataire ?? f.libelle(s.signataire) },
    { key: 'etat', header: t('parcoursOps.col.etat'), width: 160, render: (s) => <StatusBadge status={ton(s)} label={t(s.etat === 'ATTENDUE' && !s.suivante ? 'parcoursOps.signature.aVenir' : `preuves.signature.${s.etat}`)} /> },
    { key: 'par', header: t('preuves.signePar'), render: (s) => (s.etat === 'SIGNEE' ? `${s.signeParNom ?? ''}${s.signeLe ? ` · ${date(s.signeLe)}` : ''}` : s.attendu?.nom ?? '') },
  ]
  const attendues = lignes.filter((s) => s.etat === 'ATTENDUE').length
  return (
    <div className="gm-col">
      <div className="gm-stat-grid">
        <StatCard label={t('preuves.signatures.attendues')} value={attendues} status={attendues ? 'warn' : 'ok'} statusLabel={t(attendues ? 'preuves.signature.ATTENDUE' : 'preuves.compteur.aucune')} />
        <StatCard label={t('preuves.signatures.recueillies')} value={lignes.filter((s) => s.etat === 'SIGNEE').length} />
      </div>
      <DataTable columns={colonnes} rows={lignes} rowKey={(s) => s.cle} density="compact" state="ok" />
      <Note>{t('preuves.signatures.regle')}</Note>
    </div>
  )
}

export function OngletChronologie({ dossier }: { dossier: DossierLiaison }) {
  const { t } = useTranslation()
  const ouvrir = useOuvrir()
  const q = useChronologieParcours(dossier.id)
  return (
    <Donnees requete={q} vide={t('preuves.chrono.vide')}>
      {(l) => <Chronologie evenements={l} onOuvrir={ouvrir} />}
    </Donnees>
  )
}
