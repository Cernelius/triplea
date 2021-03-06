package games.strategy.triplea.attachments;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.formatter.MyFormatter;

/**
 * This class is designed to hold common code for holding "conditions". Any attachment that can hold conditions (ie:
 * RulesAttachments),
 * should extend this instead of DefaultAttachment.
 */
public abstract class AbstractConditionsAttachment extends DefaultAttachment implements ICondition {
  private static final long serialVersionUID = -9008441256118867078L;
  private static final Splitter HYPHEN_SPLITTER = Splitter.on('-');
  protected static final String AND = "AND";
  protected static final String OR = "OR";
  protected static final String DEFAULT_CHANCE = "1:1";
  protected static final String CHANCE = "chance";
  public static final String TRIGGER_CHANCE_SUCCESSFUL = "Trigger Rolling is a Success!";
  public static final String TRIGGER_CHANCE_FAILURE = "Trigger Rolling is a Failure!";

  // list of conditions that this condition can
  protected List<RulesAttachment> m_conditions = new ArrayList<>();
  // contain
  // m_conditionType modifies the relationship of m_conditions
  protected String m_conditionType = AND;
  // will logically negate the entire condition, including contained conditions
  protected boolean m_invert = false;
  // chance (x out of y) that this action is successful when attempted, default = 1:1 = always
  protected String m_chance = DEFAULT_CHANCE;
  // successful
  // if chance fails, we should increment the chance by x
  protected int m_chanceIncrementOnFailure = 0;
  // if chance succeeds, we should decrement the chance by x
  protected int m_chanceDecrementOnSuccess = 0;

  protected AbstractConditionsAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  protected void setConditions(final String conditions) throws GameParseException {
    if (m_conditions == null) {
      m_conditions = new ArrayList<>();
    }
    final Collection<PlayerID> playerIDs = getData().getPlayerList().getPlayers();
    for (final String subString : splitOnColon(conditions)) {
      m_conditions.add(playerIDs.stream()
          .map(p -> p.getAttachment(subString))
          .map(RulesAttachment.class::cast)
          .filter(Objects::nonNull)
          .findAny()
          .orElseThrow(() -> new GameParseException("Could not find rule. name:" + subString + thisErrorMsg())));
    }
  }

  private void setConditions(final List<RulesAttachment> value) {
    m_conditions = value;
  }

  @Override
  public List<RulesAttachment> getConditions() {
    return m_conditions;
  }

  protected void resetConditions() {
    m_conditions = new ArrayList<>();
  }

  private void setInvert(final boolean s) {
    m_invert = s;
  }

  private boolean getInvert() {
    return m_invert;
  }

  @VisibleForTesting
  void setConditionType(final String value) throws GameParseException {
    final String uppercaseValue = value.toUpperCase();
    if (uppercaseValue.matches("AND|X?OR|\\d+(?:-\\d+)?")) {
      final String[] split = splitOnHyphen(uppercaseValue);
      if (split.length != 2 || Integer.parseInt(split[1]) > Integer.parseInt(split[0])) {
        m_conditionType = uppercaseValue;
        return;
      }
    }
    throw new GameParseException("conditionType must be equal to 'AND' or 'OR' or 'XOR' or 'y' or 'y-z' where Y "
        + "and Z are valid positive integers and Z is greater than Y" + thisErrorMsg());
  }

  protected static String[] splitOnHyphen(final String value) {
    checkNotNull(value);

    return Iterables.toArray(HYPHEN_SPLITTER.split(value), String.class);
  }

  private String getConditionType() {
    return m_conditionType;
  }

  private void resetConditionType() {
    m_conditionType = AND;
  }

  /**
   * Accounts for Invert and conditionType. Only use if testedConditions has already been filled and this conditions has
   * been tested.
   */
  @Override
  public boolean isSatisfied(final Map<ICondition, Boolean> testedConditions) {
    return isSatisfied(testedConditions, null);
  }

