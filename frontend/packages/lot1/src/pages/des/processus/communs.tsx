import { useTranslation } from 'react-i18next'
import { Select, StatusBadge } from '@gimac/design-system'
import { estApiError } from '@gimac/socle'
import { PROFILS } from '../../../permissions'
import { useLecture } from '../../../hooks/base'
import type { EtatEtape, EtatInstance, EtatProcessus, GraviteAnomalie, Intervenant } from '../../../modele/processus'
import { useLibelleProfilEquipe } from '../activite/dialoguesEquipe'
import { useLibelleUnite } from '../activite/communs'
import './processus.css'

// Compositions communes des processus d'unité (D-87) : pastilles d'état, résumé d'un intervenant, refus traduits,
// choix d'une personne de l'annuaire interne (porteurs du profil demandé d'abord).

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

/** Profils proposés aux intervenants : ceux du domaine, hors profil d'institution ; MS-PILOTAGE vérifie leur existence. */
export const PROFILS_INTERVENANTS = Object.keys(PROFILS).filter((p) => p !== 'ROLE_CORRESPONDANT').sort()

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

/** Choix d'une personne de l'annuaire : les porteurs du profil indiqué d'abord. */
export function ChoixPersonne({ name, label, value, onChange, profil, help, error, required, exclure = [] }: {
  name: string; label: string; value: string; onChange: (v: string) => void; profil?: string | null; help?: string; error?: string; required?: boolean; exclure?: string[]
}) {
  const { t } = useTranslation()
  const annuaire = useAnnuaireProcessus()
  const libelleProfil = useLibelleProfilEquipe()
  const l = (annuaire.data?.contenu ?? []).filter((p) => !exclure.includes(p.id))
  const porteurs = profil ? l.filter((p) => p.profils?.includes(profil)) : []
  const autres = l.filter((p) => !porteurs.includes(p))
  const option = (p: (typeof l)[number]) => ({ value: p.id, label: `${p.nom}${p.fonction ? ` · ${p.fonction}` : ''}` })
  return (
    <Select name={name} label={label} required={required} help={help ?? (profil ? t('proc.personne.aideProfil', { profil: libelleProfil(profil) }) : undefined)} error={error} value={value} onChange={onChange}
      disabled={annuaire.isPending}
      options={[{ value: '', label: t('proc.personne.choisir') }, ...porteurs.map(option), ...autres.map(option)]} />
  )
}
