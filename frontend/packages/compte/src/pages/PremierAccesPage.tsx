import { useEffect, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Card, Checkbox, InlineAlert, KeyValueList, Panel, Select, Stepper, TextField } from '@gimac/design-system'
import { usePreferences } from '@gimac/socle'
import { actionsFournisseur, LANGUES_COMPTE, type LangueCompte, type Moi } from '../modele'
import { useEnrolerCle, usePremiereConfiguration, useReconnecter } from '../hooks'
import { useMessageErreur, type MessageErreur } from '../outils'

// Assistant de premier accès (D-62) : un compte créé par le super administrateur ou le RSSI est complété
// par son titulaire avant tout tableau de bord. Étapes : fiche et charte (plateforme), clé de sécurité
// (facultative), puis actions requises du fournisseur d'identité (mot de passe, second facteur,
// courriel), exécutées sur ses pages aux couleurs du GIMAC après une reconnexion.

type Etape = 'FICHE' | 'CLE' | 'IDENTITE'

function etapesDe(moi: Moi): Etape[] {
  const e: Etape[] = []
  if (moi.etapesRestantes.includes('FICHE')) e.push('FICHE')
  if (moi.clesSecurite === 0) e.push('CLE')
  if (actionsFournisseur(moi.etapesRestantes).length > 0) e.push('IDENTITE')
  return e
}

const langueValide = (l: string | null | undefined): LangueCompte | undefined =>
  l && (LANGUES_COMPTE as readonly string[]).includes(l) ? (l as LangueCompte) : undefined

function EtapeFiche({ moi, onSuivant }: { moi: Moi; onSuivant: () => void }) {
  const { t, i18n } = useTranslation()
  const prefs = usePreferences()
  const message = useMessageErreur()
  const envoyer = usePremiereConfiguration()
  const [telephone, setTelephone] = useState(moi.compte.telephone ?? '')
  const [langue, setLangue] = useState<LangueCompte>(langueValide(moi.compte.langue) ?? langueValide(i18n.language.slice(0, 2)) ?? 'fr')
  const [fonction, setFonction] = useState(moi.compte.fonction ?? '')
  const [charte, setCharte] = useState(false)
  const [erreurs, setErreurs] = useState<{ telephone?: string; langue?: string; charte?: string }>({})
  const [erreur, setErreur] = useState<MessageErreur | null>(null)

  const soumettre = async (e: FormEvent) => {
    e.preventDefault()
    const locales = {
      telephone: telephone.trim() ? undefined : t('premierAcces.fiche.telephone.requis'),
      charte: charte ? undefined : t('premierAcces.fiche.charte.requise'),
    }
    setErreurs(locales)
    setErreur(null)
    if (locales.telephone || locales.charte) return
    try {
      await envoyer.mutateAsync({ telephone: telephone.trim(), langue, fonction: fonction.trim() || null, charteAcceptee: charte })
      prefs.changerLangue(langue)
      onSuivant()
    } catch (x) {
      const m = message(x)
      if (m.code === 'IAM_PHONE_INVALID' || m.code === 'IAM_PHONE_REQUIRED') setErreurs({ telephone: m.texte })
      else if (m.code === 'IAM_LANGUAGE_INVALID') setErreurs({ langue: m.texte })
      else if (m.code === 'IAM_CHARTER_REQUIRED') setErreurs({ charte: m.texte })
      else setErreur(m)
    }
  }

  return (
    <form className="gm-col" onSubmit={(e) => void soumettre(e)} aria-labelledby="gm-premier-fiche" noValidate>
      <h2 id="gm-premier-fiche" className="gm-type-h3" style={{ margin: 0 }}>{t('premierAcces.fiche.titre')}</h2>
      <p style={{ margin: 0 }}>{t('premierAcces.fiche.texte')}</p>
      {erreur ? <InlineAlert level="crit" title={erreur.texte} correlationId={erreur.correlationId} /> : null}
      <div className="gm-form-grid">
        <div className="gm-form-field">
          <TextField name="telephone" type="tel" label={t('compte.telephone')} required value={telephone} onChange={setTelephone} error={erreurs.telephone} help={t('compte.telephone.aide')} autoComplete="tel" />
        </div>
        <div className="gm-form-field">
          <Select name="langue" label={t('compte.langue')} required value={langue} onChange={(v) => setLangue(v as LangueCompte)} error={erreurs.langue}
            options={LANGUES_COMPTE.map((l) => ({ value: l, label: t(`compte.langue.${l}`) }))} />
        </div>
        <div className="gm-form-field gm-form-wide">
          <TextField name="fonction" label={t('compte.fonction')} value={fonction} onChange={setFonction} help={t('compte.facultatif')} />
        </div>
      </div>
      <Panel>
        <h3 className="gm-card-title" style={{ marginTop: 0 }}>{t('premierAcces.charte.titre')}</h3>
        <p style={{ margin: 0 }}>{t('premierAcces.charte.texte')}</p>
      </Panel>
      <Checkbox name="charte" label={t('premierAcces.charte.accepter')} checked={charte} onChange={setCharte} error={erreurs.charte} />
      <div>
        <Button variant="primary" type="submit" icon="arrow-right" loading={envoyer.isPending}>{t('premierAcces.fiche.valider')}</Button>
      </div>
    </form>
  )
}

