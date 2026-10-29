# CompraFácil — app Android

App nativo do CompraFácil (Kotlin + Jetpack Compose). Usa a mesma API do site
(`corelist-backend`), com o visual do site e o layout mobile proposto pelo Bruno
(canvas "CompraFácil Mobile").

## O que o app faz

- **Entrar e criar conta.** Os tokens ficam no aparelho (DataStore). Quando o
  acesso vence, o app renova sozinho uma vez; se não der, volta ao login com o
  aviso "Sua sessão expirou". **Sair** fica no avatar da tela Início.
- **Início:** compra em andamento com o saldo do orçamento, atalhos (Nova lista,
  Ler nota fiscal) e últimas compras.
- **Listas:** em andamento e concluídas, criar lista com orçamento opcional.
- **Modo de compra:** saldo do orçamento em destaque (verde, âmbar perto do limite,
  vermelho quando estoura), marcar itens no carrinho, adicionar produto do catálogo
  ou cadastrar um novo, escolher **Unidade** ou **Peso (kg)**, quantidade com − e +
  (1 em 1 ou 0,1 kg), preço, remover, editar nome e orçamento, concluir compra,
  excluir lista ou compra concluída. Itens de compra concluída continuam editáveis.
- **Ler nota fiscal (NFC-e do Amazonas):** câmera lendo o QR code (com lanterna)
  ou link colado → conferência (nomes editáveis, remover item) → compra concluída.
  Nota já importada leva para a compra existente.
- **Produtos:** catálogo com busca e cadastro, inclusive lendo o código de barras
  (busca no catálogo e no Open Food Facts).
- **Menor preço:** o menor preço que você já pagou em cada produto, com loja, data,
  último preço e média, calculado a partir das compras concluídas.

### Ainda não faz (precisa de mudança no backend)

- Juntar a nota a uma lista que já existe (ou a uma lista só com orçamento) e
  comparar planejado × nota (telas B, C e 6 do design). Hoje a nota sempre vira
  uma compra nova, como na opção "Nenhuma" do design.

## Abrir no Android Studio

1. **File › Open** e escolha a pasta `corelist-android`.
2. Espere o Gradle sincronizar (a primeira vez baixa as dependências).
3. Use a configuração **app** e o botão Run.

Versões: AGP 9.3.2, Kotlin 2.2.10, Gradle 9.5.0 (wrapper), compileSdk 37, minSdk 26.

## Para onde o app aponta

| Build | API |
|---|---|
| `debug` | `http://10.0.2.2:8000/api` (o Django do PC, visto pelo emulador) |
| `release` | `https://mvp-comprafacil.dql3968zgbfv8.amplifyapp.com/api` |

O `http` só é liberado no debug (`src/debug/res/xml/network_security_config.xml`).
O debug instala como "CompraFácil (dev)" (`br.com.comprafacil.debug`) e pode ficar
no aparelho junto com o release.

### Rodar no emulador com o backend local

Em `corelist-backend/`:

```bash
python manage.py seed_demo
```

```bash
python manage.py runserver 0.0.0.0:8000
```

Depois rode o app no emulador e entre com a conta demo (`demo@comprafacil.test`;
a senha está na constante `DEMO_SENHA` de
`corelist-backend/apps/lists/management/commands/seed_demo.py`).

No emulador, a câmera traseira é uma cena virtual: para testar a nota, use
**Colar o link da nota**.

### Rodar num celular

- **Contra o site publicado:** instale o APK de release (abaixo).
- **Contra o Django do PC (mesma rede Wi-Fi):** gere o debug com o IP do PC:

```bash
./gradlew assembleDebug -Pcomprafacil.apiUrl=http://192.168.0.10:8000/api
```

  e rode o Django com `runserver 0.0.0.0:8000` (o IP precisa estar em
  `ALLOWED_HOSTS`).

Para instalar pelo cabo: ative **Opções do desenvolvedor › Depuração USB** no
celular e use Run no Android Studio, ou `adb install -r <apk>`.

### Testar em outro computador

1. Copie o projeto (o `.zip` gerado com `git archive`, ou clone o repositório
   quando ele existir no GitHub) e descompacte.
2. Instale o Android Studio e abra a pasta `corelist-android`. O `local.properties`
   (caminho do SDK) é criado sozinho na primeira abertura.
3. Sem o Django rodando nessa máquina, use a variante **release** (menu
   **Build › Select Build Variant › app: release**): ela usa o site publicado e
   roda direto pelo botão Run. Entre com uma conta que já exista no site.
4. Ou mantenha o debug e aponte para o site publicado, acrescentando no
   `gradle.properties` do projeto:
   `comprafacil.apiUrl=https://mvp-comprafacil.dql3968zgbfv8.amplifyapp.com/api`

## Gerar o APK

No Windows, sem `JAVA_HOME`, use o Java do Android Studio:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
```

```bash
./gradlew assembleRelease
```

| Comando | APK |
|---|---|
| `./gradlew assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew assembleRelease` | `app/build/outputs/apk/release/app-release.apk` |

Para um APK menor, só para um tipo de aparelho, acrescente
`-Pcomprafacil.abi=x86_64` (emulador) ou `-Pcomprafacil.abi=arm64-v8a` (celular).

O **release** é o que se entrega (aponta para o site publicado e passa pelo R8, que
o deixa bem menor). Para a apresentação ele é assinado com a chave de debug, o
que permite instalar em qualquer celular (é preciso permitir "instalar apps
desconhecidos"). **Antes de publicar na Play Store**, crie uma chave própria
(Build › Generate Signed App Bundle) e troque o `signingConfig` do release em
`app/build.gradle.kts`. Nunca coloque a chave nem a senha no repositório.

## Testes

```bash
./gradlew testDebugUnitTest
```

Testes JVM em `app/src/test`: leitura de respostas reais do backend (listas e
produtos da conta demo, em `src/test/resources`), totais iguais aos do servidor,
regras do orçamento iguais às do site, leitura de valores digitados, link da nota,
e a sessão com um servidor falso (renova uma vez e salva o refresh novo; refresh
recusado encerra a sessão com aviso; servidor fora do ar não derruba a sessão).

## Estrutura

```
app/src/main/java/br/com/comprafacil/
  CompraFacilApp.kt        objetos do app (sessão, API) e Sair
  MainActivity.kt
  data/Sessao.kt           tokens no DataStore
  data/api/                Retrofit, modelos, renovação do token, mensagens de erro
  ui/Navegacao.kt          rotas e barra inferior
  ui/theme/Tema.kt         cores e fontes do site (Poppins e Inter em res/font)
  ui/comum/                componentes, formatação de moeda/data, situação do orçamento
  ui/auth, inicio, listas, compra, nota, produtos   telas
```

Textos da interface em português do Brasil. Cores: destaque índigo `#4F46E5`;
verde, âmbar e vermelho só para a situação do orçamento.

Fontes: Poppins e Inter (SIL Open Font License), de
[Google Fonts](https://github.com/google/fonts).
