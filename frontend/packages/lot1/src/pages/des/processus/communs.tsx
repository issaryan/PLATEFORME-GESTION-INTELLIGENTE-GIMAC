import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Select, StatusBadge } from '@gimac/design-system'
import { estApiError } from '@gimac/socle'
import { PROFILS_DOMAINE } from '../../../domaine'
import { useLecture } from '../../../hooks/base'
import { INITIATEURS_DEFAUT, delaiMaxEffectif, type CandidatsProcessus, type EtatEtape, type EtatInstance, type EtatProcessus, type GraviteAnomalie, type Initiateurs, type Intervenant } from '../../../modele/processus'
import { useLibelleProfilEquipe } from '../activite/dialoguesEquipe'
import { useLibelleUnite } from '../activite/communs'
import './processus.css'

// Compositions communes des processus d'unité (D-87) : pastilles d'état, résumé d'un intervenant, refus traduits,
// choix d'une personne (candidats dans l'unité, puis ailleurs dans le GIMAC, puis l'annuaire interne).

type Statut = 'ok' | 'warn' | 'crit' | 'info' | 'neutral'
const ETAT_PROCESSUS: Record<EtatProcessus, Statut> = { BROUILLON: 'info', PUBLIEE: 'ok', RETIREE: 'neutral' }
const ETAT_INSTANCE: Record<EtatInstance, Statut> = { EN_COURS: 'info', SUSPENDUE: 'warn', CLOSE: 'ok', ANNULEE: 'neutral' }
const ETAT_ETAPE: Record<EtatEtape, Statut> = { EN_ATTENTE: 'neutral', ACTIVE: 'info', A_SIGNER: 'warn', A_VALIDER: 'warn', TERMINEE: 'ok', ANNULEE: 'neutral' }
const GRAVITE: Record<GraviteAnomalie, Statut> = { BLOQUANTE: 'crit', LEVABLE: 'warn', AVERTISSEMENT: 'info' }

export function BadgeProcessus({ etat }: { etat: EtatProcessus }) {
  const { t } = useTranslation()
  return <StatusBadge status={ETAT_PROCESSUS[etat]} label={t(`proc.etat.${etat}`)} />
}
export function BadgeInstance({ etat }: { etat: EtatInstance }) {
  const { t } = useTranslation()
  return <StatusBadge status={ETAT_INSTANCE[etat]} label={t(`proc.instance.etat.${etat}`)} />
}
export function BadgeEtape({ etat }: { etat: EtatEtape }) {
  const { t } = useTranslation()
  return <StatusBadge status={ETAT_ETAPE[etat]} label={t(`proc.etape.etat.${etat}`)} />
}
export function BadgeGravite({ gravite }: { gravite: GraviteAnomalie }) {
  const { t } = useTranslation()
  return <StatusBadge status={GRAVITE[gravite]} label={t(`proc.gravite.${gravite}`)} />
}

/**
 * Profils proposés aux intervenants et au public d'un processus : tous les profils internes du domaine « gimac »
 * (catalogue réel domaine-gimac.json, écrit par generer-realm-gimac.py), sans les profils externes d'institution ;
 * MS-PILOTAGE vérifie leur existence.
 */
export const PROFILS_INTERVENANTS = PROFILS_DOMAINE.filter((p) => !p.externe).map((p) => p.code).sort()

/** Au-delà de ce nombre, les personnes désignées du résumé sont comptées plutôt que nommées. */
const NOMS_RESUMES = 3

/**
 * Résumé du public d'un processus, « Visible et lançable par … » (D-98, D-100) : chaque règle en clair, unités et profils
 * nommés par leur libellé, personnes désignées par leur nom (les trois premières, puis leur nombre).
 */
