import type { Catalogue } from '@gimac/socle'

// Refus de MS-PILOTAGE sans libellé jusqu'au 04/11/2026 (tâches, rapports, jetons), lus par « Mon activité » et, à
// défaut de clé proc.erreur, par les processus d'unité. Préfixe « act.erreur. ». FR, EN, ES.
export const catalogue: Catalogue = {
  fr: {
    'act.erreur.PIL_DOCUMENT_ABSENT': 'Aucun document n’est rattaché à ce rapport.',
    'act.erreur.PIL_TACHE_DECISION': 'La tâche d’une décision se clôt quand le service prononce ou refuse la proposition.',
    'act.erreur.PIL_TACHE_EXISTANTE': 'Une tâche existe déjà pour cette origine.',
    'act.erreur.PIL_TACHE_INVALIDE': 'Tâche inconnue ou qui ne vous est pas confiée.',
    'act.erreur.PIL_UNITE_INCONNUE': 'Cette unité est inconnue de l’organisation.',
    'act.erreur.PIL_JETON_UTILISATEUR_EXIGE': 'Cette action exige une session utilisateur.',
    'act.erreur.PIL_JETON_SERVICE_EXIGE': 'Cette action est réservée aux services de la plateforme.',
  },
  en: {
    'act.erreur.PIL_DOCUMENT_ABSENT': 'No document is attached to this report.',
    'act.erreur.PIL_TACHE_DECISION': 'A decision task closes when the service pronounces or rejects the proposal.',
    'act.erreur.PIL_TACHE_EXISTANTE': 'A task already exists for this origin.',
    'act.erreur.PIL_TACHE_INVALIDE': 'Unknown task, or a task not assigned to you.',
    'act.erreur.PIL_UNITE_INCONNUE': 'This unit is unknown to the organisation.',
    'act.erreur.PIL_JETON_UTILISATEUR_EXIGE': 'This action requires a user session.',
    'act.erreur.PIL_JETON_SERVICE_EXIGE': 'This action is reserved for platform services.',
  },
  es: {
    'act.erreur.PIL_DOCUMENT_ABSENT': 'No hay ningún documento adjunto a este informe.',
    'act.erreur.PIL_TACHE_DECISION': 'La tarea de una decisión se cierra cuando el servicio pronuncia o rechaza la propuesta.',
    'act.erreur.PIL_TACHE_EXISTANTE': 'Ya existe una tarea para este origen.',
    'act.erreur.PIL_TACHE_INVALIDE': 'Tarea desconocida o que no le ha sido asignada.',
    'act.erreur.PIL_UNITE_INCONNUE': 'Esta unidad es desconocida en la organización.',
    'act.erreur.PIL_JETON_UTILISATEUR_EXIGE': 'Esta acción requiere una sesión de usuario.',
    'act.erreur.PIL_JETON_SERVICE_EXIGE': 'Esta acción está reservada a los servicios de la plataforma.',
  },
}
