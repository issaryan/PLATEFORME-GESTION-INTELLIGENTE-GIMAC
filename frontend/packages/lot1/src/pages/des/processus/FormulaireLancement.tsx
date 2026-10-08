import { useTranslation } from 'react-i18next'
import { Checkbox, DateField, FileDropzone, KeyValueList, NumberField, Select, TextArea, TextField } from '@gimac/design-system'
import { LIMITES_FORMULAIRE, type ChampFormulaire, type Lancement, type PieceJointe, type ValeurChampFormulaire } from '../../../modele/processus'
import { depuisSaisie, versSaisie } from '../../../modele/jours'
import { useDates } from '../activite/communs'

// Formulaire de lancement d'un processus (D-98 §2, docs/lot1/lancement-processus.md) : champs typés en données dans la
// définition (texte, texte long, nombre, date, période, liste, case, pièce jointe), saisis par l'initiateur, contrôlés au
// lancement par MS-PILOTAGE (422 PIL_PROCESSUS_FORMULAIRE_INVALIDE, fieldErrors « formulaire.CODE » lus ici) puis visibles
// des intervenants à chaque étape. La pièce jointe part par le dépôt fragmenté (finalité PROCESSUS_UNITE, référence : la
// définition lancée) juste avant le lancement.

/** Saisie d'un champ : texte (dates en JJ/MM/AAAA), case, période, fichier choisi. */
export type SaisieChamp = string | boolean | { debut: string; fin: string } | File | null
export type SaisieFormulaire = Record<string, SaisieChamp>

const taille = (o: number) => (o < 1024 ? `${o} o` : o < 1_048_576 ? `${Math.round(o / 1024)} Ko` : `${(o / 1_048_576).toFixed(1)} Mo`)
const typeAdmis = (types: string[], f: File) => !types.length || types.some((t) => t === f.type || (t.endsWith('/*') && f.type.startsWith(t.slice(0, -1))))

/** Erreurs de forme, annoncées avant l'envoi (le service refait tout). */
export function erreursFormulaire(champs: ChampFormulaire[], s: SaisieFormulaire, t: (k: string, o?: Record<string, unknown>) => string): Record<string, string | undefined> {
  return Object.fromEntries(champs.map((c) => {
    const v = s[c.code] ?? null
    const vide = v === null || v === '' || v === false || (typeof v === 'object' && !(v instanceof File) && !v.debut && !v.fin)
    if (vide) return [c.code, c.obligatoire ? t(c.type === 'CASE' ? 'dem.erreur.cocher' : 'proc.erreur.obligatoire') : undefined]
    switch (c.type) {
      case 'TEXTE': return [c.code, String(v).length > LIMITES_FORMULAIRE.texte ? t('dem.erreur.long', { n: LIMITES_FORMULAIRE.texte }) : undefined]
      case 'TEXTE_LONG': return [c.code, String(v).length > LIMITES_FORMULAIRE.texteLong ? t('dem.erreur.long', { n: LIMITES_FORMULAIRE.texteLong }) : undefined]
      case 'NOMBRE': return [c.code, Number.isNaN(Number(String(v).replace(',', '.'))) ? t('proc.erreur.nombre') : undefined]
      case 'DATE': return [c.code, depuisSaisie(String(v)) ? undefined : t('proc.erreur.date')]
      case 'PERIODE': {
        const p = v as { debut: string; fin: string }
        const debut = depuisSaisie(p.debut)
        const fin = depuisSaisie(p.fin)
        if (!debut || !fin) return [c.code, t('dem.erreur.periode')]
        return [c.code, fin < debut ? t('dem.erreur.periodeOrdre') : undefined]
      }
      case 'PIECE_JOINTE': return [c.code, v instanceof File && !typeAdmis(c.typesPieces, v) ? t('dem.erreur.typePiece', { types: c.typesPieces.join(', ') }) : undefined]
      default: return [c.code, undefined]
    }
  }))
}

/** Valeurs envoyées au service ; une pièce jointe est remplacée par le ticket de son dépôt ({ documentId }). */
export function valeursEnvoyees(champs: ChampFormulaire[], s: SaisieFormulaire, pieces: Record<string, string>): NonNullable<Lancement['formulaire']> {
  const r: NonNullable<Lancement['formulaire']> = {}
  for (const c of champs) {
    const v = s[c.code] ?? null
    if (v === null || v === '') continue
    switch (c.type) {
      case 'NOMBRE': r[c.code] = Number(String(v).replace(',', '.')); break
      case 'DATE': r[c.code] = depuisSaisie(String(v)) ?? String(v); break
      case 'PERIODE': {
        const p = v as { debut: string; fin: string }
        r[c.code] = { debut: depuisSaisie(p.debut) ?? p.debut, fin: depuisSaisie(p.fin) ?? p.fin }
        break
      }
      case 'CASE': r[c.code] = v === true; break
      case 'PIECE_JOINTE': if (pieces[c.code]) r[c.code] = { documentId: pieces[c.code] }; break
      default: r[c.code] = String(v).trim()
    }
  }
  return r
}

