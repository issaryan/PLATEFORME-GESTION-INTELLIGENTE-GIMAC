import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, CheckboxGroup, ConfirmDialog, CrudDialog, DataTable, Hash, InlineAlert, Select, StatusBadge, Tag, TextArea, type Column, type Status } from '@gimac/design-system'
import { aPermission, estApiError, usePermissions } from '@gimac/socle'
import { useEffet, useLecture } from '../hooks'
import type { AccesReexamen, DecisionReexamen, SaisieDecisionReexamen } from '../modele/reexamens'

const STATUTS: Record<DecisionReexamen, Status> = { EN_ATTENTE: 'warn', MAINTENU: 'ok', REDUIT: 'info', REVOQUE: 'crit' }

/** Campagne courante et historique : instantané des profils et de toutes les permissions, décisions et scellement. */
export function OngletReexamens() {
  const { t } = useTranslation()
  const permissions = usePermissions()
  const peutDecider = aPermission(permissions, 'IAM_ACCOUNT_ROLES')
  const campagnes = useLecture(['iam', 'reexamens'], (s) => s.reexamens.lister())
  const [selection, setSelection] = useState<string>()
  useEffect(() => { if (!selection && campagnes.data?.length) setSelection(campagnes.data[0].id) }, [campagnes.data, selection])
  const detail = useLecture(['iam', 'reexamens', selection ?? ''], (s) => s.reexamens.lire(selection!), { enabled: !!selection })
  const decider = useEffet((s, v: { id: string; compteId: string; d: SaisieDecisionReexamen }) => s.reexamens.decider(v.id, v.compteId, v.d), [['iam', 'reexamens']])
  const cloturer = useEffet((s, id: string) => s.reexamens.cloturer(id), [['iam', 'reexamens']])
  const [edition, setEdition] = useState<AccesReexamen>()
  const [confirmation, setConfirmation] = useState(false)
  const [erreur, setErreur] = useState<string>()
  const campagne = detail.data?.campagne

  const colonnes = useMemo<Column<AccesReexamen>[]>(() => [
    { key: 'compte', header: t('sbx.revue.compte'), render: (a) => <><strong>{a.nomAffiche}</strong><br /><span className="gm-mono">{a.identifiant}</span></> },
    { key: 'profils', header: t('sbx.revue.profils'), render: (a) => <div className="gm-row" style={{ flexWrap: 'wrap' }}>{a.profils.map((p) => <Tag key={p}>{p}</Tag>)}</div> },
    { key: 'permissions', header: t('sbx.revue.permissions'), render: (a) => <details><summary>{t('sbx.revue.permissionsNombre', { count: a.permissions.length, sbx: a.permissions.filter((p) => p.startsWith('SBX_')).length })}</summary><div className="gm-row" style={{ flexWrap: 'wrap' }}>{a.permissions.map((p) => <Tag key={p}>{p}</Tag>)}</div></details> },
    { key: 'decision', header: t('sbx.revue.decision'), render: (a) => <StatusBadge status={STATUTS[a.decision]} label={t(`sbx.revue.decision.${a.decision}`)} /> },
    { key: 'geste', header: t('sbx.revue.geste'), render: (a) => a.decision === 'EN_ATTENTE' && campagne?.state === 'OUVERTE' && peutDecider ? <Button size="sm" variant="secondary" onClick={() => { setErreur(undefined); setEdition(a) }}>{t('sbx.revue.decider')}</Button> : null },
  ], [campagne?.state, peutDecider, t])

  const soumettre = (a: AccesReexamen, d: SaisieDecisionReexamen) => {
    if (!campagne) return
    decider.mutate({ id: campagne.id, compteId: a.compteId, d }, {
      onSuccess: () => setEdition(undefined),
      onError: (e) => setErreur(estApiError(e) ? t(`erreur.${e.code}`, { defaultValue: e.message }) : e.message),
    })
  }

  return <div className="gm-col">
    <InlineAlert level="info" title={t('sbx.revue.aide.titre')}>{t('sbx.revue.aide')}</InlineAlert>
    {campagnes.data?.length ? <Select label={t('sbx.revue.campagne')} value={selection ?? ''} onChange={setSelection} options={campagnes.data.map((c) => ({ value: c.id, label: `${c.period} - ${t(`sbx.revue.etat.${c.state}`)}` }))} /> : null}
    {campagne ? <Card title={t('sbx.revue.titrePeriode', { periode: campagne.period })} actions={<div className="gm-row"><StatusBadge status={campagne.state === 'CLOSE' ? 'ok' : 'warn'} label={t(`sbx.revue.etat.${campagne.state}`)} />{campagne.state === 'OUVERTE' && peutDecider ? <Button variant="primary" disabled={campagne.enAttente > 0} disabledReason={campagne.enAttente > 0 ? t('sbx.revue.restant', { count: campagne.enAttente }) : undefined} onClick={() => setConfirmation(true)}>{t('sbx.revue.cloturer')}</Button> : null}</div>}>
      <p>{t('sbx.revue.ouverteLe', { date: new Date(campagne.openedAt).toLocaleString() })}</p>
      {campagne.digest ? <p>{t('sbx.revue.empreinte')} <Hash value={campagne.digest} /></p> : null}
      {detail.data ? <DataTable columns={colonnes} rows={detail.data.acces} rowKey={(a) => a.compteId} state={detail.data.acces.length ? 'ok' : 'empty'} emptyText={t('sbx.revue.vide')} /> : null}
    </Card> : null}
    {detail.isPending ? <p role="status">{t('sbx.revue.chargement')}</p> : null}
    {detail.isError || campagnes.isError ? <InlineAlert level="crit" title={t('sbx.revue.indisponible')} /> : null}
    {campagnes.data?.length === 0 ? <InlineAlert level="info" title={t('sbx.revue.aucune')} /> : null}
    {edition && campagne ? <DialogueDecision acces={edition} erreur={erreur} charge={decider.isPending} onFermer={() => setEdition(undefined)} onValider={(d) => soumettre(edition, d)} /> : null}
    {confirmation && campagne ? <ConfirmDialog title={t('sbx.revue.cloturer')} description={t('sbx.revue.cloture.aide')} confirmLabel={t('sbx.revue.cloturer')} onClose={() => setConfirmation(false)} onConfirm={() => { setConfirmation(false); cloturer.mutate(campagne.id) }} /> : null}
  </div>
}

