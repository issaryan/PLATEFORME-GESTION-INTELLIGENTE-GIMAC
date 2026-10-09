import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { CrudDialog, DateField, NumberField, Note, Select, TextArea, TextField } from '@gimac/design-system'
import { champsRefuses, useSession } from '@gimac/socle'
import { LIMITES_PROCESSUS, champsDeLancement, designationsAttendues, type ChampProcessus, type InstanceProcessus, type Lancement, type Processus } from '../../../modele/processus'
import { useActeInstance, useCandidatsProcessus } from '../../../hooks/processus'
import { depuisSaisie, versSaisie } from '../../../modele/jours'
import { useServices } from '../../../services/ServicesProvider'
import { useLibelleUnite } from '../activite/communs'
import { ChoixPersonne, useErreurProcessus } from './communs'
import { ChampsFormulaire, erreursFormulaire, valeursEnvoyees, type SaisieChamp, type SaisieFormulaire } from './FormulaireLancement'

// Lancement d'un processus publié (D-87 §2, §5.1) en fenêtre modale : intitulé (l'objet d'une demande, D-98), formulaire de
// lancement de la définition (D-98 §2 : champs typés, pièces jointes déposées avant l'envoi, erreurs du service champ par
// champ), valeurs des champs des étapes initiales,
// une personne pour chaque intervenant AU_LANCEMENT (clés CODE_ETAPE, CODE_ETAPE.validateur, CODE_ETAPE.signature.CODE),
// les porteurs du profil de remplacement proposés d'abord (dans l'unité, puis ailleurs dans le GIMAC ; un seul : choisi
// d'office). MS-PILOTAGE vérifie chaque désignation.

/** Champ de saisie d'une valeur selon son type (texte, nombre, date, choix). */
export function ChampValeur({ c, valeur, onChange, erreur, lecture, prefixe = 'champs' }: {
  c: ChampProcessus; valeur: string; onChange: (v: string) => void; erreur?: string; lecture?: boolean; prefixe?: string
}) {
  const { t } = useTranslation()
  const commun = { name: `${prefixe}.${c.code}`, label: c.libelle, required: c.obligatoire, error: erreur }
  if (c.type === 'NOMBRE') return <NumberField {...commun} value={valeur} readOnly={lecture} onChange={onChange} />
  if (c.type === 'DATE') return <DateField {...commun} value={valeur} disabled={lecture} onChange={onChange} />
  if (c.type === 'CHOIX') return <Select {...commun} value={valeur} disabled={lecture} onChange={onChange} options={[{ value: '', label: t('proc.aucun') }, ...c.choix.map((x) => ({ value: x, label: x }))]} />
  return c.obligatoire || valeur.length > 120 ? <TextArea {...commun} rows={2} maxLength={LIMITES_PROCESSUS.texte} value={valeur} readOnly={lecture} onChange={onChange} />
    : <TextField {...commun} maxLength={LIMITES_PROCESSUS.texte} value={valeur} readOnly={lecture} onChange={onChange} />
}

/** Valeur saisie convertie pour le service (NOMBRE : nombre JSON ; DATE : AAAA-MM-JJ ; vide : null). */
export const valeurEnvoyee = (c: ChampProcessus, v: string): string | number | null =>
  (v.trim() === '' ? null : c.type === 'NOMBRE' ? Number(v.replace(',', '.')) : c.type === 'DATE' ? depuisSaisie(v) : v.trim())
/** Valeur rendue par le service mise en forme de saisie (DATE : JJ/MM/AAAA). */
export const valeurSaisie = (c: ChampProcessus, v: string | number | null | undefined): string =>
  (v === null || v === undefined ? '' : c.type === 'DATE' && /^\d{4}-\d{2}-\d{2}$/.test(String(v)) ? versSaisie(String(v)) : String(v))
/** Erreur de forme d'une valeur (obligatoire, nombre décimal). */
export function erreurValeur(c: ChampProcessus, v: string, t: (k: string) => string): string | undefined {
  if (c.obligatoire && !v.trim()) return t('proc.erreur.obligatoire')
  if (c.type === 'NOMBRE' && v.trim() && Number.isNaN(Number(v.replace(',', '.')))) return t('proc.erreur.nombre')
  if (c.type === 'DATE' && v.trim() && !depuisSaisie(v)) return t('proc.erreur.date')
  return undefined
}

