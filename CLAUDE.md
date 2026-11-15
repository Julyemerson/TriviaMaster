# CLAUDE.md — Trivia Master Android (Projeto Final RUS0291)

App Android de quiz em **Java** que consome a API REST do **Open Trivia DB**.
Disciplina: Desenvolvimento de Software para Dispositivos Móveis — UFC Russas, 2026.2 — Prof. Filipe Fernandes.

**Prazos:** entrega por e-mail até **17/11/2026 (terça)** · apresentação no gabinete de 24 a 26/11.

---

## Como você (Claude Code) deve trabalhar neste projeto

- **Você pode programar, desde que explique o que está fazendo.** Antes de criar ou editar um arquivo, diga em poucas linhas o que vai fazer e por quê; depois, explique as partes importantes do código e **por que** ele foi escrito assim. Eu preciso entender tudo para a apresentação.
- **Código simples, nível de disciplina.** Use o que foi visto em aula: Activity, Intent, Layouts XML, Widgets, ContentProvider, Service, BroadcastReceiver, Threads/Handler/ExecutorService, REST. Nada de Retrofit, Room, Hilt, RxJava, ViewModel/LiveData ou Kotlin, a menos que eu peça.
- **Sempre sugira a forma mais limpa** do que estou fazendo (nomes claros, métodos curtos, sem duplicação, constantes em vez de strings soltas), sem introduzir bibliotecas ou padrões novos sem me perguntar.
- **Um passo por vez.** Ao final de cada passo, diga como testar no emulador antes de seguir.
- **Prepare para a apresentação:** na apresentação o professor faz perguntas individuais, pode pedir alterações ao vivo e não é permitido ler. Depois de cada componente importante, me faça 2 ou 3 perguntas curtas de "por que foi feito assim?" para eu treinar.
- Comentários em português, curtos, só onde ajudam.
- Se eu fizer algo que fere um requisito (rede/banco na main thread, acesso ao banco sem passar pelo ContentProvider, app fechando com exceção), **avise imediatamente**.

---

## Requisitos do enunciado (resumo — não remover)

**Obrigatório:** usar os **4 componentes**: Activity, ContentProvider, Service e BroadcastReceiver.

Funcionais:
1. Identificar o jogador pelo nome antes da partida.
2. Configurar partida: categoria, dificuldade e quantidade de questões.
3. Buscar questões **de múltipla escolha** (`type=multiple`) no Open Trivia DB com essas configurações.
4. Mostrar uma questão por vez, registrar a escolha e informar se acertou ou errou.
5. Temporizador por questão (entre 10 e 60 s); ao esgotar, avança sozinho.
6. Calcular a pontuação (fórmula livre, justificada) e mostrar resumo completo no final.
7. Salvar localmente o jogador e o resultado de cada partida.
8. Tela de histórico de partidas, organizada.
9. Sem conexão → jogar com questões salvas localmente (quantidade e estratégia justificadas).
10. Mensagens adequadas de erro; o app nunca pode fechar inesperadamente.

Técnicos:
- GET em `https://opentdb.com/api.php` com parâmetros dinâmicos (amount, category, difficulty).
- Parse do JSON + **decodificação das entidades HTML** (`&quot;`, `&#039;`…).
- Rede e persistência **fora da UI Thread** (Executors/Threads); **acesso ao banco via ContentProvider**.
- Tratar timeout, API indisponível, sem conexão e `response_code` ≠ 0.

---

## Decisões de projeto (já tomadas — justificativas para a apresentação)

