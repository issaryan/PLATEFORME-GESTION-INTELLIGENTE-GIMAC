import { useState, type CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, ConfirmDialog, Dialog, IconButton, InlineAlert, Select, Tag, TextField } from '@gimac/design-system'
import type { ApiError } from '@gimac/socle'
import { useAjouterEtape, useModifierEtape, useOrdonnerEtapes, useSupprimerEtape } from '../../../hooks/referentiels'
import type { CatalogueModelisateur, CouleurEtape, EtapeReferentiel } from '../../../modele/homologation'
import type { EtapeCode } from '../../../modele/parcours'

// Étapes configurables de la version (D-89 §2, §4) : liste ordonnable (PUT …/etapes/ordre), ajout (POST),
// renommage, couleur et étape canonique (PUT …/etapes/{code}), retrait d'une étape vide et non protégée (DELETE).
// Couleurs et étapes canoniques viennent du catalogue ; le service refait chaque contrôle (codes HOM_ETAPE_*).

const CODE_ETAPE = /^[A-Z][A-Z0-9_]{1,39}$/
type Saisie = { code: string; libelle: string; couleur: CouleurEtape; canonique: string }
type Edition = { creation: true } | { creation: false; etape: EtapeReferentiel }

const couleurDe = (catalogue: CatalogueModelisateur, code: string) => catalogue.couleurs?.find((c) => c.code === code)?.jeton
const pastille = (jeton: string | null | undefined) => (jeton ? ({ '--gm-mod-couloir-couleur': `var(${jeton})` } as CSSProperties) : undefined)