export function useResumeInitiateurs() {
  const { t } = useTranslation()
  const libelleUnite = useLibelleUnite()
  const libelleProfil = useLibelleProfilEquipe()
  return (i: Initiateurs | undefined, unite?: string) => {
    const x = i ?? INITIATEURS_DEFAUT
    return x.regles.map((r) => {
      switch (r) {
        case 'MEMBRES_UNITE': return unite ? t('dem.resume.MEMBRES_UNITE', { unite: libelleUnite(unite) }) : t('dem.regle.MEMBRES_UNITE')
        case 'UNITES': return `${t('dem.regle.UNITES')} (${x.unites.map(libelleUnite).join(', ')})`
        case 'PROFILS': return `${t('dem.regle.PROFILS')} (${x.profils.map((c) => libelleProfil(c)).join(', ')})`
        case 'PERMISSIONS': return `${t('dem.regle.PERMISSIONS')} (${x.permissions.join(', ')})`
        case 'PERSONNES': {
          const ids = x.personnes ?? []
          const noms = ids.map((id) => x.personnesDetail?.find((d) => d.id === id)?.nomAffiche ?? id)
          const liste = noms.length > NOMS_RESUMES ? t('dem.resume.etAutres', { noms: noms.slice(0, NOMS_RESUMES).join(', '), count: noms.length - NOMS_RESUMES }) : noms.join(', ')
          return `${t('dem.regle.PERSONNES')} (${liste})`
        }
        default: return t(`dem.regle.${r}`)
      }
    }).join(' ; ')
  }
}

/** Résumé lisible d'un intervenant (« Profil : Pôle réseau », « Chef de l'unité »…). */
export function useResumeIntervenant() {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfilEquipe()
  const libelleUnite = useLibelleUnite()
  return (i: Intervenant | null | undefined, personnes: Record<string, string> = {}) => {
    if (!i) return t('proc.intervenant.aucun')
    const unite = i.unite ? ` (${libelleUnite(i.unite)})` : ''
    switch (i.mode) {
      case 'PROFIL': return t('proc.intervenant.resume.PROFIL', { profil: i.profil ? libelleProfil(i.profil) : '?' }) + unite
      case 'PERSONNE': return t('proc.intervenant.resume.PERSONNE', { nom: i.personneId ? personnes[i.personneId] ?? i.personneId : '?' })
      case 'AU_LANCEMENT': return t('proc.intervenant.resume.AU_LANCEMENT') + unite
      case 'CHEF_UNITE': return t('proc.intervenant.resume.CHEF_UNITE') + unite
      default: return t(`proc.intervenant.resume.${i.mode}`)
    }
  }
}

/** Refus de MS-PILOTAGE traduit par son code (proc.erreur.<code>, puis act.erreur.<code>), message du service à défaut. */
export function useErreurProcessus() {
  const { t } = useTranslation()
  return (e: unknown): { titre: string; correlationId?: string; code?: string } | undefined => {
    if (!e) return undefined
    if (estApiError(e)) {
      return { titre: t(`proc.erreur.${e.code}`, { defaultValue: t(`act.erreur.${e.code}`, { defaultValue: e.message || t('proc.erreur.generique') }) }), correlationId: e.correlationId || undefined, code: e.code }
    }
    return { titre: e instanceof Error ? e.message : t('proc.erreur.generique') }
  }
}

/** Annuaire interne (MS-COLLABORATION) : personnes proposées aux désignations et réaffectations. */
export const useAnnuaireProcessus = (actif = true) => useLecture(['collaboration', 'annuaire', 'processus'], (s) => s.messagerie.annuaire('', 0, 50), { enabled: actif, staleTime: 60_000 })

/**
 * Choix d'une personne (désignation hors de l'unité) : candidats servis par MS-PILOTAGE groupés « Dans l'unité » puis
 * « Ailleurs dans le GIMAC » (avec leur unité), puis les autres personnes de l'annuaire. Un seul candidat : il est
 * choisi d'office (une fois, la personne peut ensuite changer). Sans candidats (profil absent) : l'annuaire, les
 * porteurs du profil d'abord.
 */
