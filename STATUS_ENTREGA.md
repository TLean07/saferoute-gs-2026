# SafeRoute - estado verificado da entrega

Verificação em 04/11/2026. Projeto referente à GS CitizenTech 2026.

- Código-fonte completo: `src/`, neste ZIP. Build: `pom.xml` e Maven Wrapper incluídos.
- Render: https://saferoute-gs-2026.onrender.com
- Serviço Render: `saferoute-gs-2026`, plano gratuito, Oracle FIAP.
- Arquivos locais conferidos pelo manifesto `MANIFEST_SHA256.json`.
- Nove testes locais passaram com HTTP e H2; evidência em `evidencias/testes_locais.txt`.
- Teste real no Render + Oracle em `evidencias/render_oracle.json`: conexão, grafo, rota acessível, autenticação, CRUD, 404 e proteção de FK.
- Modelo nativo Oracle Data Modeler e scripts completos no ZIP de Database.
- Todos os dados são fictícios; não houve pesquisa de campo, validação com moradores ou integração com sensores oficiais.
- O serviço gratuito pode demorar no primeiro acesso após inatividade. A validação comprova o resultado no instante registrado, sem garantia de disponibilidade futura.

## Para avaliar

1. Abra o deploy e consulte Origem → Destino com acessibilidade marcada.
2. A carga inicial tem alagamento vigente no trecho 1, resultando em 1.200 metros.
3. Para testar alterações sem credenciais privadas, execute o perfil `demo`; chave local `demo-saferoute-local`.
4. Use o PDF para classes, telas, DER, endpoints e execução; o README apresenta o mesmo fluxo em texto.
5. Confirme a data limite do Portal antes da submissão. A entrega ao Portal é feita pelo grupo.

Não há senha Oracle ou chave de produção neste pacote. A configuração de produção está no ambiente do serviço Render.