function DialogueDecision({ acces, erreur, charge, onFermer, onValider }: { acces: AccesReexamen; erreur?: string; charge: boolean; onFermer: () => void; onValider: (d: SaisieDecisionReexamen) => void }) {
  const { t } = useTranslation()
  const [decision, setDecision] = useState<Exclude<DecisionReexamen, 'EN_ATTENTE'>>('MAINTENU')
  const [profils, setProfils] = useState(acces.profils)
  const [comment, setComment] = useState('')
  const [tentee, setTentee] = useState(false)
  const invalide = decision !== 'MAINTENU' && !comment.trim() || decision === 'REDUIT' && (profils.length === 0 || profils.length === acces.profils.length)
  return <CrudDialog context="edit" id="gm-dlg-reexamen" title={t('sbx.revue.decider')} subject={acces.nomAffiche} onClose={onFermer} onSubmit={() => { setTentee(true); if (!invalide) onValider({ decision, profils: decision === 'MAINTENU' ? undefined : profils, comment: comment || undefined }) }} submitLabel={t('sbx.revue.enregistrer')} submitDisabled={charge}>
    <Select label={t('sbx.revue.decision')} value={decision} onChange={(x) => setDecision(x as typeof decision)} options={(['MAINTENU', 'REDUIT', 'REVOQUE'] as const).map((d) => ({ value: d, label: t(`sbx.revue.decision.${d}`) }))} />
    {decision === 'REDUIT' ? <CheckboxGroup label={t('sbx.revue.profilsConserves')} value={profils} onChange={setProfils} options={acces.profils.map((p) => ({ value: p, label: p }))} /> : null}
    {decision !== 'MAINTENU' ? <TextArea label={t('sbx.revue.motif')} required value={comment} onChange={setComment} maxLength={500} error={tentee && !comment.trim() ? t('sbx.revue.motifRequis') : undefined} /> : null}
    {tentee && invalide ? <InlineAlert level="warn" title={t('sbx.revue.decisionInvalide')} /> : null}
    {erreur ? <InlineAlert level="crit" title={erreur} /> : null}
  </CrudDialog>
}