export function ChoixPersonne({ name, label, value, onChange, profil, candidats, chargement, help, error, required, exclure = [] }: {
  name: string; label: string; value: string; onChange: (v: string) => void; profil?: string | null; candidats?: CandidatsProcessus; chargement?: boolean
  help?: string; error?: string; required?: boolean; exclure?: string[]
}) {
  const { t } = useTranslation()
  const annuaire = useAnnuaireProcessus()
  const libelleProfil = useLibelleProfilEquipe()
  const libelleUnite = useLibelleUnite()
  // D-99 : l'initiateur nommé par le service (« exclus ») n'est proposé nulle part, pas même dans l'annuaire.
  const exclus = [...exclure, ...(candidats?.exclus ?? [])]
  const dans = (candidats?.dansUnite ?? []).filter((c) => !exclus.includes(c.id))
  const ailleurs = (candidats?.ailleurs ?? []).filter((c) => !exclus.includes(c.id))
  const proposes = new Set([...dans, ...ailleurs].map((c) => c.id))
  const l = (annuaire.data?.contenu ?? []).filter((p) => !exclus.includes(p.id) && !proposes.has(p.id))
  const porteurs = profil && !candidats ? l.filter((p) => p.profils?.includes(profil)) : []
  const autres = l.filter((p) => !porteurs.includes(p))
  // QUO-02 : « seul porteur » seulement s'il l'est vraiment (un porteur écarté, l'exécutant actuel par exemple, compte).
  const porteursServis = (candidats?.dansUnite.length ?? 0) + (candidats?.ailleurs.length ?? 0)
  const seul = candidats && porteursServis === 1 && dans.length + ailleurs.length === 1 ? (dans[0] ?? ailleurs[0]) : null
  const rempli = useRef(false)
  useEffect(() => {
    if (seul && !value && !rempli.current) {
      rempli.current = true
      onChange(seul.id)
    }
  }, [seul, value, onChange])
  const fonction = (f: string | null | undefined) => (f ? ` · ${f}` : '')
  const groupe = candidats ? t('proc.personne.autres') : undefined
  const options = [
    { value: '', label: t('proc.personne.choisir') },
    ...dans.map((c) => ({ value: c.id, label: `${c.nomAffiche}${fonction(c.fonction)}`, group: t('proc.personne.dansUnite') })),
    ...ailleurs.map((c) => ({ value: c.id, label: `${c.nomAffiche} · ${c.unite ? libelleUnite(c.unite) : t('proc.suivi.toutLeGimac')}${fonction(c.fonction)}`, group: t('proc.personne.ailleurs') })),
    ...[...porteurs, ...autres].map((p) => ({ value: p.id, label: `${p.nom}${fonction(p.fonction)}`, group: groupe })),
  ]
  const libelle = profil ? libelleProfil(profil) : ''
  const aide = help ?? (chargement ? t('proc.personne.chargement')
    : candidats && profil ? (seul ? t('proc.personne.seul', { profil: libelle }) : !dans.length && !ailleurs.length ? t(porteursServis ? 'proc.personne.aucunAutre' : 'proc.personne.aucunPorteur', { profil: libelle })
      : !dans.length ? t('proc.personne.horsUnite', { profil: libelle }) : t('proc.personne.aideProfil', { profil: libelle }))
      : profil ? t('proc.personne.aideProfil', { profil: libelle }) : undefined)
  return (
    <Select name={name} label={label} required={required} help={aide} error={error} value={value} onChange={onChange} disabled={annuaire.isPending && !candidats} options={options} />
  )
}

/**
 * Délai d'une étape ou d'un processus en clair (D-106) : « 3 j » quand le délai maximal égale le délai cible, sinon
 * « 3 à 5 j » (cible à maximum).
 */
export function delais(t: (k: string, o?: Record<string, unknown>) => string, cible: number, max?: number | null): string {
  const m = delaiMaxEffectif(cible, max)
  return m === cible ? t('proc.carte.delai', { n: cible }) : t('proc.carte.delais', { cible, max: m })
}
