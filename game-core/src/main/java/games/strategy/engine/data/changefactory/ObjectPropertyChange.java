package games.strategy.engine.data.changefactory;

import java.io.IOException;
import java.io.ObjectInputStream;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Unit;

/**
 * A game data change that captures a change to an object property value.
 */
public class ObjectPropertyChange extends Change {
  private static final long serialVersionUID = 4218093376094170940L;
  private final Unit m_object;
  private String m_property;
  private final Object m_newValue;
  private final Object m_oldValue;

  ObjectPropertyChange(final Unit object, final String property, final Object newValue) {
    m_object = object;
    m_property = property.intern();
    m_newValue = newValue;
    m_oldValue = object.getPropertyOrThrow(property).getValue();
  }

  private ObjectPropertyChange(final Unit object, final String property, final Object newValue,
      final Object oldValue) {
    m_object = object;
    // prevent multiple copies of the property names being held in the game
    m_property = property.intern();
    m_newValue = newValue;
    m_oldValue = oldValue;
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    m_property = m_property.intern();
  }

  @Override
  public Change invert() {
    return new ObjectPropertyChange(m_object, m_property, m_oldValue, m_newValue);
  }

  @Override
  protected void perform(final GameData data) {
    try {
      m_object.getPropertyOrThrow(m_property).setValue(m_newValue);
    } catch (final MutableProperty.InvalidValueException e) {
      throw new IllegalStateException(
          String.format(
              "failed to set value '%s' on property '%s' for object '%s'",
              m_newValue, m_property, m_object),
          e);
    }
  }

  @Override
  public String toString() {
    return "Property change, unit:" + m_object + " property:" + m_property + " newValue:" + m_newValue + " oldValue:"
        + m_oldValue;
  }
}