/** Désignation au lancement : porteurs du profil de remplacement dans l'unité, puis ailleurs dans le GIMAC (un seul : choisi). */
function Designation({ unite, profil, onChange, exclure, ...p }: { name: string; label: string; unite: string; profil?: string | null; value: string; onChange: (v: string) => void; error?: string; exclure: string[] }) {
  const candidats = useCandidatsProcessus(unite, profil, !!profil)
  return <ChoixPersonne {...p} required profil={profil} candidats={candidats.data} chargement={!!profil && candidats.isPending} onChange={onChange} exclure={exclure} />
}

export function DialogueLancement({ processus, onFermer, onLance, demande = false }: {
  processus: Processus; onFermer: () => void; onLance: (i: InstanceProcessus) => void
  /** Lancement depuis « Lancer une demande » (D-98) : l'intitulé est l'objet de la demande, le service destinataire est rappelé. */
  demande?: boolean
}) {
  const { t } = useTranslation()
  const erreur = useErreurProcessus()
  const services = useServices()
  // D-99 : l'initiateur ne se désigne pas pour une étape de sa propre demande, sauf autorisation de la définition.
  const session = useSession()
  const moiExclu = session && !processus.initiateurs?.initiateurIntervenant ? [session.sujet] : []
  const libelleUnite = useLibelleUnite()
  const lancer = useActeInstance((s, v: Lancement) => s.processus.lancer(processus.id, v))
  const champs = champsDeLancement(processus.etapes)
  const formulaire = processus.formulaire?.champs ?? []
  const designations = designationsAttendues(processus.etapes)
  const [intitule, setIntitule] = useState('')
  const [valeurs, setValeurs] = useState<Record<string, string>>({})
  const [saisie, setSaisie] = useState<SaisieFormulaire>({})
  // Pièces déjà déposées (ticket par code de champ) : un nouvel essai après un refus ne les renvoie pas.
  const [pieces, setPieces] = useState<Record<string, { fichier: File; documentId: string }>>({})
  const [depot, setDepot] = useState<unknown>(null)
  const [envoi, setEnvoi] = useState(false)
  const [choix, setChoix] = useState<Record<string, string>>({})
  const [tente, setTente] = useState(false)
  const erreursForme = erreursFormulaire(formulaire, saisie, t)
  const erreurs: Record<string, string | undefined> = {
    intitule: !intitule.trim() || intitule.length > LIMITES_PROCESSUS.libelle ? t('proc.erreur.intitule') : undefined,
    ...Object.fromEntries(champs.map((c) => [`champ.${c.code}`, erreurValeur(c, valeurs[c.code] ?? '', t)])),
    ...Object.fromEntries(formulaire.map((c) => [`formulaire.${c.code}`, erreursForme[c.code]])),
    ...Object.fromEntries(designations.map((d) => [`designation.${d.cle}`, choix[d.cle] ? undefined : t('proc.erreur.designation')])),
  }
  // Refus du service champ par champ (422 PIL_PROCESSUS_FORMULAIRE_INVALIDE, fieldErrors « formulaire.CODE »).
  const refuses = champsRefuses(lancer.error)
  const erreursChamps = Object.fromEntries(formulaire.map((c) => [c.code, (tente ? erreursForme[c.code] : undefined) ?? refuses[`formulaire.${c.code}`]]))
  const refus = erreur(lancer.error ?? depot)
  const manquantes = (lancer.error as { details?: { cles?: string[] } } | null)?.details?.cles
  const changer = (code: string, v: SaisieChamp) => {
    setSaisie((o) => ({ ...o, [code]: v }))
    lancer.reset()
  }
  const envoyer = async () => {
    setTente(true)
    if (Object.values(erreurs).some(Boolean)) return
    setEnvoi(true)
    setDepot(null)
    try {
      const tickets: Record<string, string> = {}
      for (const c of formulaire.filter((x) => x.type === 'PIECE_JOINTE')) {
        const f = saisie[c.code]
        if (!(f instanceof File)) continue
        const deja = pieces[c.code]
        if (deja && deja.fichier === f) {
          tickets[c.code] = deja.documentId
          continue
        }
        const d = await services.processus.televerserPieceLancement(f, processus.id)
        tickets[c.code] = d.documentId
        setPieces((o) => ({ ...o, [c.code]: { fichier: f, documentId: d.documentId } }))
      }
      lancer.mutate({
        intitule: intitule.trim(),
        champs: Object.fromEntries(champs.map((c) => [c.code, valeurEnvoyee(c, valeurs[c.code] ?? '')])),
        designations: Object.fromEntries(designations.map((d) => [d.cle, choix[d.cle]])),
        ...(formulaire.length ? { formulaire: valeursEnvoyees(formulaire, saisie, tickets) } : {}),
      }, { onSuccess: onLance })
    } catch (e) {
      setDepot(e)
    } finally {
      setEnvoi(false)
    }
  }
  return (
    <CrudDialog context="create" size="lg" id="gm-dlg-lancement" title={t(demande ? 'dem.lancement.titre' : 'proc.lancement.titre')}
      subject={demande ? t('dem.lancement.sujet', { libelle: processus.libelle, unite: libelleUnite(processus.unite) }) : `${processus.libelle} · v${processus.version}`}
      description={demande ? processus.description || t('dem.lancement.texte') : t('proc.lancement.texte')}
      onClose={onFermer} submitLabel={t(demande ? 'dem.lancement.envoyer' : 'proc.lancement.lancer')} submitIcon={demande ? 'send' : 'arrow-right'} submitting={lancer.isPending || envoi}
      dirty={!!intitule || Object.keys(valeurs).length > 0 || Object.keys(saisie).length > 0}
      error={refus ? { title: refus.titre, correlationId: refus.correlationId, text: manquantes?.length ? manquantes.join(', ') : Object.keys(refuses).length ? t('dem.erreur.champs') : undefined } : undefined}
      onSubmit={() => void envoyer()}>
      <TextField name="intitule" label={t(demande ? 'dem.lancement.objet' : 'proc.lancement.intitule')} help={t(demande ? 'dem.lancement.objetAide' : 'proc.lancement.intituleAide')} required maxLength={LIMITES_PROCESSUS.libelle} value={intitule} onChange={setIntitule}
        error={tente ? erreurs.intitule : undefined} />
      {formulaire.length ? (
        <fieldset className="gm-proc-element">
          <legend className="gm-type-body-strong">{t('dem.lancement.formulaire')}</legend>
          <ChampsFormulaire champs={formulaire} valeurs={saisie} onChange={changer} erreurs={erreursChamps} />
        </fieldset>
      ) : null}
      {champs.length ? (
        <fieldset className="gm-proc-element">
          <legend className="gm-type-body-strong">{t('proc.lancement.champs')}</legend>
          <div className="gm-form-grid gm-mod-grille">
            {champs.map((c) => <ChampValeur key={c.code} c={c} valeur={valeurs[c.code] ?? ''} onChange={(v) => setValeurs((o) => ({ ...o, [c.code]: v }))} erreur={tente ? erreurs[`champ.${c.code}`] : undefined} />)}
          </div>
        </fieldset>
      ) : null}
      {designations.length ? (
        <fieldset className="gm-proc-element">
          <legend className="gm-type-body-strong">{t('proc.lancement.designations')}</legend>
          <Note>{t('proc.lancement.designationsAide')}</Note>
          {designations.map((d) => (
            <Designation key={d.cle} name={`designations.${d.cle}`} exclure={moiExclu} unite={d.intervenant.unite || processus.unite} profil={d.intervenant.remplacement}
              label={d.role === 'executant' ? t('proc.lancement.executant', { etape: d.etape.libelle }) : d.role === 'validateur' ? t('proc.lancement.validateur', { etape: d.etape.libelle })
                : t('proc.lancement.signataire', { etape: d.etape.libelle, signature: d.signature!.libelle })}
              value={choix[d.cle] ?? ''} onChange={(v) => setChoix((o) => ({ ...o, [d.cle]: v }))} error={tente ? erreurs[`designation.${d.cle}`] : undefined} />
          ))}
        </fieldset>
      ) : null}
    </CrudDialog>
  )
}