export function ModaleEtapes({ versionId, etapes, catalogue, lecture, onFermer, onAnnonce }: {
  versionId: string; etapes: EtapeReferentiel[]; catalogue: CatalogueModelisateur; lecture: boolean; onFermer: () => void; onAnnonce: (texte: string) => void
}) {
  const { t } = useTranslation()
  const ajouter = useAjouterEtape(versionId)
  const modifier = useModifierEtape(versionId)
  const supprimer = useSupprimerEtape(versionId)
  const ordonner = useOrdonnerEtapes(versionId)
  const [edition, setEdition] = useState<Edition | null>(null)
  const [retrait, setRetrait] = useState<EtapeReferentiel | null>(null)
  const [erreur, setErreur] = useState<ApiError | null>(null)
  const liste = [...etapes].sort((a, b) => a.ordre - b.ordre)
  const echec = (e: ApiError) => setErreur(e)
  const deplacer = (i: number, sens: -1 | 1) => {
    const codes = liste.map((e) => e.code)
    const j = i + sens
    ;[codes[i], codes[j]] = [codes[j], codes[i]]
    setErreur(null)
    ordonner.mutate(codes, { onError: echec, onSuccess: () => onAnnonce(t('modelisateur.etapes.annonce.ordre', { libelle: liste[i].libelle, position: j + 1, total: codes.length })) })
  }
  return (
    <Dialog open id="gm-dialogue-etapes" size="lg" title={t('modelisateur.etapes.titre')} description={t('modelisateur.etapes.aide')} onClose={onFermer}
      footer={<Button variant="ghost" onClick={onFermer}>{t('commun.fermer')}</Button>}>
      <div className="gm-col" style={{ gap: 'var(--gm-space-3)' }}>
        {erreur ? (
          <InlineAlert level="crit" title={t(`erreur.${erreur.code}`, { defaultValue: erreur.message })} correlationId={erreur.correlationId || undefined}
            action={<Button size="sm" variant="ghost" icon="x" onClick={() => setErreur(null)}>{t('commun.fermer')}</Button>} />
        ) : null}
        <ol className="gm-mod-etapes" aria-label={t('modelisateur.etapes.liste')}>
          {liste.map((e, i) => (
            <li key={e.code} className="gm-mod-etape" style={pastille(e.jeton)}>
              <span className="gm-mod-etape-pastille" aria-hidden="true" />
              <span className="gm-mod-etape-corps">
                <strong>{e.libelle}</strong>
                <span className="gm-field-help">
                  <span className="gm-mono">{e.code}</span>
                  {' · '}{t(`modelisateur.etapes.couleur.${e.couleur}`)}
                  {' · '}{t('modelisateur.etapes.operations', { count: e.nombreOperations })}
                </span>
              </span>
              {e.protegee ? <span title={t('modelisateur.etapes.protegeeAide')}><Tag>{t('modelisateur.etapes.protegee')}</Tag></span> : null}
              {!lecture ? (
                <span className="gm-mod-etape-actions">
                  <IconButton variant="ghost" size="sm" icon="arrow-up" label={t('modelisateur.etapes.monter', { libelle: e.libelle })} disabled={i === 0 || ordonner.isPending} onClick={() => deplacer(i, -1)} />
                  <IconButton variant="ghost" size="sm" icon="arrow-down" label={t('modelisateur.etapes.descendre', { libelle: e.libelle })} disabled={i === liste.length - 1 || ordonner.isPending} onClick={() => deplacer(i, 1)} />
                  <IconButton variant="ghost" size="sm" icon="pen-line" label={t('modelisateur.etapes.modifier', { libelle: e.libelle })} onClick={() => { setErreur(null); setEdition({ creation: false, etape: e }) }} />
                  {e.supprimable ? <IconButton variant="ghost" size="sm" icon="x" label={t('modelisateur.etapes.supprimer', { libelle: e.libelle })} onClick={() => setRetrait(e)} /> : null}
                </span>
              ) : null}
            </li>
          ))}
        </ol>
        {!lecture && !edition ? (
          <div><Button icon="plus" onClick={() => { setErreur(null); setEdition({ creation: true }) }}>{t('modelisateur.etapes.ajouter')}</Button></div>
        ) : null}
        {edition ? (
          <FormulaireEtape
            key={edition.creation ? 'nouvelle' : edition.etape.code}
            edition={edition}
            catalogue={catalogue}
            enCours={ajouter.isPending || modifier.isPending}
            onAnnuler={() => setEdition(null)}
            onEnregistrer={(s) => {
              setErreur(null)
              const corps = { libelle: s.libelle.trim(), couleur: s.couleur, canonique: (s.canonique || null) as EtapeCode | null }
              if (edition.creation) {
                ajouter.mutate({ ...corps, code: s.code }, { onError: echec, onSuccess: () => { setEdition(null); onAnnonce(t('modelisateur.etapes.annonce.ajoutee', { libelle: corps.libelle })) } })
              } else {
                modifier.mutate({ code: edition.etape.code, etape: corps }, { onError: echec, onSuccess: () => { setEdition(null); onAnnonce(t('modelisateur.etapes.annonce.modifiee', { libelle: corps.libelle })) } })
              }
            }}
          />
        ) : null}
      </div>
      {retrait ? (
        <ConfirmDialog
          open
          danger
          title={t('modelisateur.etapes.confirmer', { libelle: retrait.libelle })}
          description={t('modelisateur.etapes.confirmerTexte')}
          confirmLabel={t('modelisateur.etapes.supprimer', { libelle: retrait.libelle })}
          object={[{ key: t('modelisateur.etapes.code'), value: retrait.code, mono: true }, { key: t('modelisateur.etapes.libelle'), value: retrait.libelle }]}
          onConfirm={() => {
            const e = retrait
            setRetrait(null)
            setErreur(null)
            supprimer.mutate(e.code, { onError: echec, onSuccess: () => onAnnonce(t('modelisateur.etapes.annonce.supprimee', { libelle: e.libelle })) })
          }}
          onClose={() => setRetrait(null)}
        />
      ) : null}
    </Dialog>
  )
}

