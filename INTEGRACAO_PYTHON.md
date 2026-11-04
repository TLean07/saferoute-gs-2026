# Contrato para a equipe de Dynamic Programming
GET /api/graph devolve version=1, city, simulated=true, points, segments, occurrences, generatedAt.
Point: id, regionId, name, x, y. Coordenadas x/y são esquemáticas, não latitude/longitude.
Segment: id, fromId, toId, name, meters, accessible, lighting, blocked.
Occurrence: id, segmentId, type, severity, status, source, description, observedAt, validUntil.
O consumo é somente leitura. Não compartilhar credenciais Oracle com outros serviços.
POST /api/routes é a implementação Java autônoma; não chama um serviço Python ainda inexistente.
A integração futura pode usar a mesma entrada e saída: fromId/toId/accessible/night -> status/points/segments/meters/cost/warnings/calculatedAt.
