# Prompt para continuar o app Android no outro notebook

Copie o texto abaixo (da linha "Contexto" até o fim) e cole no Claude Code aberto
na pasta `corelist-android`.

---

**Contexto.** CompraFácil é um app de lista de compras com controle de orçamento e
leitura de nota fiscal (NFC-e do Amazonas). É o projeto de João e Bruno para o
curso de Empreendedorismo Digital da FPF, com apresentação em 11/11/2026. Já
existem um backend Django REST (JWT, PostgreSQL, publicado na AWS) e o site React
publicado no AWS Amplify, que repassa `/api` para o backend. Esta pasta,
`corelist-android/`, é o app Android nativo, que usa a mesma API. Leia o
`README.md` antes de começar.

**Stack.** Kotlin, Jetpack Compose com Material 3, Navigation Compose com rotas
tipadas, Retrofit, OkHttp e kotlinx.serialization, DataStore para os tokens,
CameraX com ML Kit para QR code e EAN. Gradle Kotlin DSL com version catalog
(`gradle/libs.versions.toml`). AGP 9.3.2, Kotlin 2.2.10, Gradle 9.5.0, minSdk 26,
compileSdk 37. Não troque as versões sem motivo. Se precisar trocar, use versões
estáveis lançadas há pelo menos duas semanas.

**O que já está pronto** (commits locais, ainda sem GitHub):
- Login e cadastro. Em 401 o app renova o token uma vez. O refresh é rotacionado,
  então o app salva sempre o par novo. Se a renovação falhar, volta ao login com
  "Sua sessão expirou". Sair fica no avatar da tela Início. Código em
  `data/api/Rede.kt` e `data/Sessao.kt`.
- Layout mobile do Bruno: barra inferior com Início, Listas, botão central Ler
  nota, Produtos e Menor preço (`ui/Navegacao.kt`).
- Início, Minhas listas e modo de compra (`ui/compra/`):
  - saldo do orçamento em destaque;
  - Unidade ou Peso (kg), com − e + de 1 em 1 ou de 0,1 kg;
  - editar preço, remover item, concluir compra, excluir lista.
- Nota fiscal (`ui/nota/`): câmera com lanterna ou link colado, depois
  conferência e compra concluída. A resposta 409 (nota já importada) leva para a
  compra existente.
- Produtos com leitura de código de barras (`products/lookup/`). Menor preço é
  calculado no app a partir das compras concluídas.
- 12 testes JVM (`./gradlew testDebugUnitTest`), com respostas reais da API em
  `app/src/test/resources`.

**O que falta, nesta ordem:**
1. **Testar tudo no emulador ou num celular.** O PC anterior não aguentava o
   emulador, então as telas quase não foram vistas rodando. Percorra o fluxo
   inteiro:
   - login, sessão expirada e sair;
   - criar lista com orçamento;
   - modo de compra: adicionar produto, unidade e kg, marcar itens, estourar o
     orçamento, concluir, excluir;
   - ler e importar uma nota;
   - Produtos e Menor preço.

   Corrija o que estiver quebrado ou feio. Tire prints (`adb exec-out screencap
   -p`) para conferir o visual.
2. **Testar o APK de release** (`./gradlew assembleRelease`). Ele passa pelo R8,
   que pode quebrar a serialização ou o Retrofit. Se quebrar, ajuste o
   `app/proguard-rules.pro`.
3. Pequenos ajustes de visual para ficar igual ao design: tela de listas, cartão
   de resumo e tela escura do leitor.
4. Só se sobrar tempo: o GitHub Actions gera o APK automaticamente.

**Como testar.**
- **Sem backend local:** use a variante **release** (Build › Select Build
  Variant), que aponta para
  `https://mvp-comprafacil.dql3968zgbfv8.amplifyapp.com/api`. Ou rode o debug com
  `-Pcomprafacil.apiUrl=<essa URL>`. Use uma conta que o usuário já tem no site.
  Não crie contas no site publicado por conta própria: peça ao usuário.
- **Com backend local** (se o repositório `corelist-backend` estiver na máquina):
  `python manage.py seed_demo` e `python manage.py runserver 0.0.0.0:8000`. O
  debug usa `http://10.0.2.2:8000/api`. A conta demo é `demo@comprafacil.test`;
  a senha está na constante `DEMO_SENHA` do comando `seed_demo`. Não repita a
  senha no chat.
- No emulador, a câmera é uma cena virtual: teste a nota com "Colar o link da
  nota".
- Sem `JAVA_HOME`, use o Java do Android Studio para o Gradle (`<Android
  Studio>/jbr`).

**Contrato da API** (base `/api/`):
- **Conta:**
  - `POST auth/login/` {email, password} → {access, refresh}
  - `POST auth/register/` {first_name, email, password}
  - `POST auth/login/refresh/` {refresh} → {access, refresh}
  - `POST auth/logout/` {refresh}
  - `GET auth/me/`
- **Listas:**
  - `GET/POST lists/` {name, budget?}. O GET traz as ativas e as concluídas, com
    `items`.
  - `PATCH/DELETE lists/<id>/`. A lista concluída dá 404 no PATCH, mas aceita
    DELETE.
  - `POST lists/<id>/complete/`
  - `lists/<id>/items/[<item_id>/]` {product, quantity, measure "unit"|"kg",
    price, is_completed}. Em kg, o preço é o do kg. Os itens continuam editáveis
    depois da compra concluída.
- **Produtos:** `GET/POST products/`, `GET products/lookup/?barcode=`.
- **Nota fiscal:**
  - `POST lists/nfce/ler/` {link} → {loja, emitida_em, total, itens[{nome,
    codigo_loja, unidade, medida, quantidade, preco, total, produto}],
    lista_importada}
  - `POST lists/nfce/importar/` {link, nome, itens[{nome, quantidade, medida,
    preco}]} → 201 com a lista. 409 quer dizer já importada (`list_id`).
  - O QR impresso vem como
    `http://sistemas.sefaz.am.gov.br/nfceweb/consultarNFCe.jsp?p=...|3|1`.
- Os decimais vêm como texto ("24.90") e o app usa BigDecimal. Os erros vêm como
  {"detail": "..."} ou {"campo": ["msg"]}.

**Regras.**
- Trabalhe só em `corelist-android/`. Não altere o backend nem o site. Se algo
  precisar do backend, anote e avise.
- Fora do escopo agora, porque depende do backend: juntar a nota a uma lista que
  já existe e a tela "Planejado × nota".
- Textos da interface em português do Brasil. Cores do site: índigo `#4F46E5`,
  texto `#0F172A`, secundário `#64748B`, fundo `#F8FAFC`. Verde, âmbar e
  vermelho só para a situação do orçamento. Títulos em Poppins, texto em Inter.
- Faça commits locais pequenos, com mensagem em português. Não faça push nem crie
  repositório no GitHub sem o usuário pedir.
- Ao terminar cada etapa, diga o que funcionou, o que quebrou e onde está o APK.
