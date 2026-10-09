import type { Catalogue } from '@gimac/socle'

// Corrections de la recette du lot DDR (docs/recette/ddrm.md) : libellés FR, EN, ES (mêmes clés).
export const catalogue: Catalogue = {
  fr: {
    // DDR-17 : proposant d'une transition de statut.
    'transition.demandeur': 'Proposée par', 'transition.demandeur.vous': 'Vous', 'transition.demandeur.autre': 'Un autre agent', 'transition.demandeur.systeme': 'Le système',
    // DDR-03 : contrôles des conditions d'adhésion, sous les champs.
    'adhesion.erreur.codeParticipant': 'Majuscules, chiffres et tirets seulement, 20 caractères au plus.',
    'adhesion.erreur.compteReglement': 'Le compte de règlement est exigé en adhésion directe (RG-11).',
    'adhesion.erreur.banqueMarraine': 'La banque marraine est exigée en adhésion indirecte (RG-12).',
    'adhesion.erreur.compteCantonnement': 'Le compte de cantonnement est exigé en adhésion indirecte (RG-12).',
    'adhesion.erreur.service.codeParticipant': 'Code participant refusé par le registre : majuscules, chiffres et tirets, 20 caractères au plus.',
    'adhesion.erreur.service.mode': 'Choisissez le mode d\'adhésion.',
    'adhesion.erreur.service.compteReglement': 'Le compte de règlement est exigé en adhésion directe (RG-11).',
    'adhesion.erreur.service.banqueMarraineId': 'La banque marraine est exigée en adhésion indirecte (RG-12).',
    'adhesion.erreur.service.compteCantonnement': 'Le compte de cantonnement est exigé en adhésion indirecte (RG-12).',
    // DDR-13 : chronologie et fichier d'une pièce du dossier d'adhésion.
    'piece.chrono.deposee': 'Pièce déposée : {{fichier}}', 'piece.chrono.validee': 'Pièce validée', 'piece.chrono.rejetee': 'Pièce rejetée : {{motif}}',
    'piece.chrono.aucunFichier': 'Aucun fichier déposé', 'piece.chrono.aucunFichier.texte': 'Le fichier et son empreinte s\'affichent ici dès le dépôt.',
    'piece.chrono.aucun': 'Aucun événement', 'piece.chrono.aucun.texte': 'La chronologie de la pièce commence à son premier dépôt.',
    // DDR-16 : tâche offerte sans unité.
    'pvr.tache.aPrendreSansUnite': 'À prendre', 'pvr.tache.toutesUnites': 'Toutes unités',
    // DDR-06 : affectation des ressources, une personne par fonction.
    'parcoursAffectation.erreur.manquantes_one': 'Désignez une personne pour la fonction signalée.',
    'parcoursAffectation.erreur.manquantes_other': 'Désignez une personne pour chacune des {{count}} fonctions signalées.',
  },
  en: {
    'transition.demandeur': 'Proposed by', 'transition.demandeur.vous': 'You', 'transition.demandeur.autre': 'Another officer', 'transition.demandeur.systeme': 'The system',
    'adhesion.erreur.codeParticipant': 'Capitals, digits and hyphens only, 20 characters at most.',
    'adhesion.erreur.compteReglement': 'The settlement account is required for direct membership (RG-11).',
    'adhesion.erreur.banqueMarraine': 'The sponsor bank is required for indirect membership (RG-12).',
    'adhesion.erreur.compteCantonnement': 'The ring-fenced account is required for indirect membership (RG-12).',
    'adhesion.erreur.service.codeParticipant': 'Participant code refused by the register: capitals, digits and hyphens, 20 characters at most.',
    'adhesion.erreur.service.mode': 'Choose the membership mode.',
    'adhesion.erreur.service.compteReglement': 'The settlement account is required for direct membership (RG-11).',
    'adhesion.erreur.service.banqueMarraineId': 'The sponsor bank is required for indirect membership (RG-12).',
    'adhesion.erreur.service.compteCantonnement': 'The ring-fenced account is required for indirect membership (RG-12).',
    'piece.chrono.deposee': 'Document filed: {{fichier}}', 'piece.chrono.validee': 'Document validated', 'piece.chrono.rejetee': 'Document rejected: {{motif}}',
    'piece.chrono.aucunFichier': 'No file filed', 'piece.chrono.aucunFichier.texte': 'The file and its fingerprint appear here once filed.',
    'piece.chrono.aucun': 'No event', 'piece.chrono.aucun.texte': 'The document timeline starts with its first filing.',
    'pvr.tache.aPrendreSansUnite': 'To take', 'pvr.tache.toutesUnites': 'All units',
    'parcoursAffectation.erreur.manquantes_one': 'Assign a person to the highlighted function.',
    'parcoursAffectation.erreur.manquantes_other': 'Assign a person to each of the {{count}} highlighted functions.',
  },
  es: {
    'transition.demandeur': 'Propuesta por', 'transition.demandeur.vous': 'Usted', 'transition.demandeur.autre': 'Otro agente', 'transition.demandeur.systeme': 'El sistema',
    'adhesion.erreur.codeParticipant': 'Solo mayúsculas, cifras y guiones, 20 caracteres como máximo.',
    'adhesion.erreur.compteReglement': 'La cuenta de liquidación es obligatoria en adhesión directa (RG-11).',
    'adhesion.erreur.banqueMarraine': 'El banco padrino es obligatorio en adhesión indirecta (RG-12).',
    'adhesion.erreur.compteCantonnement': 'La cuenta segregada es obligatoria en adhesión indirecta (RG-12).',
    'adhesion.erreur.service.codeParticipant': 'Código de participante rechazado por el registro: mayúsculas, cifras y guiones, 20 caracteres como máximo.',
    'adhesion.erreur.service.mode': 'Elija el modo de adhesión.',
    'adhesion.erreur.service.compteReglement': 'La cuenta de liquidación es obligatoria en adhesión directa (RG-11).',
    'adhesion.erreur.service.banqueMarraineId': 'El banco padrino es obligatorio en adhesión indirecta (RG-12).',
    'adhesion.erreur.service.compteCantonnement': 'La cuenta segregada es obligatoria en adhesión indirecta (RG-12).',
    'piece.chrono.deposee': 'Documento depositado: {{fichier}}', 'piece.chrono.validee': 'Documento validado', 'piece.chrono.rejetee': 'Documento rechazado: {{motif}}',
    'piece.chrono.aucunFichier': 'Ningún archivo depositado', 'piece.chrono.aucunFichier.texte': 'El archivo y su huella aparecen aquí tras el depósito.',
    'piece.chrono.aucun': 'Ningún evento', 'piece.chrono.aucun.texte': 'La cronología del documento empieza con su primer depósito.',
    'pvr.tache.aPrendreSansUnite': 'Por tomar', 'pvr.tache.toutesUnites': 'Todas las unidades',
    'parcoursAffectation.erreur.manquantes_one': 'Designe una persona para la función señalada.',
    'parcoursAffectation.erreur.manquantes_other': 'Designe una persona para cada una de las {{count}} funciones señaladas.',
  },
}