function EtapeCle({ onSuivant }: { onSuivant: () => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const enroler = useEnrolerCle()
  const [nom, setNom] = useState(t('premierAcces.cle.nomDefaut'))
  const [erreur, setErreur] = useState<MessageErreur | null>(null)
  const agir = async () => {
    setErreur(null)
    try {
      await enroler.mutateAsync(nom)
      onSuivant()
    } catch (x) {
      setErreur(message(x))
    }
  }
  return (
    <div className="gm-col">
      <h2 className="gm-type-h3" style={{ margin: 0 }}>{t('premierAcces.cle.titre')}</h2>
      <p style={{ margin: 0 }}>{t('premierAcces.cle.texte')}</p>
      {erreur ? <InlineAlert level="warn" title={erreur.texte} correlationId={erreur.correlationId} /> : null}
      <div style={{ maxWidth: 420 }}>
        <TextField name="nomCle" label={t('compte.cles.nom')} value={nom} onChange={setNom} required maxLength={100} />
      </div>
      <div className="gm-row">
        <Button variant="primary" icon="key-round" loading={enroler.isPending} onClick={() => void agir()}>{t('compte.cles.enroler')}</Button>
        <Button variant="ghost" onClick={onSuivant}>{t('premierAcces.cle.plusTard')}</Button>
      </div>
    </div>
  )
}

function EtapeIdentite({ actions, onSuivant }: { actions: string[]; onSuivant: () => void }) {
  const { t } = useTranslation()
  const message = useMessageErreur()
  const reconnecter = useReconnecter()
  const [erreur, setErreur] = useState<MessageErreur | null>(null)
  const continuer = async () => {
    setErreur(null)
    try {
      await reconnecter.mutateAsync(undefined)
      onSuivant()
    } catch (x) {
      setErreur(message(x))
    }
  }
  return (
    <div className="gm-col">
      <h2 className="gm-type-h3" style={{ margin: 0 }}>{t('premierAcces.identite.titre')}</h2>
      <p style={{ margin: 0 }}>{t('premierAcces.identite.texte')}</p>
      <KeyValueList items={actions.map((a) => ({ key: t(`compte.action.${a}`), value: t(`premierAcces.identite.${a}`) }))} />
      {erreur ? <InlineAlert level="crit" title={erreur.texte} correlationId={erreur.correlationId} /> : null}
      <div>
        <Button variant="primary" icon="lock" loading={reconnecter.isPending} onClick={() => void continuer()}>{t('premierAcces.identite.continuer')}</Button>
      </div>
    </div>
  )
}

export default function PremierAccesPage({ moi, onTermine }: { moi: Moi; onTermine: () => void }) {
  const { t } = useTranslation()
  // Étapes figées à l'ouverture : l'avancement ne dépend pas des relectures du compte.
  const [etapes] = useState(() => etapesDe(moi))
  const [actions] = useState(() => actionsFournisseur(moi.etapesRestantes))
  const [index, setIndex] = useState(0)
  const courante = etapes[index]
  useEffect(() => {
    if (etapes.length === 0) onTermine()
  }, [etapes, onTermine])
  const suivant = () => (index + 1 < etapes.length ? setIndex(index + 1) : onTermine())

  return (
    <div className="gm-col" style={{ gap: 'var(--gm-space-5)', maxWidth: 880 }}>
      <header className="gm-col" style={{ gap: 'var(--gm-space-2)' }}>
        <h1 className="gm-heading-display">{t('premierAcces.titre')}</h1>
        <p style={{ margin: 0 }}>{t('premierAcces.bienvenue', { nom: moi.compte.nomAffiche })}</p>
      </header>
      <nav aria-label={t('premierAcces.etapes')}>
        <Stepper steps={etapes.map((e, i) => ({ label: t(`premierAcces.etape.${e}`), state: i < index ? 'done' : i === index ? 'current' : 'todo' }))} />
      </nav>
      <Card>
        {courante === 'FICHE' ? <EtapeFiche moi={moi} onSuivant={suivant} /> : null}
        {courante === 'CLE' ? <EtapeCle onSuivant={suivant} /> : null}
        {courante === 'IDENTITE' ? <EtapeIdentite actions={actions} onSuivant={suivant} /> : null}
      </Card>
    </div>
  )
}
