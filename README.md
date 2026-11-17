## 🥋 SENSEI (Java)

🔗 Projeto original: [github.com/raulcabralc/sensei](https://github.com/raulcabralc/sensei)

---

# 🥋 Sensei: O Copiloto Inteligente para Professores

> **"A IA gera, você comanda. O ensino evolui."**

## 🚩 O Problema: A Carga Invisível e as Promessas Esquecidas

Todo professor conhece o caos pós-aula. Durante a explicação, a dinâmica é intensa: dúvidas surgem, debates acontecem e compromissos são firmados (*"Vou deixar 3 exercícios sobre isso para vocês"* ou *"Na próxima aula cobrem essa atividade"*).

Porém, ao fechar a porta da sala, a realidade bate. O cansaço mental e a correria apagam esses detalhes. O professor perde tempo tentando lembrar exatamente o que prometeu ou gasta horas criando atividades do zero. O resultado? Oportunidades de fixação perdidas e um educador sobrecarregado operacionalmente.

## 🚀 A Solução

O **Sensei** não é apenas um gravador; é um **Assistente Pedagógico Ativo**. Ele automatiza a documentação e *propõe* materiais, mas mantém o professor sempre no comando da qualidade pedagógica.

A premissa é o equilíbrio perfeito: **Agilidade na documentação, Curadoria na avaliação.**

### ✨ O Fluxo Inteligente

O Sensei trabalha em dois tempos para garantir eficiência e segurança:

1.  🎙️ **Escuta Ativa:** O professor grava a aula via app. A IA (Whisper) transcreve tudo com precisão.
2.  📝 **Resumo Imediato (Automático):** O sistema identifica os pontos-chave da aula e posta **automaticamente** um resumo estruturado. O aluno sai da aula já com o material de estudo garantido.
3.  ✅ **Geração de Tarefas (Sob Aprovação):**
    * A IA analisa o contexto: *"O professor pediu uma atividade sobre esse tema?"*
    * Se sim, o Sensei **cria um rascunho** da atividade (perguntas, exercícios ou tópicos).
    * Essa atividade **NÃO** é postada automaticamente. Ela fica com status `AGUARDANDO APROVAÇÃO`.
4.  👨‍🏫 **O Toque Humano:** O professor revisa a sugestão da IA. Ele pode ajustar, aprovar ou descartar. Isso evita que a IA crie atividades alucinadas, repetitivas ou que não condizem com a pedagogia da turma.

---

## 🛡️ Por que a "Aprovação" muda o jogo?

Muitas ferramentas de IA falham por tentar substituir o professor. O Sensei acerta por **empoderá-lo**.

* **Segurança Pedagógica:** A IA pode não entender uma ironia ou um contexto específico. A etapa de aprovação garante que nenhum material incorreto chegue aos alunos.
* **Filtro de Relevância:** Às vezes a IA sugere 10 questões, mas o professor sabe que a turma só precisa de 2 bem feitas. O professor tem a palavra final.
* **Zero Alucinação:** Se a IA inventar um tópico que não foi dado, o professor barra o envio com um clique.

---

## 🛠️ Bastidores Técnicos (Tech Stack)

Uma aplicação robusta construída com as melhores práticas de Engenharia de Software:

* **Backend:** Java 17 com Spring Boot 3.5 (Spring Web, Spring Security, Spring Data MongoDB). *O projeto original foi escrito em TypeScript com NestJS.*
* **Banco de dados:** MongoDB.
* **Inteligência Artificial:** Integração via API da OpenAI (Whisper para áudio, GPT para lógica de decisão e geração de texto).
* **Fluxo de Estados:** Implementação lógica de status de material (`WAITING`, `APPROVED`) para gerenciar o ciclo de vida das atividades.
* **Segurança:** Spring Security com autenticação JWT (stateless) e senhas com BCrypt.
* **Arquitetura:** Camadas (controller, service, repository) com Design Patterns aplicados (Repository, DTOs).

## ▶️ Executando localmente

### 1. Pré-requisitos

* **JDK 17.**
* **Maven** — o IntelliJ IDEA já traz um Maven embutido (o projeto não possui Maven Wrapper).
* **MongoDB** acessível. Para desenvolvimento, um MongoDB local na porta `27017` basta; por exemplo, via Docker:
  `docker run -d --name sensei-mongo -p 27017:27017 mongo:7`
* **Chave da API OpenAI** — necessária apenas para os endpoints `/ai/**` (veja o item 8).

### 2. Importando o projeto no IntelliJ IDEA

1. `File > Open...` e selecione a pasta do projeto (a que contém o `pom.xml`).
2. Escolha abrir como projeto Maven e aguarde a importação das dependências.
3. Em `File > Project Structure > Project`, defina o SDK como **Java 17**.

### 3. Variáveis de ambiente

Todas as configurações sensíveis vêm de variáveis de ambiente (arquivos em `src/main/resources/application*.yml`). A lista completa está em [`.env.example`](.env.example).

| Variável | Obrigatória | Observação |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Recomendada | `dev` para desenvolvimento local, `prod` para produção. |
| `JWT_SECRET` | **Sim, em todos os perfis** | Mínimo de **32 bytes**. Sem valor padrão. |
| `JWT_EXPIRATION_MS` | Não | Padrão `86400000` (24 h). Máximo `2147483647`. |
| `MONGODB_URI` | Sim, exceto no `dev` | No `dev`, o padrão é `mongodb://localhost:27017/sensei`. |
| `OPENAI_API_KEY` | Sim, exceto no `dev` | No `dev` pode ficar vazia (os endpoints `/ai/**` falham). |

> ⚠️ O Spring Boot **não** lê arquivos `.env` automaticamente. O `.env.example` serve como referência; os valores devem ser informados na Run Configuration (abaixo) ou no shell. Nunca versione um `.env` real (ele já está no `.gitignore`).

Para gerar um `JWT_SECRET` aleatório:

```bash
# Git Bash / Linux / macOS
openssl rand -base64 48
```

```powershell
# PowerShell
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

### 4. Run Configuration e perfil `dev`

1. `Run > Edit Configurations... > + >` **Spring Boot** (IntelliJ Ultimate) ou **Application** (Community).
2. **Main class:** `com.sensei.app.SenseiApplication`.
3. **Environment variables:** clique no ícone ao lado do campo e cole as variáveis no formato `NOME=valor`, uma por linha, por exemplo:
   ```
   SPRING_PROFILES_ACTIVE=dev
   JWT_SECRET=<valor gerado no passo anterior>
   OPENAI_API_KEY=<sua chave, ou deixe vazio no dev>
   ```
4. **Perfil:** no IntelliJ Ultimate é possível usar o campo **Active profiles** (`dev`); em qualquer edição, `SPRING_PROFILES_ACTIVE=dev` nas variáveis ou `-Dspring.profiles.active=dev` em *VM options* têm o mesmo efeito.

Sem perfil ativo, a aplicação usa apenas `application.yml` e exige `MONGODB_URI`, `JWT_SECRET` e `OPENAI_API_KEY`.

### 5. Executando

Execute a configuração criada (▶) ou clique no ícone de execução ao lado do método `main` de `SenseiApplication`.

Pelo terminal (com as variáveis já exportadas e o Maven instalado):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 6. Identificando uma inicialização bem-sucedida

No console, procure por:

* `The following 1 profile is active: "dev"`
* `Tomcat started on port 8080 (http)`
* `Started SenseiApplication in ... seconds`
* `Monitor thread successfully connected to server` — confirma a conexão com o MongoDB.

O driver do MongoDB conecta de forma assíncrona: a aplicação **inicia mesmo com o MongoDB desligado**, mas registra `Exception in monitor thread while connecting to server ...` e todas as operações com dados (inclusive o login) falham.

Teste rápido, sem token: `curl -i http://localhost:8080/user/` deve responder **401** em JSON, o que confirma que a cadeia de segurança está ativa.

Erros comuns na inicialização:

| Mensagem | Causa |
|---|---|
| `Could not resolve placeholder 'JWT_SECRET'` | `JWT_SECRET` não definida. |
| `Could not resolve placeholder 'OPENAI_API_KEY'` | Chave ausente fora do perfil `dev` (verifique se o perfil está ativo). |
| Connection string inválida contendo `${MONGODB_URI}` | `MONGODB_URI` ausente fora do perfil `dev`. |
| `WeakKeyException` no primeiro login | `JWT_SECRET` com menos de 32 bytes. |

### 7. Serviços externos necessários

* **MongoDB:** necessário para qualquer operação com dados, incluindo login.
* **OpenAI:** necessária apenas para `POST /ai/transcribe` (Whisper, arquivos de até **25 MB**) e `POST /ai/summarize` (GPT).

### 8. Chave OpenAI durante o desenvolvimento

* No perfil `dev`, `OPENAI_API_KEY` pode ficar vazia: a aplicação inicia normalmente e apenas os endpoints `/ai/**` retornam erro.
* Cada chamada a esses endpoints é cobrada na sua conta OpenAI.
* Use uma chave pessoal, informe-a apenas como variável de ambiente e nunca a escreva em arquivos do repositório.

> ℹ️ Estado atual: o cadastro de usuários (`POST /user/create`) exige um token JWT, portanto o primeiro login depende de um usuário já existente no banco. Isso será tratado na etapa de ajustes do Spring Security.

## 🎯 Objetivo

O Sensei entrega **tempo** e **confiança**. O aluno recebe resumos instantâneos para estudar. O professor recebe propostas de atividades prontas, precisando apenas do seu "sim" para engajar a turma, eliminando horas de trabalho braçal.

---

🌪️ *Professor ninja usa **Sensei***
