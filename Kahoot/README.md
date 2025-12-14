Dentro da pasta Kahoot executar os seguintes passos:

1 passo, abrir 5 terminais (1 server, 4 players)

2 passo, para tornar o scripts executavies
 - chmod +x build.sh run_server.sh run_client.sh

3 passo, compilar
 - ./build.sh

4 passo, lançar servidor
 - ./run_server.sh

5 passo, o servidor vai pedir "new <numEquipas> <numJogadores>"
 - new 2 2

6 passo, lançar clientes nos restantes terminais, de acordo com o numero de jogadores que se quer (nesta caso, duas equipas, com dois jogadores cada)
 - ./run_client.sh localhost 9090 JOGO1 A Joao
 - ./run_client.sh localhost 9090 JOGO1 A Joana
 - ./run_client.sh localhost 9090 JOGO1 B Maria
 - ./run_client.sh localhost 9090 JOGO1 B Manuel

de acordo com cada cliente inicializado, vão aparecer as seguintes GUI, e é so jogar ate o jogo terminar.


Ciclo do jogo:
Cada pergunta segue o seguinte fluxo:

1. GameMaster escolhe a pergunta
2. Envia NEW_QUESTION para todos os jogadores
3. Cria mecanismo (modifiedCountdownLatch perguntas individuais, simpleBarrier pergunta de equipa)
4. Players enviam ANWSER
5. GameMaster aguarda 30sg ou ate todos responderem
6. Calcula a pontução (individual os primeiros 2 tem o dobre, equipa todos corretos = dobro, senão pontuação base)
7. Envia SCORE_UPDATE
8. Passa a proxima pergunta
9. Na ultima, GAME_OVER

THREADS usadas:
1. ServerMain (principal do servidor) - aceita ligações e cria jogos
2. ClientHandler (server, 1 por jogador) - receb LOGIN/AWNSER, comunica com cliente
3. GameMaster (server) - controla o ciclo do jogo
4. ConsumerThread (server) - Lê respostas da queue e passa ao GameMaster
5. ListenThread (client) - recebe mendagens do servidor
6. Swing EDT - atuliza interfade grafica


