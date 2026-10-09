import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox, CheckboxGroup, CrudDialog, InlineAlert, Note, Select, TextField } from '@gimac/design-system'
import {
  INITIATEURS_DEFAUT, LIMITES_FORMULAIRE, LIMITES_PROCESSUS, REGLES_INITIATEUR, TYPES_CHAMP_FORMULAIRE, type ChampFormulaire, type Formulaire, type Initiateurs, type RegleInitiateur,
} from '../../../modele/processus'
import { useUnites } from '../../../hooks/pilotage'
import { useLibelleProfilEquipe } from '../activite/dialoguesEquipe'
import { unitesEnArbre } from './ProcessusPage'
import { PROFILS_INTERVENANTS } from './communs'

// Lancement d'un processus dans le modélisateur (D-98 §1, §2) : qui peut lancer (règles cumulables en données : membres de
// l'unité, tout le GIMAC, unités, profils, permissions) et formulaire de lancement (champs typés, obligation, aide, valeurs
// d'une liste, types de pièces). Enregistré avec la définition (PUT) ; contrôlé à la publication (anomalies
// INITIATEURS_INCOMPLETS, CHAMP_CODE_DUPLIQUE, CHAMP_CHOIX_ABSENT) et au lancement par MS-PILOTAGE.

const liste = (texte: string) => [...new Set(texte.split(',').map((x) => x.trim()).filter(Boolean))]
const champVierge = (n: number): ChampFormulaire => ({ code: `CHAMP_${n}`, libelle: '', type: 'TEXTE', choix: [], obligatoire: false, aide: null, typesPieces: [] })

/** Code d'un champ tiré de son libellé (majuscules sans accent), modifiable ensuite. */
export const codeDepuisLibelle = (libelle: string) => libelle.normalize('NFD').replace(/[̀-ͯ]/g, '').toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '').slice(0, 40)

