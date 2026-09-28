-- Script atomique Redis pour implémenter l'algorithme Token Bucket par tenant.
-- KEYS[1] : Clé spécifique au tenant (ex: rate_limit:AG-YDE-01)
-- ARGV[1] : Capacité maximale du seau (Burst capacity)
-- ARGV[2] : Taux de remplissage (Tokens ajoutés par minute)
-- ARGV[3] : Timestamp actuel en secondes (Now)

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate_per_min = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- Récupération de l'état actuel du bucket (tokens restants et date du dernier remplissage)
local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

-- Initialisation au premier appel pour un nouveau tenant
if tokens == nil then
    tokens = capacity
    last_refill = now
end

-- Calcul du rechargement basé sur le temps écoulé
local time_passed = math.max(0, now - last_refill)
local tokens_to_add = math.floor(time_passed * (refill_rate_per_min / 60))

if tokens_to_add > 0 then
    tokens = math.min(capacity, tokens + tokens_to_add)
    last_refill = now
end

-- Consommation d'un token pour la requête courante
local allowed = 0
if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end

-- Sauvegarde de l'état muté
redis.call('HMSET', key, 'tokens', tokens, 'last_refill', last_refill)
-- Définition d'une expiration de 2 heures (7200s) pour nettoyer la mémoire des tenants inactifs
redis.call('EXPIRE', key, 7200)

-- Retourne 1 si autorisé, 0 sinon, ainsi que le solde de tokens
return {allowed, tokens}