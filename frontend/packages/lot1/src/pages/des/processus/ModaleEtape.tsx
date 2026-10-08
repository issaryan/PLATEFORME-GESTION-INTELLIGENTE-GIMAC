import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox, CheckboxGroup, ConfirmDialog, Dialog, InlineAlert, KeyValueList, NumberField, Select, Tabs, TextArea, TextField } from '@gimac/design-system'
import type { ApiError } from '@gimac/socle'
import {
  LIMITES_PROCESSUS, MODES_INTERVENANT, MODES_REMPLACEMENT, TYPES_CHAMP, intervenantNormalise, type ChampProcessus, type EtapeProcessus, type Intervenant, type LivrableProcessus,
  type ModeIntervenant, type SignatureProcessus, type TypeChamp,
} from '../../../modele/processus'
import { useUnites } from '../../../hooks/pilotage'
import { useCandidatsProcessus } from '../../../hooks/processus'
import { useLibelleUnite } from '../activite/communs'
import { useLibelleProfilEquipe } from '../activite/dialoguesEquipe'
import { ChoixPersonne, PROFILS_INTERVENANTS, useErreurProcessus } from './communs'

// Modale d'une étape du modélisateur des processus d'unité (D-87 §4) : six onglets (Général ; Intervenant et validateur ;
// Livrables ; Signatures ; Champs ; Chronologie). Contrôles de forme avant l'enregistrement (codes, libellés, profil de
// remplacement pour PERSONNE et AU_LANCEMENT, signatures portant des livrables) ; MS-PILOTAGE refait tout.

export const ONGLETS_ETAPE = ['general', 'intervenants', 'livrables', 'signatures', 'champs', 'chronologie'] as const
export type OngletEtape = (typeof ONGLETS_ETAPE)[number]

