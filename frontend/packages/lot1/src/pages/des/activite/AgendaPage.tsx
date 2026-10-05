import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, Card, InlineAlert, PageHeader, Select, Skeleton } from '@gimac/design-system'
import { aPermission, usePermissions, useSession } from '@gimac/socle'
import type { Evenement } from '../../../modele/pilotage'
import { ajouterJours, aujourdhui, jourDe, semaineDe, semaineIso } from '../../../modele/jours'
import { useAgenda, useEquipe, useRepondre } from '../../../hooks/pilotage'
import { PERM } from '../../../permissions'
import { useRetour } from '../parcours/communs'
import { NavigationPeriode, TagCategorie, jourDemande, useDates, useEcranEtroit, useErreur } from './communs'
import { DetailEvenement, DialogueEvenement } from './dialoguesAgenda'

// Agenda (D-76, /des/agenda) : semaine du lundi au dimanche (un jour à la fois sur petit écran), invitations
// à accepter ou décliner, création et modification d'un événement en fenêtre modale. ?jour=AAAA-MM-JJ fixe
// la semaine (ou le jour) affichée ; ?evenement=<id> ouvre une fiche ; ?collaborateur=<id> (PIL_TEAM_READ)
// lit l'agenda d'un membre de l'équipe (catégorie, titre et horaires).

type Dialogue = { nouveau: string } | { modifier: Evenement } | null

/** Un événement couvre-t-il ce jour (fin exclusive) ? */
const couvre = (e: Evenement, jour: string) => jourDe(e.debut) <= jour && jourDe(new Date(new Date(e.fin).getTime() - 1)) >= jour

function CaseEvenement({ e, onOuvrir }: { e: Evenement; onOuvrir: () => void }) {
  const { t } = useTranslation()
  const dates = useDates()
  const horaires = e.journeeEntiere ? t('act.evenement.toutLeJour') : t('act.evenement.deA', { debut: dates.heure(e.debut), fin: dates.heure(e.fin) })
  return (
    <button type="button" onClick={onOuvrir} aria-label={`${horaires} · ${e.titre}`}
      style={{
        display: 'flex', flexDirection: 'column', gap: 'var(--gm-space-1)', alignItems: 'flex-start', width: '100%', textAlign: 'left', cursor: 'pointer', font: 'inherit',
        color: 'var(--gm-color-text-primary)', background: 'var(--gm-color-surface-sunken)', border: 'var(--gm-border-1) solid var(--gm-color-border)', padding: 'var(--gm-space-2)',
      }}>
      <span className="gm-type-small">{horaires}</span>
      <strong className="gm-type-small-strong">{e.titre}</strong>
      <TagCategorie categorie={e.categorie} />
    </button>
  )
}