  /**
   * Accounts for Invert and conditionType. IDelegateBridge is not used so can be null, this is because we have already
   * tested all the
   * conditions.
   */
  @Override
  public boolean isSatisfied(
      final Map<ICondition, Boolean> testedConditions,
      final IDelegateBridge delegateBridge) {
    if (testedConditions == null) {
      throw new IllegalStateException("testedCondititions cannot be null");
    }
    if (testedConditions.containsKey(this)) {
      return testedConditions.get(this);
    }
    return areConditionsMet(new ArrayList<>(getConditions()), testedConditions, getConditionType()) != getInvert();
  }

  /**
   * Anything that implements ICondition (currently RulesAttachment, TriggerAttachment, and PoliticalActionAttachment)
   * can use this to get all the conditions that must be checked for the object to be 'satisfied'. <br>
   * Since anything implementing ICondition can contain other ICondition, this must recursively search through all
   * conditions and contained
   * conditions to get the final list.
   */
  public static HashSet<ICondition> getAllConditionsRecursive(final HashSet<ICondition> startingListOfConditions,
      final HashSet<ICondition> initialAllConditionsNeededSoFar) {
    final HashSet<ICondition> allConditionsNeededSoFar = Optional.ofNullable(initialAllConditionsNeededSoFar)
        .orElseGet(HashSet::new);
    allConditionsNeededSoFar.addAll(startingListOfConditions);
    for (final ICondition condition : startingListOfConditions) {
      for (final ICondition subCondition : condition.getConditions()) {
        if (!allConditionsNeededSoFar.contains(subCondition)) {
          allConditionsNeededSoFar.addAll(getAllConditionsRecursive(
              new HashSet<>(Collections.singleton(subCondition)), allConditionsNeededSoFar));
        }
      }
    }
    return allConditionsNeededSoFar;
  }

  /**
   * Takes the list of ICondition that getAllConditionsRecursive generates, and tests each of them, mapping them one by
   * one to their boolean
   * value.
   */
  public static HashMap<ICondition, Boolean> testAllConditionsRecursive(final HashSet<ICondition> rules,
      final HashMap<ICondition, Boolean> initialAllConditionsTestedSoFar, final IDelegateBridge delegateBridge) {
    final HashMap<ICondition, Boolean> allConditionsTestedSoFar = Optional.ofNullable(initialAllConditionsTestedSoFar)
        .orElseGet(HashMap::new);
    for (final ICondition c : rules) {
      if (!allConditionsTestedSoFar.containsKey(c)) {
        testAllConditionsRecursive(new HashSet<>(c.getConditions()), allConditionsTestedSoFar, delegateBridge);
        allConditionsTestedSoFar.put(c, c.isSatisfied(allConditionsTestedSoFar, delegateBridge));
      }
    }
    return allConditionsTestedSoFar;
  }

  /**
   * Accounts for all listed rules, according to the conditionType.
   * Takes the mapped conditions generated by testAllConditions and uses it to know which conditions are true and which
   * are false. There is
   * no testing of conditions done in this method.
   */
  public static boolean areConditionsMet(final List<ICondition> rulesToTest,
      final Map<ICondition, Boolean> testedConditions, final String conditionType) {
    boolean met = false;
    if (conditionType.equals(AND)) {
      for (final ICondition c : rulesToTest) {
        met = testedConditions.get(c);
        if (!met) {
          break;
        }
      }
    } else if (conditionType.equals(OR)) {
      for (final ICondition c : rulesToTest) {
        met = testedConditions.get(c);
        if (met) {
          break;
        }
      }
    } else {
      final String[] nums = splitOnHyphen(conditionType);
      if (nums.length == 1) {
        final int start = Integer.parseInt(nums[0]);
        int count = 0;
        for (final ICondition c : rulesToTest) {
          met = testedConditions.get(c);
          if (met) {
            count++;
          }
        }
        met = (count == start);
      } else if (nums.length == 2) {
        final int start = Integer.parseInt(nums[0]);
        final int end = Integer.parseInt(nums[1]);
        int count = 0;
        for (final ICondition c : rulesToTest) {
          met = testedConditions.get(c);
          if (met) {
            count++;
          }
        }
        met = (count >= start && count <= end);
      }
    }
    return met;
  }

