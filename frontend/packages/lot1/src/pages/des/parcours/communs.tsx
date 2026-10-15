import { useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { DateTime, Link, ProgressRing, StatusBadge, Toast, type Column, type Status } from '@gimac/design-system'
import type { CoverageValue } from '../../../modele/commun'
import type { EtatEtape, EtatServicePerimetre, LiaisonResume, Sla, StatutParcours } from '../../../modele/parcours'
import { useOptionsTypesProjet } from '../../../hooks/referentiels'

// Compositions communes des écrans du parcours (liste, dossier numérique, tableau de bord par acteur) :
// tons des états servis par les services, dates localisées, couverture et échéance. Aucune règle :
// chaque valeur affichée vient du service ; ces tables ne font que traduire un état en ton visuel.

export const tonStatut: Record<StatutParcours, Status> = {
  CADREE: 'info', EN_INTEGRATION: 'info', EN_TEST: 'info', CERTIFIEE: 'ok', APPROUVEE_SECURITE: 'ok', EN_PRODUCTION: 'ok', SUSPENDUE: 'warn', CLOSE_SANS_SUITE: 'neutral',
}
export const tonSla: Record<Sla, Status> = { DANS_LES_DELAIS: 'ok', ECHEANCE_PROCHE: 'warn', DEPASSE: 'crit' }
export const tonEtape: Record<EtatEtape, Status> = { A_VENIR: 'neutral', EN_COURS: 'info', CLOSE: 'ok', BLOQUEE: 'crit', SANS_OBJET: 'neutral' }
export const tonService: Record<EtatServicePerimetre, Status> = { NON_DEMARRE: 'neutral', EN_TEST: 'info', CERTIFIE: 'ok', OUVERT: 'ok', NON_OUVERT: 'warn' }

/** Date ISO (AAAA-MM-JJ) affichée dans la langue courante ; jamais de calcul de délai côté interface. */
export function useFormatDate() {
  const { i18n } = useTranslation()
  const format = new Intl.DateTimeFormat(i18n.language, { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' })
  return (iso: string | null | undefined) => {
    if (!iso) return ''
    const d = new Date(iso.length === 10 ? `${iso}T00:00:00Z` : iso)
    // Une date illisible s'affiche telle quelle plutôt que de faire tomber l'écran.
    return Number.isNaN(d.getTime()) ? iso : format.format(d)
  }
}

/** Date affichée, ou mention explicite de son absence. */
export function Jour({ iso, absent }: { iso: string | null | undefined; absent?: string }) {
  const { t } = useTranslation()
  const date = useFormatDate()
  if (!iso) return <span className="gm-field-help">{absent ?? t('parcoursCommun.nonDefinie')}</span>
  return <DateTime local={date(iso)} iso={iso} />
}

/** Couverture servie (HOM-17) : anneau compact, ratio des services, ou « n/c » avec son motif. */
export function Couverture({ valeur, libelle, avecRatio = true }: { valeur: CoverageValue; libelle: string; avecRatio?: boolean }) {
  const { t } = useTranslation()
  if (!valeur.calculable) {
    return (
      <span className="gm-row" style={{ gap: 'var(--gm-space-1)', flexWrap: 'nowrap' }} title={valeur.motifNonCalculable}>
        <ProgressRing compact label={libelle} value={null} />
      </span>
    )
  }
  const ratio = t('parcoursCommun.ratio', { n: valeur.numerateur, d: valeur.denominateur })
  return (
    <span className="gm-row" style={{ gap: 'var(--gm-space-2)', flexWrap: 'nowrap', whiteSpace: 'nowrap' }} title={ratio}>
      <ProgressRing compact label={libelle} value={valeur.taux} />
      {avecRatio ? <span className="gm-field-help gm-num">{ratio}</span> : null}
    </span>
  )
}

export function BadgeStatut({ statut }: { statut: StatutParcours }) {
  const { t } = useTranslation()
  return <StatusBadge status={tonStatut[statut]} label={t(`parcours.statut.${statut}`)} />
}

export function BadgeSla({ sla }: { sla: Sla | null }) {
  const { t } = useTranslation()
  if (!sla) return <span className="gm-field-help">{t('parcoursCommun.sansEcheance')}</span>
  return <StatusBadge status={tonSla[sla]} label={t(`parcours.sla.${sla}`)} />
}

/** Échéance et son niveau de SLA servis ensemble, sur une ligne. */
export function Echeance({ iso, sla }: { iso: string | null; sla: Sla | null }) {
  return (
    <span className="gm-row" style={{ gap: 'var(--gm-space-2)' }}>
      <Jour iso={iso} />
      {iso && sla ? <BadgeSla sla={sla} /> : null}
    </span>
  )
}

/** Valeur ou mention « non renseigné ». */
export function Valeur({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  return children === null || children === undefined || children === '' ? <span className="gm-field-help">{t('commun.nonRenseigne')}</span> : <>{children}</>
}

/** Conversion d'une saisie JJ/MM/AAAA (DateField) en date ISO ; null si la date n'existe pas. */
export function versIso(saisie: string): string | null {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(saisie.trim())
  if (!m) return null
  const [, j, mo, a] = m
  const d = new Date(`${a}-${mo}-${j}T00:00:00Z`)
  return Number.isNaN(d.getTime()) || d.getUTCDate() !== Number(j) || d.getUTCMonth() + 1 !== Number(mo) ? null : `${a}-${mo}-${j}`
}

/** Retour non bloquant d'un geste réussi (Toast du catalogue, placé comme celui d'ExecuteurActions). */
export function useRetour(): [ReactNode, (message: string) => void] {
  const [message, setMessage] = useState<string | null>(null)
  useEffect(() => {
    if (!message) return
    const id = window.setTimeout(() => setMessage(null), 6000)
    return () => window.clearTimeout(id)
  }, [message])
  const noeud = message ? (
    <div style={{ position: 'fixed', right: 'var(--gm-space-5)', bottom: 'var(--gm-space-5)', zIndex: 60 }} onClick={() => setMessage(null)}>
      <Toast>{message}</Toast>
    </div>
  ) : null
  return [noeud, setMessage]
}

type Traduire = (cle: string, o?: Record<string, unknown>) => string

/** Recette HOM-09 : type de projet nommé par son libellé servi (type créé dans la plateforme, D-91), jamais par son code. */
export function LibelleTypeProjet({ code }: { code: string }) {
  return <>{useOptionsTypesProjet().libelle(code)}</>
}

/** Colonnes d'une liste de liaisons (liste du parcours, tableau de bord) : la référence est un lien au clavier. */
export function colonnesLiaisons(t: Traduire, date: (iso: string) => string, o: { echeances?: boolean; geste?: (l: LiaisonResume) => ReactNode } = {}): Column<LiaisonResume>[] {
  const echeances = o.echeances ?? true
  return [
    { key: 'reference', header: t('parcoursListe.reference'), render: (l) => <span style={{ whiteSpace: 'nowrap' }}><Link href={`/des/parcours/${l.id}`} mono>{l.reference}</Link></span> },
    {
      key: 'institutionNom', header: t('parcoursListe.institution'),
      render: (l) => (
        <span className="gm-col" style={{ gap: 0 }}>
          <span>{l.institutionNom}</span>
          <span className="gm-field-help"><LibelleTypeProjet code={String(l.typeProjet)} /></span>
        </span>
      ),
    },
    {
      key: 'statut', header: t('parcoursListe.statutEtape'),
      render: (l) => (
        <span className="gm-col" style={{ gap: 'var(--gm-space-1)', alignItems: 'flex-start' }}>
          <BadgeStatut statut={l.statut} />
          <span className="gm-field-help">{t(`parcours.etape.${l.etapeCourante}`)}</span>
          {l.bloquee ? <StatusBadge status="crit" label={t('parcoursListe.bloquee')} /> : null}
          {l.tachesEnRetard > 0 ? <StatusBadge status="warn" label={t('parcoursListe.retards', { count: l.tachesEnRetard })} /> : null}
        </span>
      ),
    },
    { key: 'conducteurNom', header: t('parcoursListe.conducteur'), render: (l) => l.conducteurNom ?? <StatusBadge status="warn" label={t('parcoursListe.aDesigner')} /> },
    { key: 'avancement', header: t('parcoursListe.colAvancement'), render: (l) => (l.avancement ? <Couverture valeur={l.avancement} libelle={t('parcoursListe.avancement')} avecRatio={false} /> : null) },
    { key: 'couvertureCertification', header: t('parcoursListe.colCertification'), render: (l) => <Couverture valeur={l.couvertureCertification} libelle={t('parcoursListe.couvertureCertification')} avecRatio={false} /> },
    { key: 'couvertureMiseEnService', header: t('parcoursListe.colMiseEnService'), render: (l) => <Couverture valeur={l.couvertureMiseEnService} libelle={t('parcoursListe.couvertureMiseEnService')} avecRatio={false} /> },
    ...(echeances
      ? [{
          key: 'echeances', header: t('parcoursListe.echeances'),
          render: (l: LiaisonResume) => (
            <span className="gm-col" style={{ gap: 0 }}>
              <Jour iso={l.prochaineEcheance} absent={t('parcoursListe.aucuneEcheance')} />
              <span className="gm-field-help">{t('parcoursListe.cible', { date: l.dateCibleProduction ? date(l.dateCibleProduction) : t('parcoursCommun.nonDefinie') })}</span>
            </span>
          ),
        }]
      : []),
    ...(o.geste ? [{ key: 'geste', header: '', type: 'actions' as const, render: o.geste }] : []),
  ]
}
