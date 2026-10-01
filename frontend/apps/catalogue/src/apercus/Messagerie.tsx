import { useState } from 'react'
import { ConversationList, MessageComposer, MessageThread } from '@gimac/design-system'
import { photoDemoCatalogue } from '../outils/photoDemo'

export default function Apercu() {
  const [texte, setTexte] = useState('')
  const [fichiers, setFichiers] = useState<File[]>([])
  return (
    <div className="gm-row" style={{ alignItems: 'stretch', gap: 0, height: 440 }}>
      <div style={{ width: 300, borderRight: 'var(--gm-border-1) solid var(--gm-color-border)', overflow: 'auto' }}>
        <ConversationList
          selected="c2"
          items={[
            { id: 'c1', titre: 'Aïcha Mahamat Nour', avatar: photoDemoCatalogue('U-201'), extrait: 'Le procès-verbal de LIA-2026-027 est établi à 100 %.', quand: '15:12', nonLus: 1 },
            { id: 'c2', titre: 'Essais de LIA-2026-030', sousTitre: 'Rachel, Aïcha, Hervé', extrait: 'Le blocage est déclaré sur la liaison.', quand: '15:40', nonLus: 2 },
            { id: 'c3', titre: 'Hervé Kamga Tchoupo', extrait: 'Attestation reçue et en cours de vérification.', quand: 'hier' },
          ]}
        />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <MessageThread
          titre="Essais de LIA-2026-030"
          sousTitre="4 membres · liaison LIA-2026-030"
          messages={[
            { id: 'm1', auteur: 'Rachel Biyong Mengue', avatar: photoDemoCatalogue('u-sna'), quand: '09:10', contenu: 'Les essais sont suspendus : l’élément de sécurité du participant doit être renouvelé.', etiquettes: <a className="gm-link" href="#">LIA-2026-030</a>, pieces: [{ id: 'p1', nom: 'rapport-essais.pdf', taille: '84 Ko' }] },
            { id: 'm2', auteur: 'Hervé Kamga Tchoupo', avatar: photoDemoCatalogue('u-rssi'), quand: '13:55', contenu: 'Je prends contact avec le correspondant pour la nouvelle cérémonie.' },
            { id: 'm3', auteur: 'Paul Mbarga Atangana', quand: '15:40', contenu: 'Merci, tenez-moi informé.', mine: true },
          ]}
          saisie={<MessageComposer value={texte} onChange={setTexte} fichiers={fichiers} onFichiers={setFichiers} onSend={() => setTexte('')} />}
        />
      </div>
    </div>
  )
}