/** Ajout ou modification d'une étape : code (à la création), libellé, couleur, étape canonique. */
function FormulaireEtape({ edition, catalogue, enCours, onAnnuler, onEnregistrer }: {
  edition: Edition; catalogue: CatalogueModelisateur; enCours: boolean; onAnnuler: () => void; onEnregistrer: (s: Saisie) => void
}) {
  const { t } = useTranslation()
  const initiale: Saisie = edition.creation
    ? { code: '', libelle: '', couleur: 'NEUTRE', canonique: '' }
    : { code: edition.etape.code, libelle: edition.etape.libelle, couleur: edition.etape.couleur, canonique: edition.etape.canonique ?? '' }
  const [s, setS] = useState<Saisie>(initiale)
  const [tente, setTente] = useState(false)
  const erreurs = {
    code: edition.creation && !CODE_ETAPE.test(s.code) ? t('modelisateur.etapes.erreur.code') : undefined,
    libelle: !s.libelle.trim() || s.libelle.trim().length > 120 ? t('modelisateur.etapes.erreur.libelle') : undefined,
  }
  const protegee = !edition.creation && edition.etape.protegee
  const couleurs = catalogue.couleurs ?? []
  const titre = edition.creation ? t('modelisateur.etapes.nouvelle') : t('modelisateur.etapes.edition', { libelle: edition.etape.libelle })
  return (
    <section className="gm-col gm-pad" aria-label={titre} style={{ gap: 'var(--gm-space-3)', border: 'var(--gm-border-1) solid var(--gm-color-border)' }}>
      <h3 className="gm-type-h3" style={{ margin: 0 }}>{titre}</h3>
      <div className="gm-row" style={{ gap: 'var(--gm-space-3)', alignItems: 'flex-start', flexWrap: 'wrap' }}>
        {edition.creation ? (
          <TextField name="codeEtape" label={t('modelisateur.etapes.code')} help={t('modelisateur.etapes.codeAide')} value={s.code} required mono maxLength={40}
            error={tente ? erreurs.code : undefined} onChange={(v) => setS((x) => ({ ...x, code: v.toUpperCase() }))} />
        ) : null}
        <TextField name="libelleEtape" label={t('modelisateur.etapes.libelle')} value={s.libelle} required maxLength={120} error={tente ? erreurs.libelle : undefined}
          onChange={(v) => setS((x) => ({ ...x, libelle: v }))} />
        <Select name="canonique" label={t('modelisateur.etapes.canonique')} help={protegee ? t('modelisateur.etapes.protegeeAide') : t('modelisateur.etapes.canoniqueAide')}
          value={s.canonique} disabled={protegee} onChange={(v) => setS((x) => ({ ...x, canonique: v }))}
          options={[{ value: '', label: t('modelisateur.etapes.precedente') }, ...(catalogue.etapesCanoniques ?? []).map((c) => ({ value: c.code, label: t(`parcours.etape.${c.code}`, { defaultValue: c.libelle }) }))]} />
      </div>
      <fieldset className="gm-mod-couleurs">
        <legend className="gm-field-label">{t('modelisateur.etapes.couleur')}</legend>
        {couleurs.map((c) => (
          <label key={c.code} className="gm-mod-couleur" style={pastille(couleurDe(catalogue, c.code))}>
            <input type="radio" name="couleurEtape" value={c.code} checked={s.couleur === c.code} onChange={() => setS((x) => ({ ...x, couleur: c.code }))} />
            <span className="gm-mod-etape-pastille" aria-hidden="true" />
            {t(`modelisateur.etapes.couleur.${c.code}`, { defaultValue: c.libelle })}
          </label>
        ))}
      </fieldset>
      <div className="gm-row" style={{ justifyContent: 'flex-end' }}>
        <Button variant="ghost" onClick={onAnnuler}>{t('commun.annuler')}</Button>
        <Button variant="primary" icon="check" loading={enCours} onClick={() => {
          setTente(true)
          if (!erreurs.code && !erreurs.libelle) onEnregistrer(s)
        }}>{t('modelisateur.etapes.enregistrer')}</Button>
      </div>
    </section>
  )
}