export default function AgendaPage() {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const session = useSession()
  const permissions = usePermissions()
  const etroit = useEcranEtroit()
  const equipeLisible = aPermission(permissions, PERM.equipeLire)
  const [params, setParams] = useSearchParams()
  const jour = jourDemande(params.get('jour'))
  const collaborateur = equipeLisible ? params.get('collaborateur') ?? undefined : undefined
  const jours = etroit ? [jour] : semaineDe(jour)
  const agenda = useAgenda(jours[0], jours[jours.length - 1], collaborateur)
  const equipe = useEquipe(aujourdhui(), equipeLisible)
  const repondre = useRepondre()
  const [dialogue, setDialogue] = useState<Dialogue>(null)
  const [retour, annoncer] = useRetour()
  const aller = (maj: Record<string, string | undefined>) => {
    const p = new URLSearchParams(params)
    Object.entries(maj).forEach(([k, v]) => (v ? p.set(k, v) : p.delete(k)))
    setParams(p)
  }
  const fait = (message: string) => {
    setDialogue(null)
    aller({ evenement: undefined })
    annoncer(message)
  }
  const evenements = agenda.data ?? []
  const ouvert = evenements.find((e) => e.id === params.get('evenement'))
  const invitations = collaborateur ? [] : evenements.filter((e) => e.participants.some((p) => p.id === session?.sujet && p.reponse === 'EN_ATTENTE'))
  const pas = etroit ? 1 : 7
  const libelle = etroit ? dates.jourLong(jour) : t('act.agenda.semaine', { semaine: semaineIso(jour).slice(6), du: dates.jourCourt(jours[0]), au: dates.jourCourt(jours[6]) })
  return (
    <div className="gm-col">
      <PageHeader
        title={t('act.agenda.titre')}
        subtitle={t('act.agenda.sousTitre')}
        actions={collaborateur ? undefined : <Button variant="primary" icon="plus" onClick={() => setDialogue({ nouveau: jour })}>{t('act.evenement.nouveau')}</Button>}
      />
      <div className="gm-row" style={{ justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 'var(--gm-space-3)' }}>
        <NavigationPeriode
          libelle={libelle}
          precedent={t(etroit ? 'act.jourPrecedent' : 'act.semainePrecedente')}
          suivant={t(etroit ? 'act.jourSuivant' : 'act.semaineSuivante')}
          aujourdhuiLibelle={t('act.aujourdhui')}
          onPrecedent={() => aller({ jour: ajouterJours(jour, -pas) })}
          onSuivant={() => aller({ jour: ajouterJours(jour, pas) })}
          onAujourdhui={() => aller({ jour: undefined })}
        />
        {equipeLisible ? (
          <div style={{ minWidth: 240 }}>
            <Select name="collaborateur" label={t('act.agenda.collaborateur')} value={collaborateur ?? ''} onChange={(v) => aller({ collaborateur: v || undefined, evenement: undefined })}
              options={[{ value: '', label: t('act.agenda.monAgenda') }, ...(equipe.data ?? []).map((m) => ({ value: m.collaborateur.id, label: m.collaborateur.nomAffiche }))]} />
          </div>
        ) : null}
      </div>
      {retour}
      {invitations.length ? (
        <Card title={t('act.agenda.invitations', { count: invitations.length })}>
          {repondre.isError ? <InlineAlert level="crit">{erreur(repondre.error)?.titre}</InlineAlert> : null}
          <ul className="gm-col" style={{ listStyle: 'none', margin: 0, padding: 0, gap: 'var(--gm-space-2)' }}>
            {invitations.map((e) => (
              <li key={e.id} className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'wrap' }}>
                <span style={{ flex: 1 }}>
                  <strong>{e.titre}</strong>
                  <span className="gm-field-help"> · {dates.jourLong(jourDe(e.debut))}, {t('act.evenement.deA', { debut: dates.heure(e.debut), fin: dates.heure(e.fin) })} · {t('act.agenda.par', { nom: e.proprietaire.nomAffiche })}</span>
                </span>
                <Button size="sm" variant="secondary" icon="x" onClick={() => repondre.mutate({ id: e.id, reponse: 'DECLINE' }, { onSuccess: () => annoncer(t('act.evenement.decline')) })}>{t('act.evenement.decliner')}</Button>
                <Button size="sm" variant="primary" icon="check" onClick={() => repondre.mutate({ id: e.id, reponse: 'ACCEPTE' }, { onSuccess: () => annoncer(t('act.evenement.accepte')) })}>{t('act.evenement.accepter')}</Button>
              </li>
            ))}
          </ul>
        </Card>
      ) : null}
      {agenda.isError ? <InlineAlert level="crit" correlationId={agenda.error.correlationId || undefined}>{erreur(agenda.error)?.titre}</InlineAlert> : agenda.isPending ? <Skeleton height={320} /> : (
        <div role="list" aria-label={libelle} style={{ display: 'grid', gridTemplateColumns: `repeat(${jours.length}, minmax(0, 1fr))`, gap: 'var(--gm-space-2)' }}>
          {jours.map((j) => {
            const du = evenements.filter((e) => couvre(e, j))
            return (
              <section role="listitem" key={j} aria-label={dates.jourLong(j)} className="gm-col"
                style={{ gap: 'var(--gm-space-2)', padding: 'var(--gm-space-2)', minHeight: 160, background: 'var(--gm-color-surface)',
                  border: `var(--gm-border-1) solid ${j === aujourdhui() ? 'var(--gm-color-border-strong)' : 'var(--gm-color-border)'}` }}>
                <h2 className="gm-type-small-strong" style={{ margin: 0 }}>{dates.jourCourt(j)}{j === aujourdhui() ? ` · ${t('act.aujourdhui')}` : ''}</h2>
                {du.length ? du.map((e) => <CaseEvenement key={e.id} e={e} onOuvrir={() => aller({ evenement: e.id })} />) : <span className="gm-field-help">{t('act.agenda.libre')}</span>}
              </section>
            )
          })}
        </div>
      )}
      {ouvert && !dialogue ? <DetailEvenement evenement={ouvert} onFermer={() => aller({ evenement: undefined })} onModifier={() => setDialogue({ modifier: ouvert })} onFait={fait} /> : null}
      {dialogue && 'nouveau' in dialogue ? <DialogueEvenement jour={dialogue.nouveau} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {dialogue && 'modifier' in dialogue ? <DialogueEvenement jour={jour} evenement={dialogue.modifier} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
    </div>
  )
}
