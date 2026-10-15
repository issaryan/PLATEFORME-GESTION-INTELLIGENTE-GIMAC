import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, DataTable, Hash, InlineAlert, Note, StatusBadge, type Column } from '@gimac/design-system'
import { sha256Hex } from '@gimac/socle'
import { useTelechargerPieceParcours } from '../../../../hooks/parcours'
import { useFonctionsCatalogue } from '../../../../hooks/referentiels'
import type { DepotLivrable, EtatLivrable, EvenementPreuve, LivrableAttendu, OperationParcours, SignatureOperation } from '../../../../modele/parcours'
import { enregistrerFichier } from '../../../../ressources/fichier'
import { useExecuteurActions } from '../../../../ressources/actions'
import { useFormatDate } from '../communs'
import { useActionsOperation } from './actions'
import { libelleOperation } from '../../../../adaptateurs/modelisateur'

// Preuves d'une opération (D-109, docs/lot1/referentiel-preuves-signatures.md) : livrables attendus et leur état
// (attendu, déposé, validé, refusé, sans objet, reprise sans pièce, non exigé), consigne de la pièce, une ligne par service
// au grain du service, pièce téléchargée dont l'empreinte est recalculée par le navigateur et comparée à celle inscrite au
// dépôt ; signatures attendues et recueillies (fonction, ordre, date) ; chronologie réelle.

type Ton = 'ok' | 'warn' | 'crit' | 'info' | 'neutral'
export const TON_LIVRABLE: Record<EtatLivrable, Ton> = {
  ATTENDUE: 'warn', DEPOSEE: 'info', VALIDEE: 'ok', REFUSEE: 'crit', SANS_OBJET: 'neutral', REPRISE_SANS_PIECE: 'neutral', NON_EXIGEE: 'neutral',
}
const TON_SIGNATURE: Record<SignatureOperation['etat'], Ton> = { ATTENDUE: 'warn', SIGNEE: 'ok', NON_RECUEILLIE: 'neutral', REPRISE: 'neutral' }

export function BadgeLivrable({ etat }: { etat?: EtatLivrable | null }) {
  const { t } = useTranslation()
  const e = etat ?? 'ATTENDUE'
  return <StatusBadge status={TON_LIVRABLE[e]} label={t(`preuves.etat.${e}`)} />
}

/** Empreinte SHA-256 d'une pièce reçue, comparée à celle inscrite au dépôt (sans casse). */
export async function verifierPiece(blob: Blob, inscrite: string | null | undefined, recue: string | null): Promise<boolean | null> {
  if (!inscrite) return null
  const calculee = await sha256Hex(blob)
  const attendue = inscrite.toLowerCase()
  return calculee.toLowerCase() === attendue && (recue === null || recue.toLowerCase() === attendue)
}

/** Pièce téléchargeable : enregistrée, puis empreinte recalculée et comparée à celle inscrite au dépôt. */
export function Piece({ operationId, depot }: { operationId: string; depot: Pick<DepotLivrable, 'documentRef' | 'nom' | 'empreinte'> }) {
  const { t } = useTranslation()
  const telecharger = useTelechargerPieceParcours()
  const [verifiee, setVerifiee] = useState<boolean | null | undefined>(undefined)
  const [erreur, setErreur] = useState<string | null>(null)
  if (!depot.documentRef) return null
  const agir = async () => {
    setErreur(null)
    try {
      const r = await telecharger.mutateAsync({ operationId, documentId: depot.documentRef! })
      enregistrerFichier(r.blob, depot.nom || depot.documentRef!)
      setVerifiee(await verifierPiece(r.blob, depot.empreinte, r.empreinte))
    } catch (e) {
      setErreur((e as { message?: string }).message ?? '')
    }
  }
  return (
    <span className="gm-col" style={{ gap: 2, alignItems: 'flex-start' }}>
      <Button size="sm" variant="ghost" icon="download" aria-label={t('preuves.telecharger', { nom: depot.nom ?? depot.documentRef })} onClick={() => void agir()}>{depot.nom ?? depot.documentRef}</Button>
      {depot.empreinte ? <Hash value={depot.empreinte} verified={verifiee ?? undefined} /> : null}
      {verifiee === false ? <span role="alert" className="gm-field-help">{t('preuves.empreinteDivergente')}</span> : null}
      {verifiee === true ? <span className="gm-field-help">{t('preuves.empreinteConforme')}</span> : null}
      {erreur ? <span role="alert" className="gm-field-help">{erreur}</span> : null}
    </span>
  )
}