| Decisão | Justificativa |
|---|---|
| Java + Views/XML, `minSdk 24` | Conteúdo da disciplina; simples de explicar. |
| `HttpURLConnection` + `org.json` | Nativos do Android, sem dependência externa; mostram o REST "na mão". |
| `ExecutorService` + `Handler(Looper.getMainLooper())` | Mecanismo visto em Paralelismo; tira I/O da main thread e volta para a UI de forma segura. |
| SQLite (`SQLiteOpenHelper`) exposto por **um ContentProvider** | Requisito; centraliza todo acesso a dados atrás de URIs. |
| **Service** que baixa as questões | Rede é trabalho de background desacoplado da tela; se a Activity girar ou fechar, o download não depende dela. |
| **BroadcastReceiver** recebe o resultado do Service | Service e Activity não se conhecem diretamente; o Service só avisa "pronto" ou "falhou". |
| Lista de categorias fixa em `res/values/arrays.xml` | Funciona offline e evita uma requisição extra (a API limita ~1 requisição a cada 5 s por IP). |
| Tela de quiz travada em retrato | Evita perder o timer e o progresso na rotação; mantém o código simples. |
| Cache: toda questão baixada fica salva; consulta offline filtra por categoria/dificuldade e, se faltar, completa com qualquer questão salva | Garante partida offline sempre que houver algo no cache. Limite de ~300 questões (apaga as mais antigas). |

**Pontuação (proposta, pode ajustar):** acerto vale `10 × multiplicador` (fácil 1, médio 2, difícil 3) + bônus de tempo `segundosRestantes / 2`. Erro ou tempo esgotado = 0. Justificativa: recompensa dificuldade e rapidez sem punir quem erra.

**Tempo por questão:** 20 s (constante `QUESTION_TIME_SECONDS`).

---

## Arquitetura

```
com.<seunome>.triviamaster
├── model/
│   ├── Question.java            pergunta, categoria, dificuldade, correta, alternativas embaralhadas
│   └── MatchResult.java         jogador, data, categoria, dificuldade, acertos, total, pontuação
├── data/
│   ├── TriviaContract.java      AUTHORITY, URIs e nomes de colunas (tabelas: players, matches, questions)
│   ├── TriviaDbHelper.java      SQLiteOpenHelper (CREATE TABLE)
│   ├── TriviaProvider.java      ContentProvider (query/insert/delete) ← ÚNICO acesso ao banco
│   └── OpenTriviaClient.java    HTTP GET + parse JSON + decodificação HTML + checagem do response_code
├── service/
│   └── QuestionFetchService.java  Service: chama OpenTriviaClient num Executor, salva no Provider, envia broadcast
├── receiver/
│   └── QuestionsReceiver.java   (ou classe interna) recebe ACTION_QUESTIONS_READY / ACTION_QUESTIONS_FAILED
└── ui/
    ├── MainActivity.java        nome do jogador + configuração da partida + botão Histórico
    ├── QuizActivity.java        questão, alternativas, CountDownTimer, feedback certo/errado, placar
    ├── ResultActivity.java      resumo completo; salva o resultado (via Provider, fora da main thread)
    └── HistoryActivity.java     lista de partidas (ListView/RecyclerView), mais recentes primeiro
```

### Fluxo de uma partida
1. `MainActivity` valida o nome e a configuração → mostra ProgressBar → `startService(QuestionFetchService)` com os parâmetros.
2. `QuestionFetchService` (num `ExecutorService`) chama a API.
   - **Sucesso:** salva as questões em `questions` via `ContentResolver` → envia broadcast `READY` com a lista (ou ids).
   - **Falha** (sem rede, timeout, `response_code` ≠ 0): busca questões do cache via Provider.
     - Achou o suficiente → broadcast `READY` com `offline=true`.
     - Não achou → broadcast `FAILED` com a mensagem de erro.
   - `stopSelf()` no fim.
3. O receiver (registrado em `onStart`/desregistrado em `onStop` da MainActivity) abre a `QuizActivity` ou mostra o erro (Toast/AlertDialog).
4. `QuizActivity` → ao terminar → `ResultActivity` → salva jogador + partida via Provider.
5. `HistoryActivity` lê `matches` via Provider (num Executor) e exibe.

