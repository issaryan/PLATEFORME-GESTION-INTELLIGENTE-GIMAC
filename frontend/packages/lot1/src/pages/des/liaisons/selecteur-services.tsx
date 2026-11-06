import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { SearchInput } from '@gimac/design-system'
import { decrire, grouper, TRANSVERSE } from '../../../dictionnaires/services-perimetre'

// Choix des services du périmètre (D-133), pensé pour qui découvre le système : services rangés par interface puis par
// famille (table services-perimetre.json), nom usuel en clair et code en infobulle seulement, case « tout le groupe »
// à l'état indéterminé, compteur par groupe, recherche sur le nom ou le code. TRANSVERSE reste toujours inclus.

const normaliser = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

export function SelecteurServices({ proposables, valeur, onChange, typeProjet }: { proposables: string[]; valeur: string[]; onChange: (v: string[]) => void; typeProjet: string }) {
  const { t, i18n } = useTranslation()
  const [recherche, setRecherche] = useState('')
  const groupes = useMemo(() => grouper(proposables.filter((c) => c !== TRANSVERSE), typeProjet, i18n.language), [proposables, typeProjet, i18n.language])
  const q = normaliser(recherche.trim())
  const visible = (code: string, libelle: string) => !q || normaliser(libelle).includes(q) || normaliser(code).includes(q)
  const basculer = (codes: string[], coche: boolean) => onChange(coche ? [...new Set([...valeur, ...codes])] : valeur.filter((c) => !codes.includes(c)))
  const total = proposables.filter((c) => c !== TRANSVERSE).length
  const choisis = valeur.filter((c) => c !== TRANSVERSE).length
  return (
    <div className="gm-col" role="group" aria-label={t('perimetreProjet.services')}>
      <div className="gm-row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <SearchInput label={t('perimetreProjet.recherche')} value={recherche} onChange={setRecherche} width="100%" />
        <span aria-live="polite">{t('perimetreProjet.compteur', { n: choisis, total })}</span>
      </div>
      {groupes.map((g) => (
        <section key={g.code ?? 'autres'} className="gm-col" aria-label={g.libelle}>
          <h3 className="gm-field-label" style={{ margin: 0 }}>{g.libelle}</h3>
          {g.familles.map((f) => {
            const services = f.services.filter((s) => visible(s.code, s.libelle))
            if (services.length === 0) return null
            const codes = f.services.map((s) => s.code)
            const nb = codes.filter((c) => valeur.includes(c)).length
            const tout = nb === codes.length
            return (
              <fieldset key={f.code} className="gm-field" style={{ border: 0, padding: 0, margin: 0 }}>
                <legend className="gm-field-label">{t('perimetreProjet.famille', { famille: f.libelle, n: nb, total: codes.length })}</legend>
                <label className="gm-check">
                  <input
                    type="checkbox"
                    checked={tout}
                    ref={(el) => {
                      if (el) el.indeterminate = nb > 0 && !tout
                    }}
                    onChange={() => basculer(codes, !tout)}
                    aria-label={t('perimetreProjet.toutLeGroupe.aria', { famille: f.libelle })}
                  />
                  <span>{t('perimetreProjet.toutLeGroupe')}</span>
                </label>
                <div className="gm-col" style={{ gap: 'var(--gm-space-2)', paddingLeft: 'var(--gm-space-5)' }}>
                  {services.map((s) => (
                    <label key={s.code} className="gm-check" title={t('perimetreProjet.code', { code: s.code })}>
                      <input type="checkbox" checked={valeur.includes(s.code)} onChange={(e) => basculer([s.code], e.target.checked)} />
                      <span>{s.libelle}</span>
                    </label>
                  ))}
                </div>
              </fieldset>
            )
          })}
        </section>
      ))}
    </div>
  )
}

/** Résumé des changements avant validation : services ajoutés et retirés, par leur nom usuel. */
export function ResumeChangements({ avant, apres, typeProjet }: { avant: string[]; apres: string[]; typeProjet: string }) {
  const { t, i18n } = useTranslation()
  const nom = (c: string) => decrire(c, typeProjet, i18n.language).libelle
  const ajoutes = apres.filter((c) => !avant.includes(c)).map(nom)
  const retires = avant.filter((c) => !apres.includes(c)).map(nom)
  if (ajoutes.length === 0 && retires.length === 0) return <p>{t('perimetreProjet.resume.aucun')}</p>
  return (
    <div className="gm-col" aria-label={t('perimetreProjet.resume')}>
      <strong>{t('perimetreProjet.resume')}</strong>
      {ajoutes.length ? <span>{t('perimetreProjet.resume.ajoutes', { services: ajoutes.join(', ') })}</span> : null}
      {retires.length ? <span>{t('perimetreProjet.resume.retires', { services: retires.join(', ') })}</span> : null}
    </div>
  )
}