/** Types de pièces proposés aux livrables ; « type/* » admet toute la famille. */
export const TYPES_PIECES = ['application/pdf', 'text/plain', 'image/*', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/zip']

/** Onglet d'un champ en erreur (« livrables.0.code » → livrables). */
export const ongletDe = (champ: string): OngletEtape => {
  const racine = champ.split('.')[0]
  if (['intervenant', 'validateur'].includes(racine)) return 'intervenants'
  if (['livrables', 'signatures', 'champs'].includes(racine)) return racine as OngletEtape
  if (['predecesseurs', 'delaiJours'].includes(racine)) return 'chronologie'
  return 'general'
}

/** Erreurs de forme d'une étape (clés : chemins de champ, valeurs : clés de traduction). */
export function erreursEtape(e: EtapeProcessus, autres: string[]): Record<string, string> {
  const r: Record<string, string> = {}
  if (!LIMITES_PROCESSUS.codeEtape.test(e.code)) r.code = 'proc.erreur.codeEtape'
  else if (autres.includes(e.code)) r.code = 'proc.erreur.codeDouble'
  if (!e.libelle.trim() || e.libelle.length > LIMITES_PROCESSUS.libelle) r.libelle = 'proc.erreur.libelle'
  if (e.action.length > LIMITES_PROCESSUS.description) r.action = 'proc.erreur.action'
  if (!(e.delaiJours >= 0 && e.delaiJours <= LIMITES_PROCESSUS.delai)) r.delaiJours = 'proc.erreur.delai'
  const intervenant = (i: Intervenant | null, chemin: string) => {
    if (!i) return
    if (i.mode === 'PROFIL' && !i.profil) r[`${chemin}.profil`] = 'proc.erreur.profil'
    if (i.mode === 'PERSONNE' && !i.personneId) r[`${chemin}.personneId`] = 'proc.erreur.personne'
    if (MODES_REMPLACEMENT.includes(i.mode) && !i.remplacement) r[`${chemin}.remplacement`] = 'proc.erreur.remplacement'
  }
  intervenant(e.intervenant, 'intervenant')
  intervenant(e.validateur, 'validateur')
  const codes = (l: { code: string }[], chemin: string) => l.forEach((x, i) => {
    if (!LIMITES_PROCESSUS.codeEtape.test(x.code)) r[`${chemin}.${i}.code`] = 'proc.erreur.codeElement'
    else if (l.findIndex((y) => y.code === x.code) !== i) r[`${chemin}.${i}.code`] = 'proc.erreur.codeDouble'
  })
  codes(e.livrables, 'livrables')
  codes(e.signatures, 'signatures')
  codes(e.champs, 'champs')
  e.livrables.forEach((l, i) => { if (!l.libelle.trim()) r[`livrables.${i}.libelle`] = 'proc.erreur.libelle' })
  e.signatures.forEach((s, i) => {
    if (!s.libelle.trim()) r[`signatures.${i}.libelle`] = 'proc.erreur.libelle'
    if (!s.porte.length) r[`signatures.${i}.porte`] = 'proc.erreur.porte'
    if (!(s.ordre >= 1 && s.ordre <= LIMITES_PROCESSUS.ordre)) r[`signatures.${i}.ordre`] = 'proc.erreur.ordre'
    intervenant(s.signataire, `signatures.${i}.signataire`)
  })
  e.champs.forEach((c, i) => {
    if (!c.libelle.trim()) r[`champs.${i}.libelle`] = 'proc.erreur.libelle'
    if (c.type === 'CHOIX' && !c.choix.length) r[`champs.${i}.choix`] = 'proc.erreur.choix'
  })
  return r
}

/** Intervenant : mode, puis les seuls attributs du mode (profil, personne, unité, profil de remplacement). */
function ChampsIntervenant({ chemin, valeur, onChange, lecture, err, legende, uniteProcessus }: {
  chemin: string; valeur: Intervenant; onChange: (i: Intervenant) => void; lecture: boolean; err: (c: string) => string | undefined; legende: string; uniteProcessus?: string
}) {
  const { t } = useTranslation()
  const libelleProfil = useLibelleProfilEquipe()
  const libelleUnite = useLibelleUnite()
  const unites = useUnites()
  // Désignation hors de l'unité : porteurs du profil (ou du profil de remplacement) dans l'unité, puis ailleurs.
  const uniteEffective = valeur.unite || uniteProcessus || null
  const profilCherche = valeur.mode === 'PROFIL' ? valeur.profil : valeur.mode === 'PERSONNE' ? valeur.remplacement : null
  const candidats = useCandidatsProcessus(uniteEffective, profilCherche, !lecture && !!profilCherche)
  const couverture = valeur.mode !== 'PROFIL' || !valeur.profil || !candidats.data ? undefined
    : candidats.data.dansUnite.length ? t('proc.couverture.dansUnite', { n: candidats.data.dansUnite.length })
      : candidats.data.ailleurs.length ? t('proc.couverture.ailleurs', { n: candidats.data.ailleurs.length, unites: [...new Set(candidats.data.ailleurs.map((c) => (c.unite ? libelleUnite(c.unite) : t('proc.suivi.toutLeGimac'))))].join(', ') })
        : t('proc.couverture.aucun')
  const profils = [{ value: '', label: t('proc.aucun') }, ...PROFILS_INTERVENANTS.map((p) => ({ value: p, label: libelleProfil(p) }))]
  const maj = (champs: Partial<Intervenant>) => onChange(intervenantNormalise({ ...valeur, ...champs }))
  const uniteChamp = (
    <Select name={`${chemin}.unite`} label={t('proc.intervenant.unite')} help={t('proc.intervenant.uniteAide')} value={valeur.unite ?? ''} disabled={lecture}
      options={[{ value: '', label: t('proc.intervenant.uniteProprietaire') }, ...(unites.data ?? []).filter((u) => u.active).map((u) => ({ value: u.code, label: u.libelle }))]} onChange={(v) => maj({ unite: v || null })} />
  )
  return (
    <fieldset className="gm-proc-element" aria-label={legende}>
      <legend className="gm-type-body-strong">{legende}</legend>
      <div className="gm-form-grid gm-mod-grille">
        <Select name={`${chemin}.mode`} label={t('proc.intervenant.mode')} value={valeur.mode} disabled={lecture} onChange={(v) => maj({ mode: v as ModeIntervenant })}
          options={MODES_INTERVENANT.map((m) => ({ value: m, label: t(`proc.mode.${m}`) }))} />
        {valeur.mode === 'PROFIL' ? <Select name={`${chemin}.profil`} label={t('proc.intervenant.profil')} required value={valeur.profil ?? ''} disabled={lecture} error={err(`${chemin}.profil`)} options={profils} help={couverture} onChange={(v) => maj({ profil: v || null })} /> : null}
        {valeur.mode === 'PERSONNE' ? (
          lecture ? <TextField name={`${chemin}.personneId`} label={t('proc.intervenant.personne')} value={valeur.personneId ?? ''} readOnly />
            : <ChoixPersonne name={`${chemin}.personneId`} label={t('proc.intervenant.personne')} required value={valeur.personneId ?? ''} error={err(`${chemin}.personneId`)} profil={valeur.remplacement}
              candidats={candidats.data} chargement={!!profilCherche && candidats.isPending} onChange={(v) => maj({ personneId: v || null })} />
        ) : null}
        {valeur.mode === 'PROFIL' || valeur.mode === 'AU_LANCEMENT' || valeur.mode === 'CHEF_UNITE' ? uniteChamp : null}
        {MODES_REMPLACEMENT.includes(valeur.mode) ? (
          <Select name={`${chemin}.remplacement`} label={t('proc.intervenant.remplacement')} help={t('proc.intervenant.remplacementAide')} required value={valeur.remplacement ?? ''} disabled={lecture}
            error={err(`${chemin}.remplacement`)} options={profils} onChange={(v) => maj({ remplacement: v || null })} />
        ) : null}
      </div>
      <p className="gm-field-help" style={{ margin: 0 }}>{t(`proc.mode.aide.${valeur.mode}`)}</p>
    </fieldset>
  )
}

export type ModaleEtapeProps = {
  etape: EtapeProcessus
  creation: boolean
  lecture: boolean
  /** Toutes les étapes du processus (prédécesseurs possibles, livrables des étapes antérieures). */
  etapes: EtapeProcessus[]
  /** Unité propriétaire du processus : unité des intervenants sans unité propre (candidats). */
  uniteProcessus?: string
  enCours?: boolean
  erreurServeur?: ApiError | null
  ongletInitial?: OngletEtape
  onEnregistrer: (e: EtapeProcessus) => void
  onSupprimer?: () => void
  onFermer: () => void
}

export function ModaleEtape(p: ModaleEtapeProps) {
  const { t } = useTranslation()
  const erreurProcessus = useErreurProcessus()
  const [e, setE] = useState<EtapeProcessus>(p.etape)
  const [onglet, setOnglet] = useState<OngletEtape>(p.ongletInitial ?? 'general')
  const [erreurs, setErreurs] = useState<Record<string, string>>({})
  const [suppression, setSuppression] = useState(false)
  const lecture = p.lecture
  const maj = (champs: Partial<EtapeProcessus>) => setE((x) => ({ ...x, ...champs }))
  const err = (c: string) => (erreurs[c] ? t(erreurs[c], { max: LIMITES_PROCESSUS.libelle }) : undefined)
  const autres = p.etapes.filter((x) => x.code !== p.etape.code || p.creation)
  const majListe = <K extends 'livrables' | 'signatures' | 'champs'>(cle: K, i: number, champs: Partial<EtapeProcessus[K][number]>) =>
    maj({ [cle]: (e[cle] as EtapeProcessus[K][number][]).map((x, j) => (j === i ? { ...x, ...champs } : x)) } as Partial<EtapeProcessus>)
  const retirer = (cle: 'livrables' | 'signatures' | 'champs', i: number) => maj({ [cle]: (e[cle] as unknown[]).filter((_, j) => j !== i) } as Partial<EtapeProcessus>)
  // Livrables portables par une signature : ceux de l'étape et des étapes antérieures (ancêtres).
  const ancetres = new Set<string>()
  const remonter = (c: string[]) => c.forEach((x) => { if (!ancetres.has(x)) { ancetres.add(x); remonter(p.etapes.find((y) => y.code === x)?.predecesseurs ?? []) } })
  remonter(e.predecesseurs)
  const portables = [...e.livrables, ...p.etapes.filter((x) => ancetres.has(x.code)).flatMap((x) => x.livrables)].filter((l) => l.code)
  const successeurs = p.etapes.filter((x) => x.predecesseurs.includes(p.etape.code) && !p.creation).map((x) => x.code)
  const enregistrer = () => {
    const r = erreursEtape(e, autres.map((x) => x.code))
    setErreurs(r)
    const premier = Object.keys(r)[0]
    if (premier) return setOnglet(ongletDe(premier))
    p.onEnregistrer({ ...e, code: e.code.trim(), libelle: e.libelle.trim(), intervenant: intervenantNormalise(e.intervenant), validateur: e.validateur ? intervenantNormalise(e.validateur) : null })
  }
  const nb = (k: OngletEtape) => Object.keys(erreurs).filter((x) => ongletDe(x) === k).length
  const serveur = erreurProcessus(p.erreurServeur)
  const typesPieces = TYPES_PIECES.map((m) => ({ value: m, label: t(`proc.piece.${m.replace(/[^a-z]/g, '_')}`, { defaultValue: m }) }))

  return (
    <Dialog open id="gm-modale-etape" size="lg" title={p.creation ? t('proc.etape.creer') : t('proc.etape.titre', { code: p.etape.code })} subtitle={p.creation ? undefined : p.etape.libelle} onClose={p.onFermer}
      footer={(
        <>
          {!lecture && !p.creation && p.onSupprimer ? <Button variant="danger" icon="trash" onClick={() => setSuppression(true)}>{t('proc.etape.supprimer')}</Button> : null}
          <Button variant="ghost" onClick={p.onFermer}>{lecture ? t('commun.fermer') : t('commun.annuler')}</Button>
          {!lecture ? <Button variant="primary" icon="check" loading={p.enCours} onClick={enregistrer}>{p.creation ? t('proc.etape.ajouter') : t('proc.enregistrer')}</Button> : null}
        </>
      )}>
      <div className="gm-col gm-mod-modale">
        {lecture ? <InlineAlert level="info">{t('proc.lectureSeule')}</InlineAlert> : null}
        {serveur ? <InlineAlert level="crit" title={serveur.titre} correlationId={serveur.correlationId} /> : null}
        <Tabs items={ONGLETS_ETAPE.map((k) => ({ key: k, label: nb(k) ? `${t(`proc.onglet.${k}`)} (${nb(k)})` : t(`proc.onglet.${k}`) }))} value={onglet} onChange={(k) => setOnglet(k as OngletEtape)} />
        <div role="tabpanel" aria-label={t(`proc.onglet.${onglet}`)} className="gm-col">
          {onglet === 'general' ? (
            <div className="gm-form-grid gm-mod-grille">
              <TextField name="code" label={t('proc.champ.code')} help={t('proc.aide.codeEtape')} value={e.code} required mono maxLength={40} readOnly={lecture || !p.creation} error={err('code')}
                onChange={(v) => maj({ code: v.toUpperCase() })} />
              <TextField name="libelle" label={t('proc.champ.libelle')} value={e.libelle} required maxLength={LIMITES_PROCESSUS.libelle} readOnly={lecture} error={err('libelle')} onChange={(v) => maj({ libelle: v })} />
              <TextArea name="action" label={t('proc.champ.action')} help={t('proc.aide.action')} value={e.action} rows={4} maxLength={LIMITES_PROCESSUS.description} readOnly={lecture} error={err('action')}
                onChange={(v) => maj({ action: v })} className="gm-mod-large" />
            </div>
          ) : null}
          {onglet === 'intervenants' ? (
            <div className="gm-col">
              <ChampsIntervenant chemin="intervenant" legende={t('proc.intervenant.executant')} valeur={e.intervenant} lecture={lecture} err={err} uniteProcessus={p.uniteProcessus} onChange={(i) => maj({ intervenant: i })} />
              <Checkbox name="avecValidateur" label={t('proc.intervenant.avecValidateur')} checked={!!e.validateur} disabled={lecture}
                onChange={(v) => maj({ validateur: v ? { mode: 'CHEF_UNITE' } : null })} />
              {e.validateur ? <ChampsIntervenant chemin="validateur" legende={t('proc.intervenant.validateur')} valeur={e.validateur} lecture={lecture} err={err} uniteProcessus={p.uniteProcessus} onChange={(i) => maj({ validateur: i })} /> : null}
            </div>
          ) : null}
          {onglet === 'livrables' ? (
            <div className="gm-col">
              {e.livrables.length ? null : <p className="gm-field-help" style={{ margin: 0 }}>{t('proc.livrable.aucun')}</p>}
              {e.livrables.map((l: LivrableProcessus, i) => (
                <fieldset key={i} className="gm-proc-element" aria-label={t('proc.livrable.n', { n: i + 1 })}>
                  <legend className="gm-type-body-strong">{t('proc.livrable.n', { n: i + 1 })}</legend>
                  <div className="gm-form-grid gm-mod-grille">
                    <TextField name={`livrables.${i}.code`} label={t('proc.champ.code')} value={l.code} mono required readOnly={lecture} error={err(`livrables.${i}.code`)} onChange={(v) => majListe('livrables', i, { code: v.toUpperCase() })} />
                    <TextField name={`livrables.${i}.libelle`} label={t('proc.champ.libelle')} value={l.libelle} required readOnly={lecture} error={err(`livrables.${i}.libelle`)} onChange={(v) => majListe('livrables', i, { libelle: v })} />
                    <TextField name={`livrables.${i}.modele`} label={t('proc.livrable.modele')} help={t('proc.livrable.modeleAide')} value={l.modele ?? ''} mono readOnly={lecture} onChange={(v) => majListe('livrables', i, { modele: v.trim() || null })} />
                    <Checkbox name={`livrables.${i}.obligatoire`} label={t('proc.livrable.obligatoire')} checked={l.obligatoire} disabled={lecture} onChange={(v) => majListe('livrables', i, { obligatoire: v })} />
                  </div>
                  <CheckboxGroup id={`gm-livrable-${i}-types`} label={t('proc.livrable.types')} value={l.typesPieces} disabled={lecture} options={typesPieces} onChange={(v) => majListe('livrables', i, { typesPieces: v })} />
                  {!lecture ? <div className="gm-row" style={{ justifyContent: 'flex-end' }}><Button size="sm" variant="ghost" icon="trash" onClick={() => retirer('livrables', i)}>{t('proc.livrable.retirer', { n: i + 1 })}</Button></div> : null}
                </fieldset>
              ))}
              {!lecture ? <div><Button size="sm" icon="plus" disabled={e.livrables.length >= LIMITES_PROCESSUS.elements}
                onClick={() => maj({ livrables: [...e.livrables, { code: `LIVRABLE_${e.livrables.length + 1}`, libelle: '', typesPieces: ['application/pdf'], obligatoire: true, modele: null }] })}>{t('proc.livrable.ajouter')}</Button></div> : null}
            </div>
          ) : null}
          {onglet === 'signatures' ? (
            <div className="gm-col">
              {e.signatures.length ? null : <p className="gm-field-help" style={{ margin: 0 }}>{t('proc.signature.aucune')}</p>}
              {e.signatures.map((s: SignatureProcessus, i) => (
                <fieldset key={i} className="gm-proc-element" aria-label={t('proc.signature.n', { n: i + 1 })}>
                  <legend className="gm-type-body-strong">{t('proc.signature.n', { n: i + 1 })}</legend>
                  <div className="gm-form-grid gm-mod-grille">
                    <TextField name={`signatures.${i}.code`} label={t('proc.champ.code')} value={s.code} mono required readOnly={lecture} error={err(`signatures.${i}.code`)} onChange={(v) => majListe('signatures', i, { code: v.toUpperCase() })} />
                    <TextField name={`signatures.${i}.libelle`} label={t('proc.champ.libelle')} value={s.libelle} required readOnly={lecture} error={err(`signatures.${i}.libelle`)} onChange={(v) => majListe('signatures', i, { libelle: v })} />
                    <NumberField name={`signatures.${i}.ordre`} label={t('proc.signature.ordre')} help={t('proc.signature.ordreAide')} value={String(s.ordre)} min={1} max={99} readOnly={lecture} error={err(`signatures.${i}.ordre`)}
                      onChange={(v) => majListe('signatures', i, { ordre: Number(v) || 0 })} />
                  </div>
                  <ChampsIntervenant chemin={`signatures.${i}.signataire`} legende={t('proc.signature.signataire')} valeur={s.signataire} lecture={lecture} err={err} uniteProcessus={p.uniteProcessus} onChange={(x) => majListe('signatures', i, { signataire: x })} />
                  <CheckboxGroup id={`gm-signature-${i}-porte`} label={t('proc.signature.porte')} value={s.porte} disabled={lecture} error={err(`signatures.${i}.porte`)}
                    options={portables.map((l) => ({ value: l.code, label: `${l.code} · ${l.libelle}` }))} onChange={(v) => majListe('signatures', i, { porte: v })} />
                  {!lecture ? <div className="gm-row" style={{ justifyContent: 'flex-end' }}><Button size="sm" variant="ghost" icon="trash" onClick={() => retirer('signatures', i)}>{t('proc.signature.retirer', { n: i + 1 })}</Button></div> : null}
                </fieldset>
              ))}
              {!lecture ? <div><Button size="sm" icon="plus" disabled={e.signatures.length >= LIMITES_PROCESSUS.elements}
                onClick={() => maj({ signatures: [...e.signatures, { code: `SIGNATURE_${e.signatures.length + 1}`, libelle: '', signataire: { mode: 'CHEF_UNITE' }, porte: [], ordre: e.signatures.length + 1 }] })}>{t('proc.signature.ajouter')}</Button></div> : null}
            </div>
          ) : null}
          {onglet === 'champs' ? (
            <div className="gm-col">
              {e.champs.length ? null : <p className="gm-field-help" style={{ margin: 0 }}>{t('proc.champs.aucun')}</p>}
              {e.champs.map((c: ChampProcessus, i) => (
                <fieldset key={i} className="gm-proc-element" aria-label={t('proc.champs.n', { n: i + 1 })}>
                  <legend className="gm-type-body-strong">{t('proc.champs.n', { n: i + 1 })}</legend>
                  <div className="gm-form-grid gm-mod-grille">
                    <TextField name={`champs.${i}.code`} label={t('proc.champ.code')} value={c.code} mono required readOnly={lecture} error={err(`champs.${i}.code`)} onChange={(v) => majListe('champs', i, { code: v.toUpperCase() })} />
                    <TextField name={`champs.${i}.libelle`} label={t('proc.champ.libelle')} value={c.libelle} required readOnly={lecture} error={err(`champs.${i}.libelle`)} onChange={(v) => majListe('champs', i, { libelle: v })} />
                    <Select name={`champs.${i}.type`} label={t('proc.champs.type')} value={c.type} disabled={lecture} options={TYPES_CHAMP.map((x) => ({ value: x, label: t(`proc.champs.type.${x}`) }))}
                      onChange={(v) => majListe('champs', i, { type: v as TypeChamp, choix: v === 'CHOIX' ? c.choix : [] })} />
                    {c.type === 'CHOIX' ? (
                      <TextField name={`champs.${i}.choix`} label={t('proc.champs.choix')} help={t('proc.champs.choixAide')} value={c.choix.join(', ')} readOnly={lecture} error={err(`champs.${i}.choix`)}
                        onChange={(v) => majListe('champs', i, { choix: v.split(',').map((x) => x.trim()).filter(Boolean).slice(0, LIMITES_PROCESSUS.choix) })} />
                    ) : null}
                    <Checkbox name={`champs.${i}.obligatoire`} label={t('proc.champs.obligatoire')} checked={c.obligatoire} disabled={lecture} onChange={(v) => majListe('champs', i, { obligatoire: v })} />
                  </div>
                  {!lecture ? <div className="gm-row" style={{ justifyContent: 'flex-end' }}><Button size="sm" variant="ghost" icon="trash" onClick={() => retirer('champs', i)}>{t('proc.champs.retirer', { n: i + 1 })}</Button></div> : null}
                </fieldset>
              ))}
              {e.predecesseurs.length === 0 && e.champs.length ? <p className="gm-field-help" style={{ margin: 0 }}>{t('proc.champs.lancement')}</p> : null}
              {!lecture ? <div><Button size="sm" icon="plus" disabled={e.champs.length >= LIMITES_PROCESSUS.elements}
                onClick={() => maj({ champs: [...e.champs, { code: `CHAMP_${e.champs.length + 1}`, libelle: '', type: 'TEXTE', choix: [], obligatoire: false }] })}>{t('proc.champs.ajouter')}</Button></div> : null}
            </div>
          ) : null}
          {onglet === 'chronologie' ? (
            <div className="gm-col">
              <CheckboxGroup id="gm-etape-predecesseurs" label={t('proc.champ.predecesseurs')} value={e.predecesseurs} disabled={lecture}
                options={autres.filter((x) => x.code !== e.code).map((x) => ({ value: x.code, label: `${x.code} · ${x.libelle}` }))} onChange={(v) => maj({ predecesseurs: v })} />
              <NumberField name="delaiJours" label={t('proc.champ.delai')} help={t('proc.aide.delai')} unit={t('proc.jours')} value={String(e.delaiJours)} min={0} max={LIMITES_PROCESSUS.delai} readOnly={lecture} error={err('delaiJours')}
                onChange={(v) => maj({ delaiJours: v === '' ? -1 : Number(v) })} />
              <KeyValueList items={[{ key: t('proc.champ.successeurs'), value: successeurs.length ? successeurs.join(', ') : t('proc.aucun'), mono: true }]} />
            </div>
          ) : null}
        </div>
      </div>
      {suppression ? (
        <ConfirmDialog open danger title={t('proc.etape.supprimer')} confirmLabel={t('proc.etape.supprimer')} object={[{ key: t('proc.champ.code'), value: p.etape.code, mono: true }, { key: t('proc.champ.libelle'), value: p.etape.libelle }]}
          consequences={[t('proc.etape.suppressionConsequence')]} onConfirm={() => { setSuppression(false); p.onSupprimer?.() }} onClose={() => setSuppression(false)} />
      ) : null}
    </Dialog>
  )
}
