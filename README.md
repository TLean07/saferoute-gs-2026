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
| Território | Jardim Pantanal, distrito Jardim Helena, São Paulo (SP). Rede demonstrativa simulada. |
| Público | Moradores, trabalhadores e pessoas com mobilidade reduzida. |
| Problema | Dificuldade para consultar condições dos trechos e alternativas durante chuvas. |
| Capacidade central | Registrar ocorrências e leituras, apresentar alertas e calcular alternativas. |
| Integração | Java + Oracle; `/api/graph` disponibiliza os dados à equipe de algoritmos Python. |
| ODS | 11: mobilidade e resiliência urbana; 10: acessibilidade; 13: adaptação climática. |
| Avaliação proposta | Medir desvio de bloqueios e tempo para localizar alternativa. Ainda sem medição em campo. |

O Jardim Pantanal foi escolhido por seu histórico de inundações, documentado pela [Prefeitura de São Paulo](https://gestaourbana.prefeitura.sp.gov.br/urbanismo-social-jardim-pantanal/). O bairro é real; os setores, pontos, trechos e sensores do protótipo são simulados. Os cenários são fictícios. Não houve pesquisa com moradores ou comprovação de impacto real nesta entrega. A plataforma não orienta deslocamentos reais nem substitui alertas oficiais.

---

### 📖 Sobre o Projeto

A SafeRoute reúne informações sobre circulação em períodos de chuva. O cidadão escolhe origem, destino e necessidade de acessibilidade; a aplicação desconsidera trechos bloqueados e apresenta uma alternativa quando ela existe na rede.

O backend tem modelos, DAOs com SQL parametrizado, BOs com regras de negócio, controllers REST e tratamento centralizado de exceções. A interface web acompanha a API para demonstrar o fluxo completo.

**[Documentação PDF](SafeRoute_Java_GS.pdf)** · **[Código-fonte](src/main/java/br/com/saferoute/)** · **[Interface web](src/main/resources/static/index.html)** · **[Scripts SQL](sql/)** · **[Testes](src/test/java/br/com/saferoute/)**

Todos os arquivos desta entrega estão neste ZIP. Extraia a pasta completa e execute o projeto a partir dela. O Maven Wrapper incluído baixa as dependências na primeira execução.

Para navegar no pacote: instruções de execução abaixo, [requisições HTTP](requisicoes.http), [evidências](evidencias/) e [contrato de integração](INTEGRACAO_PYTHON.md). O deploy é apenas uma demonstração adicional: https://saferoute-gs-2026.onrender.com.

---

### 📁 Estrutura do Projeto

```text
saferoute/
├── src/main/java/br/com/saferoute/
│   ├── Region, Point, Segment, Sensor, Reading, Occurrence, Alert
│   ├── *DAO.java               # SQL, sequências Oracle e persistência
│   ├── *BO.java                # Validações e regras de negócio
│   ├── *Controller.java        # Endpoints REST
│   ├── RouteBO.java            # Dijkstra e restrições de circulação
│   ├── ApiErrors.java          # Respostas de erro consistentes
│   └── ApiAccess.java          # Proteção das alterações por chave
├── src/main/resources/
│   ├── application*.properties # Perfis oracle e demo
│   └── static/index.html       # Interface responsiva
├── src/test/java/              # Testes de integração HTTP com H2
├── sql/                       # DDL, DML e DQL Oracle
├── evidencias/                # Resultados reais de verificação
├── Dockerfile / render.yaml   # Publicação no Render
├── INTEGRACAO_PYTHON.md        # Contrato do grafo para outra disciplina
└── SafeRoute_Java_GS.pdf       # Documento único da disciplina
```

---

### 🧮 Regras de Negócio

- **Bloqueios:** trecho bloqueado, alagamento vigente ou ocorrência HIGH retira a aresta do cálculo.
- **Acessibilidade:** o perfil acessível aceita apenas trechos com `accessible=1`.
- **Custo:** metros × (1 + risco MEDIUM 0,6 ou LOW 0,2 + iluminação insuficiente à noite 0,5). Pesos didáticos, ainda não validados em campo.
- **Validade:** registros vencidos deixam de bloquear, mas geram aviso de condição desconhecida.
- **Sem alternativa:** `NO_ROUTE` devolve listas vazias, sem fabricar um trajeto.
- **Sensores:** leituras recentes acima do limiar didático geram ocorrência e alerta simulados. Sensores inativos não recebem leituras.
- **Alertas:** a lista mostra apenas ocorrências não encerradas e ainda válidas. Excluir uma ocorrência remove seus alertas na mesma transação.
- **Tempo:** campos `LocalDateTime` representam UTC; a conexão Oracle usa sessão UTC.

O grafo é pedestre, bidirecional e esquemático. Não existe integração com trânsito, previsão meteorológica, SMS ou push. O algoritmo Java permite demonstrar a API; o módulo Python permanece sob responsabilidade da outra disciplina.

---

### ▶️ Como Executar

**Pré-requisitos:** JDK 21 e internet para baixar dependências pelo Maven Wrapper 3.9.11.

**Windows / PowerShell – demonstração local:**

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Também é possível abrir `INICIAR_DEMO.cmd`. Acesse `http://localhost:8080`. O perfil `demo` usa H2 descartável e chave `demo-saferoute-local`, válida somente nesse ambiente.

**Linux / macOS:**

```bash
sh mvnw test
sh mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

**Oracle FIAP:**

1. Em um namespace `SR_` vazio, execute `sql/01_ddl.sql` e `sql/02_dml.sql`. Os scripts não apagam objetos existentes.
2. Defina `DB_URL`, `DB_USER`, `DB_PASSWORD` e `ADMIN_API_KEY` no ambiente local ou no painel Render. Não grave segredos no Git.
3. Use a URL `jdbc:oracle:thin:@//oracle.fiap.com.br:1521/ORCL` e uma chave de operador com pelo menos 16 caracteres.
4. Execute `mvnw.cmd spring-boot:run`. O perfil padrão é `oracle`; o banco não é recriado ao iniciar.
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
| `/api/alerts`, `/api/graph`, `/api/health` | GET | Alertas, integração e conectividade |

GETs e POST `/api/routes` são públicos. Demais alterações exigem `X-Admin-Key`. Criação retorna 201; exclusão, 204. Erros: 400, 401, 404, 409, 503 e 500.

```http
POST /api/routes
Content-Type: application/json

{"fromId":1,"toId":3,"accessible":true,"night":false}
```

Com a carga inicial e o alagamento vigente no trecho 1, a resposta contém `FOUND`, pontos `[1,4,5,6,3]` e `1200` metros. Depois da validade dos dados demonstrativos, o cenário muda; revise ocorrências antes de demonstrar.

---

### ✅ Verificações Realizadas

- Nove testes automatizados passaram com HTTP real e H2 em modo Oracle.
- Render + Oracle FIAP: health, grafo, rota, autenticação, CRUD, erro 404 e integridade referencial verificados.
- As três consultas DQL e a carga de 39 registros foram conferidas pelo MCP SQLcl.
- O modelo nativo foi importado, salvo, reaberto e exportado no Oracle Data Modeler.

Resultados em `evidencias/` e `STATUS_ENTREGA.md`. Testes H2 e testes no Oracle estão identificados separadamente.

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
| Oracle Data Modeler 24.3.1 / SQLcl | Modelagem e validação SQL |

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
        <img src="https://github.com/TLean07.png" width="90px;" alt="Leandro Afonso Silva Santos Junior"/><br />
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
