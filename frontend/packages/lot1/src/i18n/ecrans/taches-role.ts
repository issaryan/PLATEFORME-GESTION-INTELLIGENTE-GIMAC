import type { Catalogue } from '@gimac/socle'

// Tâches adressées à un rôle (D-138) : vues et faisables par tous les porteurs du rôle, sans prise préalable.
// Préfixe « act.tache. ». FR, EN, ES.
export const catalogue: Catalogue = {
  fr: {
    'act.tache.pourRole': 'Pour le rôle {{role}}',
    'act.tache.pourRoleAide': 'Tout porteur du rôle la voit et peut l’effectuer, sans la prendre d’abord ; le premier qui l’achève la clôt pour tous.',
    'act.tache.jeMenOccupe': 'Je m’en occupe',
    'act.tache.destinataire': 'Destinataire',
  },
  en: {
    'act.tache.pourRole': 'For the role {{role}}',
    'act.tache.pourRoleAide': 'Every holder of the role sees it and can carry it out without taking it first; the first to complete it closes it for everyone.',
    'act.tache.jeMenOccupe': 'I’ll handle it',
    'act.tache.destinataire': 'Addressee',
  },
  es: {
    'act.tache.pourRole': 'Para el rol {{role}}',
    'act.tache.pourRoleAide': 'Todo titular del rol la ve y puede realizarla sin tomarla antes; el primero que la termina la cierra para todos.',
    'act.tache.jeMenOccupe': 'Me encargo',
    'act.tache.destinataire': 'Destinatario',
  },
}