export function ModaleLancement({ initiateurs, formulaire, lecture, enCours, erreur, onFermer, onEnregistrer }: {
  initiateurs?: Initiateurs; formulaire?: Formulaire; lecture: boolean; enCours: boolean; erreur?: { titre: string; correlationId?: string }
  onFermer: () => void; onEnregistrer: (i: Initiateurs, f: Formulaire) => void
}) {
  const { t } = useTranslation()
  const unites = useUnites()
  const libelleProfil = useLibelleProfilEquipe()
  const depart = { initiateurs: initiateurs ?? INITIATEURS_DEFAUT, champs: formulaire?.champs ?? [] }
  const [i, setI] = useState<Initiateurs>(depart.initiateurs)
  const [champs, setChamps] = useState<ChampFormulaire[]>(depart.champs)
  const [permissions, setPermissions] = useState(depart.initiateurs.permissions.join(', '))
  const [tente, setTente] = useState(false)
  const regle = (r: RegleInitiateur, oui: boolean) => setI((o) => ({ ...o, regles: oui ? [...o.regles, r] : o.regles.filter((x) => x !== r) }))
  const modifier = (n: number, c: Partial<ChampFormulaire>) => setChamps((l) => l.map((x, k) => (k === n ? { ...x, ...c } : x)))
  const deplacer = (n: number, d: -1 | 1) => setChamps((l) => {
    const m = [...l]
    ;[m[n], m[n + d]] = [m[n + d], m[n]]
    return m
  })
  const codes = champs.map((c) => c.code)
  const erreurs: Record<string, string | undefined> = {
    regles: !i.regles.length ? t('dem.modele.erreur.regles') : undefined,
    unites: i.regles.includes('UNITES') && !i.unites.length ? t('dem.modele.erreur.unites') : undefined,
    profils: i.regles.includes('PROFILS') && !i.profils.length ? t('dem.modele.erreur.profils') : undefined,
    permissions: i.regles.includes('PERMISSIONS') && !liste(permissions).length ? t('dem.modele.erreur.permissions')
      : liste(permissions).some((x) => !/^[A-Z][A-Z0-9_]{1,79}$/.test(x)) ? t('dem.modele.erreur.permissionFormat') : undefined,
    ...Object.fromEntries(champs.flatMap((c, n) => [
      [`libelle.${n}`, !c.libelle.trim() ? t('proc.erreur.libelle') : undefined],
      [`code.${n}`, !LIMITES_PROCESSUS.codeEtape.test(c.code) ? t('proc.erreur.codeEtape') : codes.indexOf(c.code) !== n ? t('dem.modele.erreur.codeDouble') : undefined],
      [`choix.${n}`, c.type === 'LISTE' && !c.choix.length ? t('dem.modele.erreur.choix') : undefined],
    ])),
  }
  const dirty = JSON.stringify({ i: { ...i, permissions: liste(permissions) }, champs }) !== JSON.stringify({ i: depart.initiateurs, champs: depart.champs })
  return (
    <CrudDialog context={lecture ? 'detail' : 'edit'} size="lg" id="gm-dlg-lancement-modele" title={t('dem.modele.titre')} description={t('dem.modele.texte')} onClose={onFermer}
      submitLabel={lecture ? undefined : t('proc.enregistrer')} submitting={enCours} dirty={!lecture && dirty} cancelLabel={lecture ? t('commun.fermer') : undefined}
      error={erreur ? { title: erreur.titre, correlationId: erreur.correlationId } : undefined}
      onSubmit={lecture ? undefined : () => {
        setTente(true)
        if (Object.values(erreurs).some(Boolean)) return
        onEnregistrer(
          { regles: REGLES_INITIATEUR.filter((r) => i.regles.includes(r)), unites: i.regles.includes('UNITES') ? i.unites : [], profils: i.regles.includes('PROFILS') ? i.profils : [], permissions: i.regles.includes('PERMISSIONS') ? liste(permissions) : [], initiateurIntervenant: !!i.initiateurIntervenant },
          { champs: champs.map((c) => ({ ...c, libelle: c.libelle.trim(), aide: c.aide?.trim() || null, choix: c.type === 'LISTE' ? c.choix : [], typesPieces: c.type === 'PIECE_JOINTE' ? c.typesPieces : [] })) },
        )
      }}>
      <fieldset className="gm-proc-element">
        <legend className="gm-type-body-strong">{t('dem.modele.initiateurs')}</legend>
        <Note>{t('dem.modele.initiateursAide')}</Note>
        {REGLES_INITIATEUR.map((r) => (
          <div key={r} className="gm-col" style={{ gap: 'var(--gm-space-1)' }}>
            <Checkbox name={`initiateurs.${r}`} label={t(`dem.regle.${r}`)} checked={i.regles.includes(r)} disabled={lecture} onChange={(x) => regle(r, x)} />
            <span className="gm-field-help" style={{ marginLeft: 26 }}>{t(`dem.regle.${r}.aide`)}</span>
            {r === 'UNITES' && i.regles.includes('UNITES') ? (
              <CheckboxGroup id="initiateurs-unites" label={t('dem.modele.unites')} disabled={lecture} value={i.unites} onChange={(v) => setI((o) => ({ ...o, unites: v }))}
                options={unitesEnArbre(unites.data ?? []).map((u) => ({ value: u.code, label: u.libelle }))} error={tente ? erreurs.unites : undefined} />
            ) : null}
            {r === 'PROFILS' && i.regles.includes('PROFILS') ? (
              <CheckboxGroup id="initiateurs-profils" label={t('dem.modele.profils')} disabled={lecture} value={i.profils} onChange={(v) => setI((o) => ({ ...o, profils: v }))}
                options={PROFILS_INTERVENANTS.map((p) => ({ value: p, label: libelleProfil(p) }))} error={tente ? erreurs.profils : undefined} />
            ) : null}
            {r === 'PERMISSIONS' && i.regles.includes('PERMISSIONS') ? (
              <TextField name="initiateurs.permissions" label={t('dem.modele.permissions')} help={t('dem.modele.permissionsAide')} mono readOnly={lecture} value={permissions}
                onChange={(x) => setPermissions(x.toUpperCase())} error={tente ? erreurs.permissions : undefined} />
            ) : null}
          </div>
        ))}
        {tente && erreurs.regles ? <InlineAlert level="crit" title={erreurs.regles} /> : null}
        {/* D-99 : séparation des fonctions, réglage en données de la définition (non par défaut). */}
        <Checkbox name="initiateurs.initiateurIntervenant" label={t('proc.lancement.initiateurIntervenant')} checked={!!i.initiateurIntervenant} disabled={lecture}
          onChange={(x) => setI((o) => ({ ...o, initiateurIntervenant: x }))} />
        <span className="gm-field-help">{t('proc.lancement.initiateurIntervenantAide')}</span>
      </fieldset>
      <fieldset className="gm-proc-element">
        <legend className="gm-type-body-strong">{t('dem.modele.formulaire')}</legend>
        <Note>{t('dem.modele.formulaireAide')}</Note>
        {!champs.length ? <p className="gm-field-help" style={{ margin: 0 }}>{t('dem.modele.aucunChamp')}</p> : null}
        {champs.map((c, n) => (
          <fieldset key={n} className="gm-proc-element" aria-label={t('dem.modele.champN', { n: n + 1, libelle: c.libelle || c.code })}>
            <div className="gm-form-grid gm-mod-grille">
              <div className="gm-form-field">
                <TextField name={`formulaire.${n}.libelle`} label={t('proc.champ.libelle')} required readOnly={lecture} maxLength={LIMITES_PROCESSUS.libelle} value={c.libelle}
                  onChange={(x) => modifier(n, { libelle: x, ...(c.code === `CHAMP_${n + 1}` || c.code === codeDepuisLibelle(c.libelle) ? { code: codeDepuisLibelle(x) || `CHAMP_${n + 1}` } : {}) })}
                  error={tente ? erreurs[`libelle.${n}`] : undefined} />
              </div>
              <div className="gm-form-field">
                <TextField name={`formulaire.${n}.code`} label={t('proc.champ.code')} required mono readOnly={lecture} maxLength={40} value={c.code} onChange={(x) => modifier(n, { code: x.toUpperCase() })}
                  error={tente ? erreurs[`code.${n}`] : undefined} />
              </div>
              <div className="gm-form-field">
                <Select name={`formulaire.${n}.type`} label={t('dem.modele.type')} disabled={lecture} value={c.type} onChange={(x) => modifier(n, { type: x as ChampFormulaire['type'] })}
                  options={TYPES_CHAMP_FORMULAIRE.map((x) => ({ value: x, label: t(`dem.type.${x}`) }))} />
              </div>
              <div className="gm-form-field" style={{ alignSelf: 'end' }}>
                <Checkbox name={`formulaire.${n}.obligatoire`} label={t('dem.modele.obligatoire')} disabled={lecture} checked={c.obligatoire} onChange={(x) => modifier(n, { obligatoire: x })} />
              </div>
              <div className="gm-form-field gm-form-wide">
                <TextField name={`formulaire.${n}.aide`} label={t('dem.modele.aide')} readOnly={lecture} maxLength={LIMITES_FORMULAIRE.aide} value={c.aide ?? ''} onChange={(x) => modifier(n, { aide: x })} />
              </div>
              {c.type === 'LISTE' ? (
                <div className="gm-form-field gm-form-wide">
                  <TextField name={`formulaire.${n}.choix`} label={t('dem.modele.choix')} help={t('dem.modele.virgules')} required readOnly={lecture} value={c.choix.join(', ')}
                    onChange={(x) => modifier(n, { choix: liste(x) })} error={tente ? erreurs[`choix.${n}`] : undefined} />
                </div>
              ) : null}
              {c.type === 'PIECE_JOINTE' ? (
                <div className="gm-form-field gm-form-wide">
                  <TextField name={`formulaire.${n}.typesPieces`} label={t('dem.modele.typesPieces')} help={t('dem.modele.typesPiecesAide')} mono readOnly={lecture} value={c.typesPieces.join(', ')}
                    onChange={(x) => modifier(n, { typesPieces: liste(x.toLowerCase()) })} />
                </div>
              ) : null}
            </div>
            {!lecture ? (
              <div className="gm-row" style={{ gap: 'var(--gm-space-1)' }}>
                <Button size="sm" variant="ghost" icon="arrow-up" disabled={n === 0} disabledReason={t('dem.modele.premier')} aria-label={t('dem.modele.monter', { n: n + 1 })} onClick={() => deplacer(n, -1)}>{t('dem.modele.monterCourt')}</Button>
                <Button size="sm" variant="ghost" icon="arrow-down" disabled={n === champs.length - 1} disabledReason={t('dem.modele.dernier')} aria-label={t('dem.modele.descendre', { n: n + 1 })} onClick={() => deplacer(n, 1)}>{t('dem.modele.descendreCourt')}</Button>
                <Button size="sm" variant="ghost" icon="trash" aria-label={t('dem.modele.retirer', { n: n + 1 })} onClick={() => setChamps((l) => l.filter((_, k) => k !== n))}>{t('dem.modele.retirerCourt')}</Button>
              </div>
            ) : null}
          </fieldset>
        ))}
        {!lecture ? (
          <div>
            <Button size="sm" icon="plus" disabled={champs.length >= LIMITES_FORMULAIRE.champs} disabledReason={t('dem.modele.max', { n: LIMITES_FORMULAIRE.champs })}
              onClick={() => setChamps((l) => [...l, champVierge(l.length + 1)])}>{t('dem.modele.ajouterChamp')}</Button>
          </div>
        ) : null}
      </fieldset>
    </CrudDialog>
  )
}