  protected void setChance(final String chance) throws GameParseException {
    final String[] s = splitOnColon(chance);
    try {
      final int i = getInt(s[0]);
      final int j = getInt(s[1]);
      if (i > j || i < 0 || i > 120 || j > 120) {
        throw new GameParseException(
            "chance should have a format of \"x:y\" where x is <= y and both x and y are >=0 and <=120"
                + thisErrorMsg());
      }
    } catch (final IllegalArgumentException iae) {
      throw new GameParseException(
          "Invalid chance declaration: " + chance + " format: \"1:10\" for 10% chance" + thisErrorMsg());
    }
    m_chance = chance;
  }

  /**
   * Returns the number you need to roll to get the action to succeed format "1:10" for 10% chance.
   */
  private String getChance() {
    return m_chance;
  }

  private void resetChance() {
    m_chance = DEFAULT_CHANCE;
  }

  public int getChanceToHit() {
    return getInt(splitOnColon(getChance())[0]);
  }

  public int getChanceDiceSides() {
    return getInt(splitOnColon(getChance())[1]);
  }

  private void setChanceIncrementOnFailure(final int value) {
    m_chanceIncrementOnFailure = value;
  }

  public int getChanceIncrementOnFailure() {
    return m_chanceIncrementOnFailure;
  }

  private void setChanceDecrementOnSuccess(final int value) {
    m_chanceDecrementOnSuccess = value;
  }

  public int getChanceDecrementOnSuccess() {
    return m_chanceDecrementOnSuccess;
  }

  public void changeChanceDecrementOrIncrementOnSuccessOrFailure(final IDelegateBridge delegateBridge,
      final boolean success,
      final boolean historyChild) {
    if (success) {
      if (m_chanceDecrementOnSuccess == 0) {
        return;
      }
      final int oldToHit = getChanceToHit();
      final int diceSides = getChanceDiceSides();
      final int newToHit = Math.max(0, Math.min(diceSides, (oldToHit - m_chanceDecrementOnSuccess)));
      if (newToHit == oldToHit) {
        return;
      }
      final String newChance = newToHit + ":" + diceSides;
      delegateBridge.getHistoryWriter()
          .startEvent("Success changes chance for " + MyFormatter.attachmentNameToText(getName()) + " to " + newChance);
      delegateBridge.addChange(ChangeFactory.attachmentPropertyChange(this, newChance, CHANCE));
    } else {
      if (m_chanceIncrementOnFailure == 0) {
        return;
      }
      final int oldToHit = getChanceToHit();
      final int diceSides = getChanceDiceSides();
      final int newToHit = Math.max(0, Math.min(diceSides, (oldToHit + m_chanceIncrementOnFailure)));
      if (newToHit == oldToHit) {
        return;
      }
      final String newChance = newToHit + ":" + diceSides;
      if (historyChild) {
        delegateBridge.getHistoryWriter().addChildToEvent(
            "Failure changes chance for " + MyFormatter.attachmentNameToText(getName()) + " to " + newChance);
      } else {
        delegateBridge.getHistoryWriter().startEvent(
            "Failure changes chance for " + MyFormatter.attachmentNameToText(getName()) + " to " + newChance);
      }
      delegateBridge.addChange(ChangeFactory.attachmentPropertyChange(this, newChance, CHANCE));
    }
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("conditions",
            MutableProperty.of(
                this::setConditions,
                this::setConditions,
                this::getConditions,
                this::resetConditions))
        .put("conditionType",
            MutableProperty.ofString(
                this::setConditionType,
                this::getConditionType,
                this::resetConditionType))
        .put("invert",
            MutableProperty.ofMapper(
                DefaultAttachment::getBool,
                this::setInvert,
                this::getInvert,
                () -> false))
        .put("chance",
            MutableProperty.ofString(
                this::setChance,
                this::getChance,
                this::resetChance))
        .put("chanceIncrementOnFailure",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setChanceIncrementOnFailure,
                this::getChanceIncrementOnFailure,
                () -> 0))
        .put("chanceDecrementOnSuccess",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setChanceDecrementOnSuccess,
                this::getChanceDecrementOnSuccess,
                () -> 0))
        .build();
  }
}
