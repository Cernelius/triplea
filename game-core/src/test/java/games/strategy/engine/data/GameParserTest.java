package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.util.Tuple;

final class GameParserTest {
  @Nested
  final class DecapitalizeTest {
    @Test
    void shouldReturnValueWithFirstCharacterDecapitalized() {
      Arrays.asList(
          Tuple.of("", ""),
          Tuple.of("N", "n"),
          Tuple.of("name", "name"),
          Tuple.of("Name", "name"),
          Tuple.of("NAME", "nAME"))
          .forEach(t -> {
            final String value = t.getFirst();
            final String decapitalizedValue = t.getSecond();
            assertThat(
                String.format("wrong decapitalization for '%s'", value),
                GameParser.decapitalize(value),
                is(decapitalizedValue));
          });
    }
  }

  @Nested
  final class ParsePlayersFromIsDisplayedForTest {
    private final GameData gameData = new GameData();
    private final GameParser gameParser = new GameParser(gameData, "mapName");

    private String joinPlayerNames(final PlayerID... playerIds) {
      return Arrays.stream(playerIds)
          .map(PlayerID::getName)
          .collect(Collectors.joining(":"));
    }

    @Test
    void shouldReturnPlayersWhenAllPlayersExist() throws Exception {
      final PlayerID player1 = new PlayerID("player1Name", gameData);
      gameData.getPlayerList().addPlayerId(player1);
      final PlayerID player2 = new PlayerID("player2Name", gameData);
      gameData.getPlayerList().addPlayerId(player2);

      assertThat(
          gameParser.parsePlayersFromIsDisplayedFor(joinPlayerNames(player1)),
          contains(player1));
      assertThat(
          gameParser.parsePlayersFromIsDisplayedFor(joinPlayerNames(player1, player2)),
          contains(player1, player2));
    }

    @Test
    void shouldThrowExceptionWhenAnyPlayerDoesNotExist() {
      final PlayerID player = new PlayerID("unknownPlayerName", gameData);

      final Exception e = assertThrows(
          GameParseException.class,
          () -> gameParser.parsePlayersFromIsDisplayedFor(joinPlayerNames(player)));
      assertThat(e.getMessage(), containsString("Parse resources could not find player: " + player.getName()));
    }
  }
}