/** Saisie des champs du formulaire de lancement, avec l'aide de chaque champ et les erreurs (de forme ou du service). */
export function ChampsFormulaire({ champs, valeurs, onChange, erreurs }: {
  champs: ChampFormulaire[]; valeurs: SaisieFormulaire; onChange: (code: string, v: SaisieChamp) => void; erreurs: Record<string, string | undefined>
}) {
  const { t } = useTranslation()
  return (
    <div className="gm-form-grid gm-mod-grille" data-testid="formulaire-lancement">
      {champs.map((c) => {
        const commun = { name: `formulaire.${c.code}`, label: c.libelle, required: c.obligatoire, help: c.aide ?? undefined, error: erreurs[c.code] }
        const v = valeurs[c.code] ?? null
        const large = c.type === 'TEXTE_LONG' || c.type === 'PERIODE' || c.type === 'PIECE_JOINTE' || c.type === 'CASE'
        let champ
        switch (c.type) {
          case 'TEXTE_LONG':
            champ = <TextArea {...commun} rows={3} maxLength={LIMITES_FORMULAIRE.texteLong} value={typeof v === 'string' ? v : ''} onChange={(x) => onChange(c.code, x)} />
            break
          case 'NOMBRE':
            champ = <NumberField {...commun} value={typeof v === 'string' ? v : ''} onChange={(x) => onChange(c.code, x)} />
            break
          case 'DATE':
            champ = <DateField {...commun} value={typeof v === 'string' ? v : ''} onChange={(x) => onChange(c.code, x)} />
            break
          case 'PERIODE': {
            const p = v && typeof v === 'object' && !(v instanceof File) ? v : { debut: '', fin: '' }
            champ = (
              <fieldset className="gm-proc-element" aria-describedby={c.aide ? `formulaire-${c.code}-aide` : undefined}>
                <legend className="gm-type-body-strong">{c.libelle}{c.obligatoire ? ' *' : ''}</legend>
                {c.aide ? <span id={`formulaire-${c.code}-aide`} className="gm-field-help">{c.aide}</span> : null}
                <div className="gm-row" style={{ gap: 'var(--gm-space-3)', flexWrap: 'wrap', alignItems: 'flex-start' }}>
                  <DateField name={`formulaire.${c.code}.debut`} label={t('dem.periode.du')} required={c.obligatoire} value={p.debut} onChange={(x) => onChange(c.code, { ...p, debut: x })} />
                  <DateField name={`formulaire.${c.code}.fin`} label={t('dem.periode.au')} required={c.obligatoire} value={p.fin} onChange={(x) => onChange(c.code, { ...p, fin: x })} error={erreurs[c.code]} />
                </div>
              </fieldset>
            )
            break
          }
          case 'LISTE':
            champ = <Select {...commun} value={typeof v === 'string' ? v : ''} onChange={(x) => onChange(c.code, x)} options={[{ value: '', label: t('proc.aucun') }, ...c.choix.map((x) => ({ value: x, label: x }))]} />
            break
          case 'CASE':
            champ = <Checkbox name={commun.name} label={`${c.libelle}${c.obligatoire ? ' *' : ''}`} checked={v === true} onChange={(x) => onChange(c.code, x)} error={erreurs[c.code]} />
            break
          case 'PIECE_JOINTE': {
            const f = v instanceof File ? v : null
            champ = (
              <FileDropzone {...commun} accept={c.typesPieces.join(',') || undefined} help={[c.aide, c.typesPieces.length ? t('proc.suivi.types', { types: c.typesPieces.join(', ') }) : null].filter(Boolean).join(' ') || undefined}
                file={f ? { name: f.name, size: taille(f.size) } : undefined} state={f ? 'done' : 'idle'} onFile={(x) => onChange(c.code, x)} onRemove={() => onChange(c.code, null)} />
            )
            break
          }
          default:
            champ = <TextField {...commun} maxLength={LIMITES_FORMULAIRE.texte} value={typeof v === 'string' ? v : ''} onChange={(x) => onChange(c.code, x)} />
        }
        return <div key={c.code} className={large ? 'gm-form-field gm-form-wide' : 'gm-form-field'}>{champ}</div>
      })}
    </div>
  )
}

/** Valeur lisible d'un champ rempli (date JJ/MM/AAAA, période, case, pièce jointe). */
export function useValeurLisible() {
  const { t } = useTranslation()
  const dates = useDates()
  return (c: ValeurChampFormulaire): string => {
    const v = c.valeur
    if (v === null || v === undefined || v === '') return t('proc.suivi.nonRenseigne')
    switch (c.type) {
      case 'DATE': return /^\d{4}-\d{2}-\d{2}$/.test(String(v)) ? versSaisie(String(v)) : String(v)
      case 'PERIODE': {
        const p = v as { debut: string; fin: string }
        return t('dem.periode.valeur', { du: dates.jour(p.debut), au: dates.jour(p.fin) })
      }
      case 'CASE': return t(v === true ? 'dem.case.oui' : 'dem.case.non')
      case 'PIECE_JOINTE': {
        const p = v as PieceJointe
        return `${p.nomFichier} · ${taille(p.taille)}`
      }
      default: return String(v)
    }
  }
}

/** Valeurs du formulaire de lancement en lecture (instance, à chaque étape). */
export function ValeursFormulaire({ valeurs }: { valeurs: ValeurChampFormulaire[] }) {
  const lisible = useValeurLisible()
  return <KeyValueList items={valeurs.map((c) => ({ key: c.libelle, value: lisible(c), mono: c.type === 'PIECE_JOINTE' }))} />
}