/** Détail d'un dépôt : pièce, sans objet motivé, origine, date et auteur. */
function Depot({ op, depot }: { op: OperationParcours; depot?: DepotLivrable | null }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  if (!depot) return op.reprise ? <span className="gm-field-help">{t('preuves.repriseSansPiece')}</span> : <span className="gm-field-help">{t('preuves.aucunePiece')}</span>
  if (depot.sansObjet) return <span className="gm-field-help">{t('preuves.sansObjetMotif', { motif: depot.motif ?? '', par: depot.deposePar, le: date(depot.deposeLe) })}</span>
  return (
    <span className="gm-col" style={{ gap: 2, alignItems: 'flex-start' }}>
      <Piece operationId={op.id} depot={depot} />
      <span className="gm-field-help">
        {t('preuves.deposePar', { par: depot.deposePar, le: date(depot.deposeLe) })}
        {depot.origine ? ` · ${t('preuves.origine')}` : ''}
        {depot.repris ? ` · ${t('preuves.repris')}` : ''}
      </span>
    </span>
  )
}

type Ligne = { cle: string; livrable: LivrableAttendu; service?: string; etat: EtatLivrable; depot?: DepotLivrable | null }

/** Livrables et preuves d'une opération : une ligne par livrable, une par service au grain du service. */
export function LivrablesEtPreuves({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const f = useFonctionsCatalogue()
  const executeur = useExecuteurActions()
  const { sansObjet } = useActionsOperation()
  const l = op.livrablesAttendus ?? []
  if (!l.length) return <InlineAlert level="info">{t('preuves.aucunLivrable')}</InlineAlert>
  const lignes: Ligne[] = l.flatMap((x) =>
    x.grain === 'SERVICE' && x.services?.length
      ? x.services.map((s) => ({ cle: `${x.code}/${s.serviceCode}`, livrable: x, service: s.serviceCode, etat: s.etat, depot: s.depot }))
      : [{ cle: x.code, livrable: x, etat: x.etat ?? 'ATTENDUE', depot: x.depot }],
  )
  const ouverte = op.etat === 'A_FAIRE' || op.etat === 'EN_COURS'
  const colonnes: Column<Ligne>[] = [
    { key: 'code', header: t('parcoursOps.livrable.code'), width: 90, render: (x) => <span className="gm-mono">{x.service ? `${x.livrable.code} · ${x.service}` : x.livrable.code}</span> },
    {
      key: 'libelle', header: t('preuves.attendu'),
      render: (x) => (
        <span className="gm-col" style={{ gap: 2, alignItems: 'flex-start' }}>
          <span>{x.livrable.libelle}</span>
          <span className="gm-field-help">
            {[t(`preuves.nature.${x.livrable.nature}`), x.livrable.obligatoire ? t('preuves.obligatoire') : t('parcoursOps.livrable.facultatif'), x.livrable.grain === 'SERVICE' ? t('preuves.parService') : null,
              x.livrable.deposant ? t('preuves.deposant', { fonction: x.livrable.libelleDeposant ?? f.libelle(x.livrable.deposant) }) : null].filter(Boolean).join(' · ')}
          </span>
          {x.livrable.consigne ? <span className="gm-field-help">{x.livrable.consigne}</span> : null}
        </span>
      ),
    },
    { key: 'etat', header: t('parcoursOps.col.etat'), width: 150, render: (x) => <BadgeLivrable etat={x.etat} /> },
    { key: 'depot', header: t('preuves.piece'), render: (x) => <Depot op={op} depot={x.depot} /> },
    {
      key: 'action', header: '', type: 'actions',
      render: (x) => (ouverte && x.livrable.peutDeposer && (x.etat === 'ATTENDUE' || x.etat === 'REFUSEE')
        ? <Button size="sm" variant="ghost" icon="circle-x" aria-label={t('preuves.sansObjet.pour', { code: x.cle })}
            onClick={() => executeur.lancer(sansObjet, { ...op, saisie: { livrable: x.livrable.code, libelle: x.livrable.libelle, service: x.service } }, `${op.rang} · ${libelleOperation(t, op)}`, 'parcours')}>
            {t('preuves.sansObjet')}
          </Button>
        : null),
    },
  ]
  return (
    <section className="gm-col" aria-labelledby={`preuves-${op.id}`} style={{ gap: 'var(--gm-space-2)' }}>
      <h3 id={`preuves-${op.id}`} className="gm-type-h3" style={{ margin: 0 }}>{t('preuves.titre')}</h3>
      {op.reprise ? <InlineAlert level="info" title={t('preuves.reprise.titre')}>{t('preuves.reprise.texte')}</InlineAlert> : null}
      <DataTable columns={colonnes} rows={lignes} rowKey={(x) => x.cle} density="compact" pagination={false} state="ok" />
      <Note>{t('preuves.regle')}</Note>
    </section>
  )
}

/** Signatures d'une opération : ordre, libellé, fonction, état, signataire et date ; signature suivante à donner. */
export function SignaturesOperation({ op }: { op: OperationParcours }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const executeur = useExecuteurActions()
  const { signature } = useActionsOperation()
  const f = useFonctionsCatalogue()
  const l = [...(op.signatures ?? [])].sort((a, b) => a.ordre - b.ordre)
  if (!l.length) return <InlineAlert level="info">{t('preuves.aucuneSignature')}</InlineAlert>
  const peutSigner = op.actionsPermises.includes('SIGNER')
  const colonnes: Column<SignatureOperation>[] = [
    { key: 'ordre', header: t('parcoursOps.signature.ordre'), type: 'number', width: 72 },
    { key: 'libelle', header: t('parcoursOps.signature.libelle'), render: (x) => <span className="gm-col" style={{ gap: 0 }}><span>{x.libelle}</span><span className="gm-mono gm-type-small">{x.code}</span></span> },
    { key: 'signataire', header: t('parcoursOps.signature.signataire'), render: (x) => x.libelleSignataire ?? f.libelle(x.signataire) },
    { key: 'etat', header: t('parcoursOps.col.etat'), width: 150, render: (x) => <StatusBadge status={x.etat === 'ATTENDUE' && !x.suivante ? 'neutral' : TON_SIGNATURE[x.etat]} label={t(x.etat === 'ATTENDUE' && !x.suivante ? 'parcoursOps.signature.aVenir' : `preuves.signature.${x.etat}`)} /> },
    { key: 'par', header: t('preuves.signePar'), render: (x) => (x.etat === 'SIGNEE' ? `${x.signeParNom ?? ''}${x.signeLe ? ` · ${date(x.signeLe)}` : ''}` : x.attendu?.nom ?? '') },
    {
      key: 'action', header: '', type: 'actions', render: (x) => (peutSigner && x.suivante && x.etat === 'ATTENDUE'
        ? <Button size="sm" variant="primary" icon="pen-line" aria-label={t('parcoursOps.signature.signerQui', { libelle: x.libelle })}
            onClick={() => executeur.lancer(signature, { ...op, saisie: x }, `${op.rang} · ${x.libelle}`, 'parcours')}>{t('parcoursOps.action.signer')}</Button>
        : null),
    },
  ]
  return <DataTable columns={colonnes} rows={l} rowKey={(x) => x.code} density="compact" pagination={false} state="ok" />
}

/** Chronologie réelle : date, événement, opération, acteur et détail, du plus récent au plus ancien. */
export function Chronologie({ evenements, avecOperation = true, onOuvrir }: { evenements: EvenementPreuve[]; avecOperation?: boolean; onOuvrir?: (operationId: string) => void }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  const colonnes: Column<EvenementPreuve>[] = [
    { key: 'date', header: t('preuves.chrono.date'), width: 120, render: (e) => (e.date ? date(e.date) : '') },
    { key: 'type', header: t('preuves.chrono.evenement'), width: 190, render: (e) => t(`preuves.chrono.type.${e.type}`) },
    ...(avecOperation
      ? [{
          key: 'operation', header: t('parcoursOps.col.operation'),
          render: (e: EvenementPreuve) => (e.rang
            ? onOuvrir && e.operationId ? <Button size="sm" variant="ghost" onClick={() => onOuvrir(e.operationId!)}>{`${e.rang} · ${e.operation ?? ''}`}</Button> : `${e.rang} · ${e.operation ?? ''}`
            : e.etapeCode ? t(`parcours.etape.${e.etapeCode}`, { defaultValue: e.etapeCode }) : ''),
        }]
      : []),
    { key: 'acteur', header: t('preuves.chrono.acteur'), width: 180, render: (e) => e.acteur ?? '' },
    { key: 'detail', header: t('preuves.chrono.detail'), render: (e) => [e.livrable ? `${e.livrable}${e.service ? ` · ${e.service}` : ''}` : null, e.detail].filter(Boolean).join(' : ') },
  ]
  return <DataTable columns={colonnes} rows={evenements} rowKey={(e) => `${e.type}-${e.operationId ?? ''}-${e.livrable ?? ''}-${e.service ?? ''}-${e.date ?? ''}-${e.instant ?? ''}-${e.detail ?? ''}`} density="compact" state={evenements.length ? 'ok' : 'empty'} emptyText={t('preuves.chrono.vide')} />
}
