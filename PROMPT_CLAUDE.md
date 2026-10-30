# Prompt para continuar o app Android (atualizado em 30/10/2026)

Copie o texto abaixo (da linha "Contexto" até o fim) e cole no Claude Code aberto
na pasta `corelist-android`.

---

**Contexto.** CompraFácil é um app de lista de compras com controle de orçamento,
leitura de nota fiscal (NFC-e do Amazonas) e preços reais da cidade tirados das
notas. É o projeto de João e Bruno para o curso de Empreendedorismo Digital da FPF,
com apresentação em **11/11/2026**. Já existem:

- o backend Django REST (JWT, PostgreSQL), publicado na AWS (EC2), com deploy
  automático pelo GitHub Actions;
- o site React, publicado no AWS Amplify em
  https://mvp-comprafacil.dql3968zgbfv8.amplifyapp.com, que repassa `/api` para o
  backend.

Esta pasta, `corelist-android/`, é o app Android nativo (repositório
https://github.com/jvxp34/corelist-android). Ele usa a mesma API e deve ter
**tudo o que o site já faz**. O site é a referência de comportamento e de textos:
abra-o no navegador para ver cada tela. Leia o `README.md` antes de começar.

**Stack.** Kotlin, Jetpack Compose com Material 3, Navigation Compose com rotas
tipadas, Retrofit, OkHttp e kotlinx.serialization, DataStore para os tokens,
CameraX com ML Kit para QR code e EAN. Gradle Kotlin DSL com version catalog
(`gradle/libs.versions.toml`). AGP 9.3.2, Kotlin 2.2.10, Gradle 9.5.0, minSdk 26,
compileSdk 37. Não troque as versões sem motivo; se precisar, use versões estáveis
lançadas há pelo menos duas semanas. Para carregar imagens da internet (foto do
produto), pode adicionar o Coil.

## O que o app já tem (versão de 28/10)

- **Login e cadastro.** Em 401 o app renova o token uma vez. O refresh é
  rotacionado, então o app salva sempre o par novo. Se a renovação falhar, volta ao
  login com "Sua sessão expirou". O refresh dura 30 dias. Sair fica no avatar da
  tela Início (`data/api/Rede.kt`, `data/Sessao.kt`).
- **Barra inferior** (layout do Bruno): Início, Listas, botão central Ler nota,
  Produtos e Menor preço (`ui/Navegacao.kt`).
- **Início, Minhas listas e modo de compra** (`ui/compra/`):
  - saldo do orçamento em destaque;
  - Unidade ou Peso (kg), com − e + de 1 em 1 ou de 0,1 kg;
  - editar preço, remover item, concluir compra, excluir lista.
- **Nota fiscal pelo menu** (`ui/nota/`): câmera com lanterna ou link colado,
  depois conferência e compra concluída. A resposta 409 leva para a compra que já
  existe.
- **Produtos** com leitura de código de barras (`products/lookup/`).
- **Menor preço** calculado no app a partir das compras do próprio usuário. Isto
  vai ser substituído pelos preços da cidade (item 5 abaixo).
- 12 testes JVM (`./gradlew testDebugUnitTest`), com respostas reais da API em
  `app/src/test/resources`.

## O que falta, nesta ordem

Faça uma etapa de cada vez. Em cada uma, rode os testes e confira no emulador ou
num celular antes de passar para a próxima.

1. **Rodar e corrigir o que já existe.** O PC anterior não aguentava o emulador,
   então as telas quase não foram vistas rodando. Percorra o fluxo inteiro e
   corrija o que estiver quebrado ou feio:
   - login, sessão expirada e sair;
   - criar lista com orçamento;
   - modo de compra (unidade e kg, estourar o orçamento, concluir, excluir);
   - ler e importar uma nota.

   Tire prints (`adb exec-out screencap -p > tela.png`) para conferir o visual.

2. **Quantidade com 3 casas.** O backend aceita `quantity` com até 3 casas
   decimais (ex.: 1,235 kg da balança). Mostre o peso com até 3 casas e o preço do
   kg como "R$ 6,99/kg".

3. **Busca de produto ao adicionar na lista.** No lugar de escolher numa lista
   longa, use um campo de busca:
   - **Buscar:** `GET lists/produtos/?q=arr` devolve os produtos ordenados pelo
     começo das palavras (sem acento, acha também os nomes abreviados da nota),
     depois pelos que a pessoa mais compra.
   - **Atalhos:** sem `q`, a mesma rota devolve os mais comprados, para mostrar
     como atalhos de um toque.
   - **Preço e medida:** cada produto traz `ultimo_preco` e `ultima_medida`, que
     preenchem o preço e a medida sozinhos.
   - **Cadastrar:** se não achar, oferece "Cadastrar <texto>".

4. **Nota fiscal dentro da lista** (ideia do Bruno). Na lista em andamento, botão
   "Já comprou? Concluir com a nota fiscal":
   - **Ler:** `POST lists/<id>/nfce/ler/` cruza a nota com a lista. Cada item da
     nota vem com `list_item`, que é o item da lista que casou ou null quando o
     item está fora da lista. Vem também `produto_sugerido` para os itens fora da
     lista.
   - **Esquecidos:** os itens da lista que não estão na nota vêm em `esquecidos`.
   - **Conferir:** a tela mostra três grupos:
     - comprados, com a diferença entre o preço planejado e o pago;
     - fora da lista;
     - esquecidos.

     A pessoa pode trocar o item da lista ligado a cada item da nota, renomear ou
     tirar itens.
   - **Concluir:** `POST lists/<id>/nfce/concluir/`. Preço e quantidade vêm sempre
     da nota, lida de novo no servidor. Os esquecidos ficam como "não comprado".
   - **Não comprados:** numa compra concluída, mostre a seção "Não comprados" com
     "Passar para a próxima lista". Esse botão cria uma lista nova com esses itens
     usando `lists/<id>/repetir/`, com os ids dos não comprados em `itens`.

5. **Preços da cidade** (aba "Menor preço"). Toda nota lida vira preço público:
   produto, loja, preço e data, sem o usuário. Troque o cálculo local pela API.
   - **Tela "Produtos com menor preço":**
     - busca e o seletor 7, 30 ou 90 dias;
     - por produto: "Mais barato agora", que só conta preços de até 1 dia;
     - "Menor preço em N dias";
     - a lista de lojas, com o aviso "pode ter mudado" quando
       `desatualizado=true`.
   - **Na lista em andamento:**
     - cartão "Melhor loja para esta lista": primeiro a loja que tem mais itens com
       preço, depois o menor total;
     - por item, "Mais barato agora: <loja> · R$ x".
   - **Botão "Como chegar"** abre o Google Maps já com a rota até o endereço da
     loja:
     `https://www.google.com/maps/dir/?api=1&destination=<endereço codificado>&travelmode=driving&dir_action=navigate`.
     Use um `Intent.ACTION_VIEW` com essa URL.

6. **Foto do produto e logo da loja.**
   - **Foto:** mostre `image_url` do produto quando existir. Sem foto, mostre um
     ícone neutro.
   - **"Adicionar foto":** para produto sem foto, a câmera lê o código de barras da
     embalagem e chama `POST products/<id>/codigo-de-barras/`. Essa rota preenche
     foto, marca e embalagem pelo Open Food Facts. Mostre o crédito "Dados: Open
     Food Facts" onde esses dados aparecem.
   - **Logo:** use o campo `icone` da loja (`assai`, `atacadao`, `carrefour` ou
     `db`). Copie os PNGs de `corelist-frontend-v2/public/lojas/` para
     `res/drawable` ou baixe de `<site>/lojas/<icone>.png`.
   - **Nome:** mostre `marca` como nome da loja quando existir. Sem logo, mostre um
     círculo com as iniciais do nome.

7. **Sugerir a próxima lista.** Depois de salvar uma compra pela nota (menu) e
   depois de concluir a lista com a nota, pergunte "Criar a próxima lista com estes
   produtos?":
   - mostre os itens comprados, todos marcados, que a pessoa pode desmarcar;
   - mostre o campo do nome da lista, que começa como "Próxima compra";
   - dê os botões "Criar lista" e "Agora não".

   "Criar lista" chama `POST lists/<id>/repetir/` e abre a lista nova. O preço pago
   vira o preço planejado, e o orçamento é o mesmo da compra.

8. **Excluir compra concluída** (com confirmação), por exemplo uma nota importada
   por engano: `DELETE lists/<id>/`.

9. **APK de release.** Gere com `./gradlew assembleRelease` e teste num celular de
   verdade. O R8 pode quebrar a serialização ou o Retrofit; se quebrar, ajuste
   `app/proguard-rules.pro`. Na apresentação, o APK vai ser instalado num celular.

10. Só se sobrar tempo: o GitHub Actions gera o APK automaticamente a cada push.

**Não faça:** Grupos (foi removido do site), insights com IA e redefinir senha.

## Como testar

- **Sem backend local:** use a variante **release** (Build › Select Build
  Variant), que aponta para
  `https://mvp-comprafacil.dql3968zgbfv8.amplifyapp.com/api`. Ou rode o debug com
  `-Pcomprafacil.apiUrl=<essa URL>`. Use uma conta que o usuário já tem no site.
  Não crie contas no site publicado por conta própria: peça ao usuário.
- **Com backend local** (se o repositório `corelist-backend` estiver na máquina):
  - Rode `python manage.py seed_demo` e `python manage.py runserver 0.0.0.0:8000`.
  - O debug usa `http://10.0.2.2:8000/api`.
  - A conta demo é `demo@comprafacil.test`. A senha está na constante
    `DEMO_SENHA` do comando `seed_demo`; não repita a senha no chat.
  - Os preços da cidade só aparecem depois de ler alguma nota.
- **Nota de teste real** (Assaí, Manaus):
  `https://sistemas.sefaz.am.gov.br/nfceweb/consultarNFCe.jsp?p=13261006057223044209650080000819951081030560%7C3%7C1`.
  Cada nota só pode ser importada uma vez por conta. Para repetir, exclua a compra.
- **No emulador** a câmera é uma cena virtual: teste a nota com "Colar o link da
  nota".
- **Java:** sem `JAVA_HOME`, use o Java do Android Studio para o Gradle
  (`<Android Studio>/jbr`).
- **Servidor no ar:** `GET /api/health/` responde
  `{"status":"ok","banco":"ok","versao":"<commit>"}`.

## Contrato da API (base `/api/`)

Os decimais vêm como texto ("24.90"), e o app usa BigDecimal. Os erros vêm como
`{"detail": "..."}` ou `{"campo": ["msg"]}`. Todas as rotas, menos login, cadastro
e health, exigem `Authorization: Bearer <access>`.

**Conta**
- `POST auth/login/` {email, password} → {access, refresh}
- `POST auth/register/` {first_name, email, password}
- `POST auth/login/refresh/` {refresh} → {access, refresh}
- `POST auth/logout/` {refresh}
- `GET auth/me/`; `PATCH auth/me/` {first_name, last_name}

**Listas**
- `GET lists/` traz as listas em andamento e as concluídas, com `items` e `total`.
  `POST lists/` recebe {name, budget?}.
- `GET/PATCH lists/<id>/` só para listas em andamento (a concluída dá 404).
  `DELETE lists/<id>/` aceita as duas.
- `POST lists/<id>/complete/`
- `GET lists/history/` lista as compras concluídas.
- Itens: `lists/<id>/items/[<item_id>/]` {product, quantity (até 3 casas), measure
  "unit"|"kg", price, is_completed}.
  - Em kg, o preço é o do kg.
  - Os itens de uma compra concluída continuam editáveis.
  - Numa compra concluída, `is_completed=false` quer dizer "não comprado".
- `POST lists/<id>/repetir/` {nome?, itens?: [ids de item]} cria uma lista nova
  em andamento e devolve 201 com a lista.
  - Copia produto, quantidade e medida, e o preço pago vira o preço planejado.
  - Sem `itens`, copia só os itens comprados.
  - Devolve 400 se nenhum item servir.
- `GET lists/produtos/?q=` → [{id, name, brand, embalagem, ultimo_preco,
  ultima_medida, vezes}]

**Produtos**
- `GET/POST products/`: {name, brand, package_size, barcode, image_url, ...}
- `GET products/lookup/?barcode=` devolve o produto do catálogo ou uma sugestão do
  Open Food Facts.
- `POST products/<id>/codigo-de-barras/` {barcode} → {produto, encontrado}
  - Preenche só os campos vazios: foto, marca e embalagem.
  - Dá 409 se o produto já tem outro código ou se o código é de outro produto.

**Nota fiscal**
- O QR impresso vem como
  `http://sistemas.sefaz.am.gov.br/nfceweb/consultarNFCe.jsp?p=...|3|1`. Aceite
  http e o `|` cru.
- `POST lists/nfce/ler/` {link} → {chave, loja, cnpj, emitida_em, total,
  itens[{nome, codigo_loja, unidade, medida, quantidade, preco, total,
  produto{id,name}|null}], lista_importada}
- `POST lists/nfce/importar/` {link, nome, itens[{nome, quantidade, medida,
  preco}]} → 201 com a lista. 409 quer dizer que a nota já foi importada
  (`list_id`).
- `POST lists/<id>/nfce/ler/` {link} → {chave, loja, endereco, emitida_em, total,
  orcamento, itens[{indice, nome, codigo_loja, medida, quantidade, preco, total,
  list_item, motivo, produto_sugerido{id,name,motivo}|null}], itens_lista[{id,
  nome, quantidade, medida, preco}], esquecidos[ids], lista_importada}
- `POST lists/<id>/nfce/concluir/` {link, itens[{indice, list_item?, produto?,
  nome?}]} → a lista concluída + `esquecidos`

**Preços da cidade**
- `GET lists/precos/?q=&dias=7|30|90` → [{produto{id, name, brand, embalagem,
  image_url}, mais_barato_agora, menor_no_periodo{preco, medida, loja, data, dias},
  lojas[]}]
  - Sem `q`, devolve os preços mais recentes.
  - `mais_barato_agora` e cada linha de `lojas` têm o formato {loja, preco,
    medida, data, desatualizado}.
- `GET lists/precos/produto/<id>/?dias=` devolve o mesmo resumo para um produto.
- `GET lists/<id>/precos/` → {total_itens, itens[{item, mais_barato_agora,
  lojas_com_preco}], melhor_loja[{loja, total, itens, desatualizados}]}
- Loja: {id, nome, marca, icone, endereco}

## Regras

- Trabalhe só em `corelist-android/`. Não altere o backend nem o site. Se algo
  precisar do backend, anote e avise.
- Os textos da interface ficam em português do Brasil, iguais aos do site.
- **Cores:**
  - índigo `#4F46E5`;
  - texto `#0F172A`;
  - secundário `#64748B`;
  - fundo `#F8FAFC`;
  - verde, âmbar e vermelho só para a situação do orçamento.
- **Fontes:** títulos em Poppins, texto em Inter.
- Faça commits pequenos, com mensagem em português. Avise e peça permissão antes
  de qualquer push.
- Nunca peça nem mostre senhas, chaves ou o `.env` do servidor no chat.
- Ao terminar cada etapa, diga o que funcionou, o que quebrou e onde está o APK.
