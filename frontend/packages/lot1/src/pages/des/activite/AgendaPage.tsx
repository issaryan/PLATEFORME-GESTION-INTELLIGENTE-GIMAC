import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { AgendaGrid, Button, Card, CheckboxGroup, InlineAlert, PageHeader, Select, Skeleton, Tabs, type AgendaEvent } from '@gimac/design-system'
import { aPermission, usePermissions, useSession } from '@gimac/socle'
import { CATEGORIES_EVENEMENT, type CategorieEvenement, type Evenement } from '../../../modele/pilotage'
import { ajouterJours, aujourdhui, jourDe, lundiDe, minutesDe, semaineDe, semaineIso } from '../../../modele/jours'
import { useAgenda, useEquipe, useRepondre } from '../../../hooks/pilotage'
import { PERM } from '../../../permissions'
import { useRetour } from '../parcours/communs'
import { NavigationPeriode, jourDemande, useDates, useEcranEtroit, useErreur } from './communs'
import { DetailEvenement, DialogueEvenement } from './dialoguesAgenda'

// Agenda (D-76, D-113, /des/agenda) : vues jour, semaine et mois sur une grille horaire (AgendaGrid du système de
// conception) ; ligne « toute la journée », chevauchements côte à côte, heure actuelle ; une case libre crée un
// événement à cette heure ; catégories filtrables, chacune d'une teinte de la palette de données avec sa légende ;
// invitations à accepter ou décliner ; agenda d'un membre de l'équipe (PIL_TEAM_READ, lecture seule).
// Adresse : ?vue=jour|semaine|mois, ?jour=AAAA-MM-JJ, ?evenement=<id>, ?collaborateur=<id>, ?categories=A,B.

type Vue = 'jour' | 'semaine' | 'mois'
type Dialogue = { nouveau: string; heure?: number } | { modifier: Evenement } | null

/** Teinte de la palette de données de chaque catégorie (ordre fixe, légende toujours affichée). */
export const COULEUR_CATEGORIE: Record<CategorieEvenement, number> = { REUNION: 1, ACTIVITE: 2, DEPLACEMENT: 4, ABSENCE: 7, FORMATION: 5, AUTRE: 8 }

/** Jours affichés : le jour, sa semaine, ou les six semaines qui couvrent son mois (lundi en tête). */
export function joursDeVue(vue: Vue, jour: string): string[] {
  if (vue === 'jour') return [jour]
  if (vue === 'semaine') return semaineDe(jour)
  const premier = lundiDe(`${jour.slice(0, 7)}-01`)
  return Array.from({ length: 42 }, (_, i) => ajouterJours(premier, i))
}

/** Pas de navigation : un jour, une semaine, ou le même jour du mois voisin. */
export function voisin(vue: Vue, jour: string, sens: 1 | -1): string {
  if (vue === 'jour') return ajouterJours(jour, sens)
  if (vue === 'semaine') return ajouterJours(jour, 7 * sens)
  const [a, m] = jour.slice(0, 7).split('-').map(Number)
  const d = new Date(Date.UTC(a, m - 1 + sens, 1))
  return d.toISOString().slice(0, 10)
}

