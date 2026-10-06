import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useSearchParams } from 'react-router'
import { Button, Card, ConfirmDialog, DataTable, EmptyState, IconButton, InlineAlert, Link, LinkList, PageHeader, Skeleton, type Column } from '@gimac/design-system'
import { aPermission, usePermissions } from '@gimac/socle'
import { STATUTS_ACTIVITE, type Activite, type Rapport, type StatutActivite } from '../../../modele/pilotage'
import { ajouterJours, aujourdhui } from '../../../modele/jours'
import { useActivites, useAgenda, useChangerStatut, useEchangerActivites, useEquipe, useRapports, useSupprimerActivite, useTaches } from '../../../hooks/pilotage'
import { PERM } from '../../../permissions'
import { useRetour } from '../parcours/communs'
import { etatListe } from '../liaisons/communs'
import { BadgeActivite, BadgeRapport, BadgeTache, NavigationPeriode, TagCategorie, jourDemande, useDates, useErreur } from './communs'
import { DialogueActivite, DialogueReport } from './dialoguesActivite'
import { MesEtapes } from '../processus/MesEtapes'
import { ConsultationRapport, DialogueRapport } from './dialoguesRapport'

// Ma journée (D-76, /des/activite) : activités du jour (ajout, modification, statut, report, ordre),
// événements du jour, tâches ouvertes échues ou en retard, état du rapport du jour et sa rédaction.
// ?jour=AAAA-MM-JJ change de jour ; ?collaborateur=<id> (PIL_TEAM_READ) lit la journée d'un membre de
// l'équipe, sans geste. Toute création et modification se fait dans une fenêtre modale.

const STATUTS_CHOISIS: StatutActivite[] = STATUTS_ACTIVITE.filter((s) => s !== 'REPORTEE')

type Dialogue = { ajout: true } | { modifier: Activite } | { reporter: Activite } | { supprimer: Activite } | { rediger: true } | { rapport: string } | null

