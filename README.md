<h1 align="center">Global Solution – Domain Driven Design – Java</h1>
<p align="center">SafeRoute: API de ocorrências, alertas e trajetos alternativos desenvolvida com Spring Boot e Oracle FIAP.</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Tecnologia principal">
  <img src="https://img.shields.io/badge/FIAP-Engenharia%20de%20Software-E4002B" alt="FIAP">
  <img src="https://img.shields.io/badge/Global%20Solution-CitizenTech-103f4c" alt="CitizenTech 2026">
</p>

---

### 📋 Ficha da CitizenTech

| Campo | Definição |
| --- | --- |
| Solução / equipe | SafeRoute |
| Território | Jardim Pantanal e entorno, São Paulo (SP). Malha real do OpenStreetMap. |
| Público | Moradores, trabalhadores e pessoas com mobilidade reduzida. |
| Problema | Dificuldade para consultar condições dos trechos e alternativas durante chuvas. |
| Capacidade central | Registrar ocorrências e leituras, apresentar alertas e calcular alternativas. |
| Integração | API Java + Oracle; OpenStreetMap / Overpass para a malha e Open-Meteo para previsão. |
| ODS | 11: mobilidade e resiliência urbana; 10: acessibilidade; 13: adaptação climática. |
| Avaliação proposta | Medir desvio de bloqueios e tempo para localizar alternativa. Ainda sem medição em campo. |

