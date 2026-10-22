import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, CrudDialog, EmptyState, InlineAlert, KeyValueList, PageSection, Skeleton } from '@gimac/design-system'
import { ApiError, aPermission, usePermissions } from '@gimac/socle'
import { nouvelleCle, useActeEspace, useEspace } from '../hooks'
import { PERMISSION, type ActeEspace, type Espace, type Ouverture } from '../modele'
import { BadgeEspace, useFormat, useMessageErreur, type MessageErreur } from './communs'
import { OuvertureEspace } from './OuvertureEspace'

// Espace d'une institution dans la Sandbox GIMAC (UC-INS-09, D-118 points 2 et 3) : fiche, motifs de suspension et
// actes de la console. Servi par l'écran « Espaces des institutions » et par l'onglet « Sandbox » du dossier de
// liaison. Lecture : SBX_HAB_CONSULTATION ; actes : SBX_HAB_ADMINISTRATION (second facteur de la session).

/** Actes que l'état de l'espace admet (règles de EspaceInstitution de SBX-ESPACE, refaites par le service). */
export function actesPossibles(x: Espace): ActeEspace[] {
  const actes: ActeEspace[] = []
  if (x.etat === 'OUVERT' || (x.etat === 'SUSPENDU' && !x.motifsSuspension.includes('DECISION_GIMAC'))) actes.push('suspension')
  if (x.etat === 'SUSPENDU' && !x.motifsSuspension.includes('STATUT_REGISTRE')) actes.push('retablissement')
  if (x.etat === 'SUSPENDU' && x.statutRegistre === 'RETIRE') actes.push('fermeture')
  return actes
}

const ICONE: Record<ActeEspace, 'pause' | 'rotate-ccw' | 'circle-x'> = { suspension: 'pause', retablissement: 'rotate-ccw', fermeture: 'circle-x' }

export function EspaceInstitution({ codeParticipant, ouvrable = false, titre }: { codeParticipant: string; ouvrable?: boolean; titre?: string }) {
  const { t } = useTranslation()
  const f = useFormat()
  const permissions = usePermissions()
  const espace = useEspace(codeParticipant)
  const [acte, setActe] = useState<ActeEspace | null>(null)
  const [ouverture, setOuverture] = useState(false)
  const [ouverte, setOuverte] = useState<Ouverture | null>(null)
  const administrateur = aPermission(permissions, PERMISSION.administration)
  const absent = espace.error instanceof ApiError && espace.error.status === 404
  const x = espace.data
  return (
    <PageSection title={titre ?? t('sbx.espace.titre', { code: codeParticipant })} actions={x ? <BadgeEspace etat={x.etat} /> : undefined}>
      {ouverte ? <InlineAlert level="info" title={t(`sbx.ouverture.resultat.${ouverte.etatSaga}`, { code: ouverte.codeParticipant })}>{ouverte.motifEchec ?? undefined}</InlineAlert> : null}
      {espace.isPending ? <Skeleton height={96} /> : null}
      {absent ? (
        <EmptyState icon="inbox" title={t('sbx.espace.absent')} text={t('sbx.espace.absentTexte')}
          action={ouvrable && administrateur ? <Button variant="primary" icon="plus" onClick={() => setOuverture(true)}>{t('sbx.ouverture.titre')}</Button> : undefined} />
      ) : null}
      {espace.isError && !absent ? <InlineAlert level="crit" title={t('sbx.erreur.lecture')} /> : null}
      {x ? (
        <>
          <KeyValueList items={[
            { key: t('sbx.espace.code'), value: x.codeEspace, mono: true },
            { key: t('sbx.espace.codeParticipant'), value: x.codeParticipant, mono: true },
            { key: t('sbx.espace.statutRegistre'), value: x.statutRegistre ? t(`sbx.registre.${x.statutRegistre}`, { defaultValue: x.statutRegistre }) : t('sbx.espace.inconnu') },
            { key: t('sbx.espace.administrateurs'), value: String(x.administrateursActifs) },
            { key: t('sbx.espace.dateOuverture'), value: f.instant(x.dateOuverture) || t('sbx.espace.sansDate') },
            ...(x.dateSuspension ? [{ key: t('sbx.espace.dateSuspension'), value: f.instant(x.dateSuspension) }] : []),
            ...(x.dateFermeture ? [{ key: t('sbx.espace.dateFermeture'), value: f.instant(x.dateFermeture) }] : []),
          ]} />
          {x.motifsSuspension.map((m) => <InlineAlert key={m} level="warn" title={t(`sbx.espace.motif.${m}`)} />)}
          {administrateur && actesPossibles(x).length ? (
            <span className="gm-row">
              {actesPossibles(x).map((a) => (
                <Button key={a} variant={a === 'fermeture' ? 'danger' : 'secondary'} icon={ICONE[a]} onClick={() => setActe(a)}>{t(`sbx.acte.${a}`)}</Button>
              ))}
            </span>
          ) : null}
        </>
      ) : null}
      {acte && x ? <DialogueActe espace={x} acte={acte} onFermer={() => setActe(null)} /> : null}
      {ouverture ? <OuvertureEspace codeParticipant={codeParticipant} onFermer={() => setOuverture(false)} onOuvert={(o) => { setOuverture(false); setOuverte(o) }} /> : null}
    </PageSection>
  )
}

function DialogueActe({ espace, acte, onFermer }: { espace: Espace; acte: ActeEspace; onFermer: () => void }) {
  const { t } = useTranslation()
  const erreur = useMessageErreur()
  const agir = useActeEspace()
  const [cle] = useState(nouvelleCle)
  const [echec, setEchec] = useState<MessageErreur | undefined>()
  return (
    <CrudDialog context="confirm" title={t(`sbx.acte.${acte}`)} subject={espace.codeEspace} onClose={onFermer} submitLabel={t(`sbx.acte.valider.${acte}`)} submitIcon={ICONE[acte]}
      submitting={agir.isPending} error={echec} consequences={[t(`sbx.acte.consequence.${acte}`)]} danger={acte === 'fermeture'} irreversible={acte === 'fermeture'}
      onSubmit={() => {
        setEchec(undefined)
        agir.mutate({ code: espace.codeParticipant, acte, cle }, { onSuccess: onFermer, onError: (e) => setEchec(erreur(e)) })
      }} />
  )
}
