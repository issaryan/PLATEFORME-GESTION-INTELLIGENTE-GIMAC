-- Barre latérale du DES en sections repliables (05/11/2026) : quatre fonctionnalités changent de section, et
-- seulement si leur groupe vaut encore l'ancienne valeur de départ. Un groupe déjà changé par un administrateur
-- (console, écran des fonctionnalités) n'est jamais écrasé, et les autres champs de la définition sont gardés.
update fonctionnalite
   set definition = jsonb_set(definition::jsonb, '{groupe}', '"processus"')::text
 where cle in ('processus', 'lancerDemande', 'mesDemandes')
   and definition::jsonb ->> 'groupe' = 'monActivite';
update fonctionnalite
   set definition = jsonb_set(definition::jsonb, '{groupe}', '"homologation"')::text
 where cle = 'institutions'
   and definition::jsonb ->> 'groupe' = 'registre';
