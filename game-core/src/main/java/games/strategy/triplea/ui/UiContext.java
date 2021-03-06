package games.strategy.triplea.ui;

import java.awt.Cursor;
import java.awt.Window;
import java.util.concurrent.CountDownLatch;

import javax.swing.JLabel;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.triplea.image.DiceImageFactory;
import games.strategy.triplea.image.FlagIconImageFactory;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.PuImageFactory;
import games.strategy.triplea.image.ResourceImageFactory;
import games.strategy.triplea.image.TerritoryEffectImageFactory;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.image.UnitIconImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.ui.screen.drawable.IDrawable.OptionalExtraBorderLevel;
import games.strategy.util.CountDownLatchHandler;

/**
 * Provides a context for UI-dependent operations to execute without requiring specific knowledge of the underlying UI
 * implementation (e.g. headed vs. headless).
 */
public interface UiContext {
  Cursor getCursor();

  double getScale();

  void setScale(double scale);

  void setDefaultMapDir(GameData data);

  void setMapDir(GameData data, String mapDir);

  MapData getMapData();

  TileImageFactory getTileImageFactory();

  UnitImageFactory getUnitImageFactory();

  /**
   * Indicates the damaged or undamaged version of a unit image should be used.
   *
   * @see UiContext#createUnitImageJLabel(UnitType, PlayerID, UnitDamage, UnitEnable)
   */
  enum UnitDamage {
    DAMAGED, NOT_DAMAGED
  }

  /**
   * Indicates the enabled or disabled version of a unit image should be used.
   *
   * @see UiContext#createUnitImageJLabel(UnitType, PlayerID, UnitDamage, UnitEnable)
   */
  enum UnitEnable {
    DISABLED, ENABLED
  }

  default JLabel createUnitImageJLabel(final UnitType type, final PlayerID player) {
    return createUnitImageJLabel(type, player, UnitDamage.NOT_DAMAGED, UnitEnable.ENABLED);
  }

  JLabel createUnitImageJLabel(final UnitType type, final PlayerID player,
      final UnitDamage damaged,
      final UnitEnable disabled);

  ResourceImageFactory getResourceImageFactory();

  TerritoryEffectImageFactory getTerritoryEffectImageFactory();

  MapImage getMapImage();

  UnitIconImageFactory getUnitIconImageFactory();

  FlagIconImageFactory getFlagImageFactory();

  PuImageFactory getPuImageFactory();

  DiceImageFactory getDiceImageFactory();

  void removeActive(Active actor);

  void addActive(Active actor);

  void addShutdownLatch(CountDownLatch latch);

  void removeShutdownLatch(CountDownLatch latch);

  CountDownLatchHandler getCountDownLatchHandler();

  void addShutdownWindow(Window window);

  void removeShutdownWindow(Window window);

  boolean isShutDown();

  void shutDown();

  boolean getShowUnits();

  void setShowUnits(boolean showUnits);

  OptionalExtraBorderLevel getDrawTerritoryBordersAgain();

  void setDrawTerritoryBordersAgain(OptionalExtraBorderLevel level);

  void resetDrawTerritoryBordersAgain();

  void setDrawTerritoryBordersAgainToMedium();

  void setShowTerritoryEffects(boolean showTerritoryEffects);

  boolean getShowTerritoryEffects();

  boolean getShowMapOnly();

  void setShowMapOnly(boolean showMapOnly);

  boolean getLockMap();

  void setLockMap(boolean lockMap);

  boolean getShowEndOfTurnReport();

  void setShowEndOfTurnReport(boolean value);

  boolean getShowTriggeredNotifications();

  void setShowTriggeredNotifications(boolean value);

  boolean getShowTriggerChanceSuccessful();

  void setShowTriggerChanceSuccessful(boolean value);

  boolean getShowTriggerChanceFailure();

  void setShowTriggerChanceFailure(boolean value);

  LocalPlayers getLocalPlayers();

  void setLocalPlayers(LocalPlayers players);

  void setUnitScaleFactor(double scaleFactor);
}