export default function MaJourneePage() {
  const { t } = useTranslation()
  const dates = useDates()
  const erreur = useErreur()
  const permissions = usePermissions()
  const [params, setParams] = useSearchParams()
  const jour = jourDemande(params.get('jour'))
  const collaborateur = aPermission(permissions, PERM.equipeLire) ? params.get('collaborateur') ?? undefined : undefined
  const lecture = !!collaborateur
  const activites = useActivites(jour, jour, collaborateur)
  const agenda = useAgenda(jour, jour, collaborateur)
  const taches = useTaches({ vue: 'moi', taille: 100 }, !lecture)
  const rapports = useRapports({ vue: 'moi', taille: 100 }, !lecture)
  const equipe = useEquipe(jour, lecture)
  const statut = useChangerStatut()
  const supprimer = useSupprimerActivite()
  const echanger = useEchangerActivites()
  const [dialogue, setDialogue] = useState<Dialogue>(null)
  const [retour, annoncer] = useRetour()
  const fait = (message: string) => {
    setDialogue(null)
    annoncer(message)
  }
  const aller = (maj: Record<string, string | undefined>) => {
    const p = new URLSearchParams(params)
    Object.entries(maj).forEach(([k, v]) => (v ? p.set(k, v) : p.delete(k)))
    setParams(p)
  }
  const nom = equipe.data?.find((m) => m.collaborateur.id === collaborateur)?.collaborateur.nomAffiche
  const lignes = activites.data ?? []
  const echues = (taches.data?.contenu ?? []).filter((x) => x.etat !== 'TERMINEE' && x.etat !== 'ANNULEE' && x.echeance && x.echeance <= jour)
  const rapportDuJour: Rapport | undefined = rapports.data?.contenu.find((r) => r.type === 'QUOTIDIEN' && r.periode === jour)
  const refus = erreur(statut.error ?? supprimer.error ?? echanger.error)

  const colonnes: Column<Activite>[] = [
    ...(lecture ? [] : [{
      key: 'ordre', header: t('act.activite.ordre'), width: 88, render: (a: Activite) => {
        const i = lignes.indexOf(a)
        return (
          <span className="gm-row" style={{ gap: 0, flexWrap: 'nowrap' }}>
            <IconButton size="sm" variant="ghost" icon="arrow-up" label={t('act.activite.monter', { titre: a.titre })} disabled={i === 0 || echanger.isPending}
              onClick={() => echanger.mutate({ a, b: lignes[i - 1] })} />
            <IconButton size="sm" variant="ghost" icon="arrow-down" label={t('act.activite.descendre', { titre: a.titre })} disabled={i === lignes.length - 1 || echanger.isPending}
              onClick={() => echanger.mutate({ a, b: lignes[i + 1] })} />
          </span>
        )
      },
    }]),
    {
      key: 'titre', header: t('act.activite.titre'), render: (a) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <span>{a.titre}</span>
          {a.description ? <span className="gm-field-help">{a.description}</span> : null}
          {a.reporteeAu ? <span className="gm-field-help">{t('act.activite.reporteeLe', { jour: dates.jourLong(a.reporteeAu) })}</span> : null}
        </span>
      ),
    },
    {
      key: 'statut', header: t('act.activite.statut'), render: (a) => (lecture || a.statut === 'REPORTEE' ? <BadgeActivite statut={a.statut} /> : (
        // Liste compacte du système de conception (classe gm-input), libellée pour les technologies d'assistance.
        <select className="gm-input" name={`statut-${a.id}`} aria-label={t('act.activite.statutDe', { titre: a.titre })} value={a.statut} disabled={statut.isPending} style={{ minWidth: 140 }}
          onChange={(e) => {
            const v = e.target.value as StatutActivite
            statut.mutate({ id: a.id, statut: v }, { onSuccess: () => annoncer(t('act.activite.statutChange', { statut: t(`act.statut.${v}`) })) })
          }}>
          {STATUTS_CHOISIS.map((s) => <option key={s} value={s}>{t(`act.statut.${s}`)}</option>)}
        </select>
      )),
    },
    { key: 'duree', header: t('act.activite.duree'), render: (a) => dates.duree(a.dureeMinutes) },
    ...(lecture ? [] : [{
      key: 'actions', header: '', type: 'actions' as const, render: (a: Activite) => (
        <>
          <IconButton size="sm" variant="ghost" icon="pen-line" label={t('act.activite.modifierLibelle', { titre: a.titre })} onClick={() => setDialogue({ modifier: a })} />
          {a.statut !== 'REPORTEE' && a.statut !== 'FAITE' ? <IconButton size="sm" variant="ghost" icon="calendar-clock" label={t('act.activite.reporterLibelle', { titre: a.titre })} onClick={() => setDialogue({ reporter: a })} /> : null}
          <IconButton size="sm" variant="ghost" icon="trash" label={t('act.activite.supprimerLibelle', { titre: a.titre })} onClick={() => setDialogue({ supprimer: a })} />
        </>
      ),
    }]),
  ]

  return (
    <div className="gm-col">
      <PageHeader
        title={lecture ? t('act.journee.de', { nom: nom ?? '' }) : t('act.journee.titre')}
        back={lecture ? { href: '/des/equipe', label: t('act.equipe.titre') } : undefined}
        subtitle={t('act.journee.sousTitre')}
        actions={lecture ? undefined : <Button variant="primary" icon="plus" onClick={() => setDialogue({ ajout: true })}>{t('act.activite.ajouter')}</Button>}
      />
      <NavigationPeriode
        libelle={dates.jourLong(jour)}
        precedent={t('act.jourPrecedent')}
        suivant={t('act.jourSuivant')}
        aujourdhuiLibelle={t('act.aujourdhui')}
        onPrecedent={() => aller({ jour: ajouterJours(jour, -1) })}
        onSuivant={() => aller({ jour: ajouterJours(jour, 1) })}
        onAujourdhui={jour !== aujourdhui() ? () => aller({ jour: undefined }) : undefined}
      />
      {retour}
      <Card title={t('act.journee.activites', { count: lignes.length })}>
        {refus ? <InlineAlert level="crit" correlationId={refus.correlationId}>{refus.titre}</InlineAlert> : null}
        <DataTable columns={colonnes} rows={lignes} rowKey={(a) => a.id} pagination={false} emptyText={t('act.journee.aucuneActivite')} emptyHelp={lecture ? undefined : t('act.journee.aucuneActiviteAide')}
          {...etatListe(activites, lignes)} onRetry={() => void activites.refetch()} />
      </Card>
      <div className="gm-form-grid">
        <Card title={t('act.journee.agenda')} actions={<Link href={`/des/agenda?jour=${jour}${collaborateur ? `&collaborateur=${collaborateur}` : ''}`}>{t('act.journee.ouvrirAgenda')}</Link>}>
          {agenda.isPending ? <Skeleton height={64} /> : agenda.isError ? <InlineAlert level="crit">{erreur(agenda.error)?.titre}</InlineAlert> : (
            <LinkList label={t('act.journee.agenda')} pagination={false} empty={t('act.journee.aucunEvenement')}
              items={agenda.data.map((e) => ({
                key: e.id, href: `/des/agenda?jour=${jour}&evenement=${e.id}${collaborateur ? `&collaborateur=${collaborateur}` : ''}`,
                label: e.journeeEntiere ? `${t('act.evenement.toutLeJour')} · ${e.titre}` : `${t('act.evenement.deA', { debut: dates.heure(e.debut), fin: dates.heure(e.fin) })} · ${e.titre}`,
                meta: <TagCategorie categorie={e.categorie} />,
              }))} />
          )}
        </Card>
        {lecture ? null : (
          <Card title={t('act.journee.taches')} actions={<Link href="/des/taches">{t('act.journee.toutesTaches')}</Link>}>
            {taches.isPending ? <Skeleton height={64} /> : (
              <LinkList label={t('act.journee.taches')} pagination={false} empty={t('act.journee.aucuneTache')}
                items={echues.map((x) => ({ key: x.id, href: `/des/taches?tache=${x.id}`, label: x.titre, meta: <BadgeTache etat={x.etat} enRetard={x.enRetard} /> }))} />
            )}
          </Card>
        )}
      </div>
      {/* Processus d'unité (D-87) : étapes qui m'attendent (exécuter, prendre, signer, valider). */}
      {lecture ? null : <MesEtapes />}
      {lecture ? null : (
        <Card title={t('act.journee.rapport')}>
          <div className="gm-row" style={{ gap: 'var(--gm-space-3)', flexWrap: 'wrap' }}>
            {rapports.isPending ? <Skeleton height={32} width="40%" /> : <BadgeRapport etat={rapportDuJour?.etat ?? 'ABSENT'} />}
            <span className="gm-field-help">{t(`act.journee.rapport.${rapportDuJour?.etat ?? 'ABSENT'}`)}</span>
            <span style={{ marginLeft: 'auto' }}>
              {rapportDuJour ? (
                <Button variant="secondary" icon="file-text" onClick={() => setDialogue({ rapport: rapportDuJour.id })}>{t('act.journee.ouvrirRapport')}</Button>
              ) : (
                <Button variant="primary" icon="pen-line" disabled={rapports.isPending} onClick={() => setDialogue({ rediger: true })}>{t('act.journee.rediger')}</Button>
              )}
            </span>
          </div>
        </Card>
      )}
      {lecture && !activites.isPending && lignes.length === 0 ? <EmptyState icon="calendar" title={t('act.journee.rien')} /> : null}

      {dialogue && 'ajout' in dialogue ? <DialogueActivite jour={jour} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {dialogue && 'modifier' in dialogue ? <DialogueActivite jour={jour} activite={dialogue.modifier} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {dialogue && 'reporter' in dialogue ? <DialogueReport activite={dialogue.reporter} onFermer={() => setDialogue(null)} onFait={fait} /> : null}
      {dialogue && 'supprimer' in dialogue ? (
        <ConfirmDialog danger title={t('act.activite.supprimer')} description={t('act.activite.supprimerTexte')} confirmLabel={t('act.activite.supprimer')}
          object={[{ key: t('act.activite.titre'), value: dialogue.supprimer.titre }]} onClose={() => setDialogue(null)}
          onConfirm={() => supprimer.mutate(dialogue.supprimer.id, { onSuccess: () => fait(t('act.activite.supprimee')), onError: () => setDialogue(null) })} />
      ) : null}
      {dialogue && 'rediger' in dialogue ? (
        <DialogueRapport type="QUOTIDIEN" periode={jour} onFermer={() => setDialogue(null)} onFait={(_r, message) => fait(message)} />
      ) : null}
      {dialogue && 'rapport' in dialogue ? (
        <ConsultationRapport id={dialogue.rapport} onFermer={() => setDialogue(null)} onFait={(_r, message) => fait(message)} />
      ) : null}
    </div>
  )
}