O Jardim Pantanal foi escolhido por seu histórico de inundações, documentado pela [Prefeitura de São Paulo](https://gestaourbana.prefeitura.sp.gov.br/urbanismo-social-jardim-pantanal/). A aplicação usa 1.188 pontos e 1.427 segmentos reais extraídos do OpenStreetMap, com coordenadas e atribuição de origem. A previsão vem da API Open-Meteo. Os sensores de teste ficam separados da malha publicada. Não houve pesquisa de campo nem confirmação automática de alagamento por rua.

---

### 📖 Sobre o Projeto

A SafeRoute reúne informações sobre circulação em períodos de chuva. O cidadão escolhe origem, destino e necessidade de acessibilidade; a aplicação desconsidera trechos bloqueados e apresenta uma alternativa quando ela existe na rede.

O backend tem modelos, DAOs com SQL parametrizado, BOs com regras de negócio, controllers REST e tratamento centralizado de exceções. A interface web acompanha a API para demonstrar o fluxo completo.

**[Documentação PDF](SafeRoute_Java_GS.pdf)** · **[Código-fonte](src/main/java/com/tlean07/saferoute/)** · **[Interface web](src/main/resources/static/index.html)** · **[Scripts SQL](sql/)** · **[Testes](src/test/java/com/tlean07/saferoute/)**

Todos os arquivos desta entrega estão neste ZIP. Extraia a pasta completa e execute o projeto a partir dela. O Maven Wrapper incluído baixa as dependências na primeira execução.

Instruções de execução abaixo e exemplos em [requisições HTTP](requisicoes.http). Aplicação publicada: https://saferoute-gs-2026.onrender.com.

---

### 📁 Estrutura do Projeto

```text
saferoute/
├── src/main/java/com/tlean07/saferoute/
│   ├── Region, Point, Segment, Sensor, Reading, Occurrence, Alert
│   ├── *DAO.java               # SQL, sequências Oracle e persistência
│   ├── *BO.java                # Validações e regras de negócio
│   ├── *Controller.java        # Endpoints REST
│   ├── RouteBO.java            # Dijkstra e restrições de circulação
│   ├── ApiErrors.java          # Respostas de erro consistentes
│   └── ApiAccess.java          # Proteção das alterações por chave
├── src/main/resources/
│   ├── osm-network.json       # Extrato OSM com georreferência e proveniência
│   ├── application*.properties # Perfis oracle e demo
│   └── static/                # Interface Leaflet, JS, CSS e biblioteca local
├── src/test/java/              # Testes de integração HTTP com H2
├── sql/                       # Criação e carga inicial do Oracle
├── Dockerfile / render.yaml   # Publicação no Render
└── SafeRoute_Java_GS.pdf       # Documento único da disciplina
```

---

### 🧮 Regras de Negócio

- **Bloqueios:** trecho bloqueado, alagamento vigente ou ocorrência HIGH retira a aresta do cálculo. Os 11 segmentos OSM com acesso restrito também são excluídos do cálculo de passagem.
- **Acessibilidade:** o filtro aceita apenas `accessible=1`. No extrato atual não há `wheelchair=yes`; os dados ausentes são desconhecidos, e exigir acessibilidade retorna `NO_ROUTE`.
- **Custo:** metros × (1 + risco MEDIUM 0,6 ou LOW 0,2 + iluminação insuficiente à noite 0,5). Pesos didáticos, ainda não validados em campo.
- **Validade:** registros vencidos deixam de bloquear, mas geram aviso de condição desconhecida.
- **Sem alternativa:** `NO_ROUTE` devolve listas vazias, sem fabricar um trajeto.
- **Sensores:** a carga didática mantém sensores simulados fora da região real (ID 900). Ocorrências `SIMULATED` são rejeitadas nessa malha e excluídas do mapa e do cálculo publicado.
- **Alertas:** a lista mostra apenas ocorrências não encerradas e ainda válidas. Excluir uma ocorrência remove seus alertas na mesma transação.
- **Tempo:** campos `LocalDateTime` representam UTC; a conexão Oracle usa sessão UTC.

O grafo é pedestre e bidirecional, obtido das vias permitidas no OSM dentro de um recorte geográfico. Restrições explícitas de acesso, barreiras e sentidos exclusivos para pedestres são excluídos na preparação. Distâncias são somadas a partir das coordenadas. Não há integração com trânsito, criminalidade, SMS ou push.

---

### ▶️ Como Executar

**Pacote Java:** `com.tlean07.saferoute`.

**Pré-requisitos:** JDK 21 e internet para baixar dependências pelo Maven Wrapper 3.9.11.

**Windows / PowerShell – demonstração local:**

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Também é possível abrir `INICIAR_DEMO.cmd`. Acesse `http://localhost:8080`. O perfil `demo` usa H2 descartável, importa a mesma malha OSM real e usa a chave `demo-saferoute-local`, válida somente nesse ambiente.

**Linux / macOS:**

```bash
sh mvnw test
sh mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

**Oracle FIAP:**

1. Em um namespace `SR_` vazio, execute `sql/01_ddl.sql` e `sql/02_dml.sql`. Os scripts não apagam objetos existentes.
2. Defina `DB_URL`, `DB_USER`, `DB_PASSWORD` e `ADMIN_API_KEY` no ambiente local ou no painel Render. Não grave segredos no Git.
3. Use a URL `jdbc:oracle:thin:@//oracle.fiap.com.br:1521/ORCL` e uma chave de operador com pelo menos 16 caracteres.
4. Execute `mvnw.cmd spring-boot:run`. O perfil padrão é `oracle`. `RealNetwork` importa os registros OSM ausentes em uma transação, sem recriar tabelas nem sobrescrever ocorrências ou bloqueios. A região 900 identifica a malha real.
5. Confira `/api/health`: a resposta `UP` depende de uma consulta real ao banco.

No Render, o serviço usa Docker, plano gratuito e health check `/api/health`. O primeiro acesso após inatividade pode demorar. A chave de produção é privada; para avaliar alterações, use a demonstração local ou uma chave fornecida pelo responsável pelo deploy.

---

### 💻 Endpoints e Exemplo de Uso

| Recurso | Métodos | Finalidade |
| --- | --- | --- |
| `/api/regions` | GET, POST; GET, PUT, DELETE em `/{id}` | Regiões |
| `/api/points` | GET, POST; GET, PUT, DELETE em `/{id}` | Nós da rede |
| `/api/segments` | GET, POST; GET, PUT, DELETE em `/{id}` | Trechos |
| `/api/sensors` | GET, POST; GET, PUT, DELETE em `/{id}` | Sensores simulados |
| `/api/occurrences` | GET, POST; GET, PUT, DELETE em `/{id}` | Ocorrências |
| `/api/readings` | GET, POST | Leituras |
| `/api/routes` | POST | Alternativa de trajeto |
| `/api/alerts`, `/api/graph`, `/api/health` | GET | Ocorrências vigentes, malha georreferenciada e conectividade |
| `/api/weather` | GET | Previsão regional Open-Meteo com fonte, horário e estado do cache |

GETs e POST `/api/routes` são públicos. Demais alterações exigem `X-Admin-Key`. Criação retorna 201; exclusão, 204. Erros: 400, 401, 404, 409, 503 e 500.

```http
POST /api/routes
Content-Type: application/json

{"fromId":13106396488,"toId":7907574602,"accessible":false,"night":false}
```

Use IDs retornados por `/api/graph`. O trajeto padrão percorre vias reais; a rota muda conforme os pontos escolhidos e os bloqueios cadastrados. Nenhum alagamento é criado a partir da previsão meteorológica.

---

### ✅ Verificações Realizadas

- 17 testes automatizados passaram: CRUD HTTP, regras de rota, importação idempotente, separação entre dados reais e simulados e cache/falhas do Open-Meteo.
- Render + Oracle FIAP: health, grafo, rota, autenticação, CRUD, erro 404 e integridade referencial verificados.

Os testes automatizados estão em `src/test/java/` e podem ser executados com o Maven Wrapper.

---

### 🌦️ Origem e atualização dos dados

- **Ruas:** OpenStreetMap, extraído uma vez pela Overpass API em 04/11/2026. O arquivo `src/main/resources/osm-network.json` preserva latitude/longitude, IDs OSM, tags e horário da base. O servidor consulta o Oracle para calcular trajetos; não sobrecarrega a Overpass a cada visita.
- **Previsão:** `/api/weather` consulta Open-Meteo sem chave, com timeout de 15 segundos e cache de 15 minutos. Em falha, uma consulta anterior de até 2 horas é marcada `STALE`; sem dados válidos, retorna `503 / UNAVAILABLE`. Nova tentativa após 60 segundos.
- **Mapa:** Leaflet 1.9.4 incluído localmente e tiles OSM sob demanda, com atribuição visível. Não há download em massa de tiles.
- **Ocorrências:** registros autenticados de operadores, com origem, observação e validade. A tela não apresenta dados sintéticos como alagamentos reais. O CGE é um link oficial de consulta, não uma API integrada.
- **Limites:** previsões são estimativas de modelos regionais. Não confirmam alagamento, iluminação, acessibilidade ou segurança em uma rua. Não há API de alagamento em tempo real integrada nesta versão.

---

### 🚀 Tecnologias Utilizadas

| Tecnologia | Uso |
| --- | --- |
| Java 21 / Spring Boot 3.3.5 | API REST e regras |
| Spring JDBC / Oracle 19c FIAP | Persistência com DAO |
| Bean Validation | Validação das entradas |
| H2 / JUnit / Spring Boot Test | Testes locais |
| Maven Wrapper 3.9.11 / Docker | Build reproduzível |
| Render | Serviço publicado no plano gratuito |
| HTML, CSS e JavaScript | Interface demonstrativa |
| Leaflet 1.9.4 / OpenStreetMap | Mapa e malha real |
| Open-Meteo | Previsão meteorológica por HTTP |

---

### 👥 Integrantes

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/Gamabg">
        <img src="https://github.com/Gamabg.png" width="90px;" alt="Bruno Gama Masseiras"/><br />
        <sub><b>Bruno Gama Masseiras</b></sub>
      </a><br /><sub>RM 562526</sub>
    </td>
    <td align="center">
      <a href="https://github.com/KauaiRosa">
        <img src="https://github.com/KauaiRosa.png" width="90px;" alt="Kauai Rosa de Assis Rocha"/><br />
        <sub><b>Kauai Rosa de Assis Rocha</b></sub>
      </a><br /><sub>RM 563256</sub>
    </td>
    <td align="center">
      <a href="https://github.com/TLean07">
        <img src="https://github.com/TLean07.png" width="90px;" alt="TLean07"/><br />
        <sub><b>TLean07</b></sub>
      </a><br /><sub>RM 561344</sub>
    </td>
    <td align="center">
      <a href="https://github.com/Lucasvssz">
        <img src="https://github.com/Lucasvssz.png" width="90px;" alt="Lucas Gilmar Pedro da Silva"/><br />
        <sub><b>Lucas Gilmar Pedro da Silva</b></sub>
      </a><br /><sub>RM 566231</sub>
    </td>
    <td align="center">
      <a href="https://github.com/MuriloBastos6">
        <img src="https://github.com/MuriloBastos6.png" width="90px;" alt="Murilo da Silva Basto"/><br />
        <sub><b>Murilo da Silva Basto</b></sub>
      </a><br /><sub>RM 565445</sub>
    </td>
  </tr>
</table>

---

### 📚 Referências

- Enunciado FIAP GS CitizenTech 2026, páginas 31–33.
- [Render: Docker](https://render.com/docs/docker) e [health checks](https://render.com/docs/health-checks).

- [OpenStreetMap / ODbL](https://www.openstreetmap.org/copyright), [Overpass](https://dev.overpass-api.de/overpass-doc/en/) e [Open-Meteo / CC BY 4.0](https://open-meteo.com/en/docs).
