# SafeRoute - Global Solution CitizenTech 2026

## Ficha da CitizenTech
- Nome: SafeRoute.
- Cidade: São Paulo. Bairro ainda não delimitado; rede demonstrativa simulada.
- Cidadão: moradores, trabalhadores e pessoas com mobilidade reduzida.
- Problema: dificuldade de consultar condições dos trechos e alternativas durante chuvas.
- Funcionamento: registro de ocorrências/leituras, alertas e cálculo de trajetos respeitando restrições.
- Partes: Java fornece API, regras e persistência; Oracle armazena o domínio; GET /api/graph é o contrato para a equipe de algoritmos Python.
- Impacto proposto: taxa de trajetos que evitam bloqueios e tempo para localizar alternativa. Nenhuma medição de impacto real foi realizada.
- ODS: 11 (mobilidade e resiliência urbana), 10 (acessibilidade), 13 (adaptação a eventos climáticos). Contribuição proposta, não impacto comprovado.
- Repositório: https://github.com/TLean07/saferoute-gs-2026

## Integrantes
Bruno Gama Masseiras - RM562526
Kauai Rosa de Assis Rocha - RM563256
Leandro Afonso Silva Santos Junior - RM561344
Lucas Gilmar Pedro da Silva - RM566231
Murilo da Silva Basto - RM565445

## Executar
Java JDK 21 e Maven Wrapper incluído. Spring Boot 3.3.5.
```
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```
Windows: substituir ./mvnw por .\mvnw.cmd. Abrir http://localhost:8080.
O perfil demo usa H2 descartável. Chave local de operador: demo-saferoute-local.
O perfil oracle é o padrão e exige DB_URL, DB_USER, DB_PASSWORD, ADMIN_API_KEY.
Use jdbc:oracle:thin:@//oracle.fiap.com.br:1521/ORCL como URL FIAP.
Execute sql/01_ddl.sql e sql/02_dml.sql somente em namespace SR_ vazio. Não há DROP automático.
ADMIN_API_KEY precisa de pelo menos 16 caracteres e deve ser secreta. Nunca colocar senha no Git.
O banco Oracle não é recriado no startup. A chave de operador protege POST/PUT/DELETE de cadastro.
POST /api/routes e GETs são públicos. Não existem dados pessoais de moradores.

## API
CRUD completo: /api/regions, /api/points, /api/segments, /api/sensors, /api/occurrences.
GET coleção, GET /{id}, POST (201+Location), PUT /{id}, DELETE /{id} (204).
Monitoramento: GET/POST /api/readings; GET /api/alerts; GET /api/health; GET /api/graph.
Rotas: POST /api/routes com {"fromId":1,"toId":3,"accessible":true,"night":false}.
Erros 400 validação, 401 operador, 404 inexistente, 409 integridade, 503 banco e 500 inesperado.
Horários em UTC; campos LocalDateTime sem sufixo representam UTC.

## Regras
Rede pedestre bidirecional e simulada. Dijkstra em Java mantém a API demonstrável de forma autônoma.
O módulo Python da outra disciplina pode consumir /api/graph; não está implementado neste pacote.
Trecho bloqueado ou alagamento vigente fica fora do grafo. Severidade HIGH também exclui o trecho.
Perfil acessível só aceita accessible=1. Distância não compensa essas restrições.
Custos restantes: metros × (1 + risco MEDIUM 0,6 ou LOW 0,2 + iluminação inadequada à noite 0,5).
Pesos heurísticos não validados em campo. Uma ocorrência vencida produz aviso de condição desconhecida.
NO_ROUTE significa ausência de alternativa no grafo; nunca uma rota fictícia.
Leituras acima do limiar do sensor geram ocorrência/alerta simulados; limiar didático, não norma de segurança.
Alertas são consultados na plataforma. Não há SMS/push, integração oficial ou previsão meteorológica.
Fechar ocorrência remove seu alerta da lista ativa; excluir remove alertas dependentes na mesma transação.
Nenhuma rota deste protótipo deve orientar deslocamentos reais.

## Render
Dockerfile e render.yaml incluídos. Runtime Docker, plano free, health /api/health.
Configure DB_URL, DB_USER, DB_PASSWORD e ADMIN_API_KEY no painel; perfil oracle.
O health consulta o banco, portanto UP comprova conectividade nessa requisição.
Os limites e disponibilidade do serviço gratuito podem causar inicialização demorada.
Consulte STATUS_ENTREGA.md para URL e verificações finais.

## Referências
Enunciado GS CitizenTech FIAP 2026, páginas 31-33. https://render.com/docs/docker
https://render.com/docs/health-checks