### Detalhes técnicos importantes
- Broadcast interno: `intent.setPackage(getPackageName())` ao enviar; `ContextCompat.registerReceiver(..., ContextCompat.RECEIVER_NOT_EXPORTED)` ao registrar. (`LocalBroadcastManager` está depreciado.)
- Declarar no Manifest: `INTERNET`, `<service>`, `<provider android:authorities=... android:exported="false">`.
- `HttpURLConnection`: `setConnectTimeout(8000)`, `setReadTimeout(8000)`; tratar `SocketTimeoutException`, `UnknownHostException`, `IOException`, `JSONException`, HTTP ≠ 200.
- `response_code`: 0 ok · 1 sem questões suficientes · 2 parâmetro inválido · 3/4 token · 5 rate limit (esperar 5 s).
- Decodificar: `Html.fromHtml(texto, Html.FROM_HTML_MODE_LEGACY).toString()`.
- Embaralhar `correct_answer` + `incorrect_answers` com `Collections.shuffle`.
- `CountDownTimer`: cancelar em `onDestroy` e ao responder; ao esgotar, conta como erro e avança.
- Encerrar `ExecutorService` em `onDestroy` (Service e Activities).
- Textos em `strings.xml`; cores de certo/errado em `colors.xml`.

---

## Fases e cronograma

### Fase 1 — Interface com dados mockados (domingo 15/11)
1. Projeto "Empty Views Activity" (Java).
2. `Question` + uma classe `MockQuestions` com ~5 questões fixas (inclusive com `&quot;` para testar decodificação depois).
3. `MainActivity`: EditText nome, Spinner categoria, Spinner dificuldade, Spinner/NumberPicker quantidade (5, 10, 15, 20), botões Iniciar e Histórico. Validar nome vazio.
4. `QuizActivity`: pergunta, 4 botões, "3/10", placar, timer com ProgressBar. Feedback verde/vermelho + avanço após ~1 s.
5. `ResultActivity` (resumo) e `HistoryActivity` (lista mockada).

### Fase 2 — Persistência com ContentProvider (domingo 15/11)
1. `TriviaContract`, `TriviaDbHelper`, `TriviaProvider`; registrar no Manifest.
2. `ResultActivity` grava jogador + partida; `HistoryActivity` passa a ler do Provider. Tudo num Executor.

### Fase 3 — REST + Service + BroadcastReceiver + offline (segunda 16/11)
1. `OpenTriviaClient` (testar primeiro isolado, logando o resultado).
2. `QuestionFetchService` + broadcast + receiver na MainActivity; trocar o mock pelos dados reais.
3. Cache em `questions` + fallback offline + todas as mensagens de erro.
4. Testar: modo avião, quantidade alta numa categoria pequena (response_code 1), dois cliques rápidos (response_code 5).

### Fase 4 — Entrega (terça 17/11)
- [ ] Testar todo o checklist abaixo
- [ ] Vídeo no YouTube (até 20 min): funcionamento + organização do código + justificativas
- [ ] Zip **sem** `build/`, `.gradle/`, `.idea/`, `.apk`, `.class`, `local.properties` (só `.zip`, nada de `.rar`/`.7z`)
- [ ] E-mail para filipe.fernandes@ufc.br — título: `[2026.2] 2ª Atividade Avaliativa de Desenvolvimento de Software para Dispositivos Móveis` — corpo: nomes + matrículas, zip, link do vídeo

## Checklist final
- [ ] Os 4 componentes estão em uso real e eu sei explicar cada um
- [ ] Nome obrigatório; categoria, dificuldade e quantidade funcionando
- [ ] Nenhum `&quot;`/`&#039;` aparecendo nas questões
- [ ] Timer avança sozinho e conta como erro
- [ ] Resultado salvo e aparece no histórico após reabrir o app
- [ ] Modo avião: joga com cache; cache vazio: mensagem clara, sem crash
- [ ] response_code 1 e 5 com mensagens amigáveis
- [ ] Nenhuma rede/banco na main thread
- [ ] Todos os integrantes sabem explicar qualquer parte (perguntas são individuais)