export default function AgendaPage() {
  const { t, i18n } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const session = useSession()
  const permissions = usePermissions()
  const etroit = useEcranEtroit()
  const equipeLisible = aPermission(permissions, PERM.equipeLire)
  const [params, setParams] = useSearchParams()
  const jour = jourDemande(params.get('jour'))
  const demandee = params.get('vue')
  const vue: Vue = etroit ? 'jour' : demandee === 'jour' || demandee === 'mois' ? demandee : 'semaine'
  const collaborateur = equipeLisible ? params.get('collaborateur') ?? undefined : undefined
  const filtre = params.get('categories')
  const categories = filtre ? (filtre.split(',').filter((c) => (CATEGORIES_EVENEMENT as readonly string[]).includes(c)) as CategorieEvenement[]) : [...CATEGORIES_EVENEMENT]
  const jours = joursDeVue(vue, jour)
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
  const enAttente = (e: Evenement) => e.participants.some((p) => p.id === session?.sujet && p.reponse === 'EN_ATTENTE')
  const invitations = collaborateur ? [] : evenements.filter(enAttente)
  const affiches = useMemo<AgendaEvent[]>(
    () => evenements
      .filter((e) => categories.includes(e.categorie))
      .map((e) => ({ id: e.id, titre: e.titre, debut: e.debut, fin: e.fin, journeeEntiere: e.journeeEntiere, categorie: t(`act.categorie.${e.categorie}`), couleur: COULEUR_CATEGORIE[e.categorie] ?? 8, lieu: e.lieu, enAttente: !collaborateur && enAttente(e) })),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [evenements, filtre, collaborateur, t],
  )
  const moisLibelle = new Intl.DateTimeFormat(i18n.language, { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${jour.slice(0, 7)}-15T12:00:00Z`))
  const libelle = vue === 'jour' ? dates.jourLong(jour)
    : vue === 'semaine' ? t('act.agenda.semaine', { semaine: semaineIso(jour).slice(6), du: dates.jourCourt(jours[0]), au: dates.jourCourt(jours[6]) })
    : moisLibelle
  const lecture = !!collaborateur
  return (
    <div className="gm-col">
      <PageHeader
        title={t('act.agenda.titre')}
        subtitle={t('act.agenda.sousTitre')}
        actions={lecture ? undefined : <Button variant="primary" icon="plus" onClick={() => setDialogue({ nouveau: jour })}>{t('act.evenement.nouveau')}</Button>}
      />
      <div className="gm-row" style={{ justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 'var(--gm-space-3)' }}>
        <NavigationPeriode
          libelle={libelle}
          precedent={t(`act.agenda.precedent.${vue}`)}
          suivant={t(`act.agenda.suivant.${vue}`)}
          aujourdhuiLibelle={t('act.aujourdhui')}
          onPrecedent={() => aller({ jour: voisin(vue, jour, -1) })}
          onSuivant={() => aller({ jour: voisin(vue, jour, 1) })}
          onAujourdhui={() => aller({ jour: undefined })}
        />
        <div className="gm-row" style={{ gap: 'var(--gm-space-3)', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          {etroit ? null : (
            <Tabs
              value={vue}
              onChange={(v) => aller({ vue: v === 'semaine' ? undefined : v })}
              items={(['jour', 'semaine', 'mois'] as const).map((v) => ({ key: v, label: t(`act.agenda.vue.${v}`) }))}
            />
          )}
          {equipeLisible ? (
            <div style={{ minWidth: 240 }}>
              <Select name="collaborateur" label={t('act.agenda.collaborateur')} value={collaborateur ?? ''} onChange={(v) => aller({ collaborateur: v || undefined, evenement: undefined })}
                options={[{ value: '', label: t('act.agenda.monAgenda') }, ...(equipe.data ?? []).map((m) => ({ value: m.collaborateur.id, label: m.collaborateur.nomAffiche }))]} />
            </div>
          ) : null}
        </div>
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
      <div className="gm-row" style={{ alignItems: 'flex-start', gap: 'var(--gm-space-4)', flexWrap: 'wrap' }}>
        <div style={{ flex: '1 1 640px', minWidth: 0 }}>
          {agenda.isError ? <InlineAlert level="crit" correlationId={agenda.error.correlationId || undefined}>{erreur(agenda.error)?.titre}</InlineAlert> : agenda.isPending ? <Skeleton height={480} /> : (
            <AgendaGrid
              vue={vue}
              jours={jours}
              mois={vue === 'mois' ? jour.slice(0, 7) : undefined}
              evenements={affiches}
              aujourdhui={aujourdhui()}
              maintenant={new Date().toISOString()}
              label={libelle}
              libelleJour={vue === 'mois' ? dates.jourLong : dates.jourCourt}
              heure={dates.heure}
              jourDe={jourDe}
              minutesDe={minutesDe}
              onOuvrir={(id) => aller({ evenement: id })}
              onCreer={lecture ? undefined : (j, h) => setDialogue({ nouveau: j, heure: h })}
              onVoirJour={(j) => aller({ vue: 'jour', jour: j })}
            />
          )}
        </div>
        <aside style={{ flex: '0 0 220px' }} aria-label={t('act.agenda.categories')}>
          <CheckboxGroup
            label={t('act.agenda.categories')}
            value={categories}
            onChange={(l) => aller({ categories: l.length === CATEGORIES_EVENEMENT.length ? undefined : l.join(',') || 'AUCUNE' })}
            options={CATEGORIES_EVENEMENT.map((c) => ({ value: c, label: t(`act.categorie.${c}`) }))}
          />
          <ul className="gm-agenda-legende" aria-label={t('act.agenda.legende')}>
            {CATEGORIES_EVENEMENT.map((c) => (
              <li key={c}><span className="gm-agenda-pastille" style={{ ['--gm-agenda-couleur' as string]: `var(--gm-data-${COULEUR_CATEGORIE[c]})` }} aria-hidden="true" />{t(`act.categorie.${c}`)}</li>
            ))}
            <li><span className="gm-agenda-pastille gm-agenda-legende-attente" aria-hidden="true" />{t('act.agenda.enAttente')}</li>
          </ul>
        </aside>
      </div>
      {ouvert && !dialogue ? <DetailEvenement evenement={ouvert} onFermer={() => aller({ evenement: undefined })} onModifier={() => setDialogue({ modifier: ouvert })} onFait={fait} /> : null}
      {dialogue && 'nouveau' in dialogue ? <DialogueEvenement jour={dialogue.nouveau} heure={dialogue.heure} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {dialogue && 'modifier' in dialogue ? <DialogueEvenement jour={jour} evenement={dialogue.modifier} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
    </div>
  )
}
