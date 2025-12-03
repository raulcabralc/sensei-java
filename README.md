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

* **Backend:** TypeScript com NestJS.
* **Inteligência Artificial:** Integração via API da OpenAI (Whisper para áudio, GPT para lógica de decisão e geração de texto).
* **Fluxo de Estados:** Implementação lógica de status de material (`WAITING`, `APPROVED`, `REJECTED`) para gerenciar o ciclo de vida das atividades.
* **Segurança:** JWT para proteção total dos dados.
* **Arquitetura:** Design Patterns aplicados (Strategy, Repository, DTOs) garantindo um código limpo e escalável.

## 🎯 Objetivo

O Sensei entrega **tempo** e **confiança**. O aluno recebe resumos instantâneos para estudar. O professor recebe propostas de atividades prontas, precisando apenas do seu "sim" para engajar a turma, eliminando horas de trabalho braçal.

---

🌪️ *Professor ninja usa **Sensei***
