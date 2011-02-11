package to.etc.domui.dom.html;

import java.util.*;

import to.etc.domui.component.meta.*;

/**
 * This is a simple marker which groups radiobuttons together. It can be used as a component too.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Feb 4, 2011
 */
public class RadioGroup<T> extends Div implements IHasChangeListener {
	static private int m_gidCounter;

	private String m_groupName;

	private List<RadioButton<T>> m_buttonList = new ArrayList<RadioButton<T>>();

	private T m_value;

	private IValueChanged< ? > m_onValueChanged;

	public RadioGroup() {
		m_groupName = "g" + nextID();
	}

	static private synchronized int nextID() {
		return ++m_gidCounter;
	}

	void addButton(RadioButton<T> b) {
		m_buttonList.add(b);
	}

	void removeButton(RadioButton<T> b) {
		m_buttonList.remove(b);
	}

	public String getName() {
		return m_groupName;
	}

	public T getValue() {
		return m_value;
	}

	public void setValue(T value) {
		if(MetaManager.areObjectsEqual(value, m_value))
			return;
		m_value = value;
		for(RadioButton<T> rb : getButtonList()) {
			rb.setChecked(MetaManager.areObjectsEqual(value, rb.getButtonValue()));
		}
	}

	void internalSetValue(T newval) {
		m_value = newval;
	}

	public List<RadioButton<T>> getButtonList() {
		return Collections.unmodifiableList(m_buttonList);
	}

	public IValueChanged< ? > getOnValueChanged() {
		return m_onValueChanged;
	}

	public void setOnValueChanged(IValueChanged< ? > onValueChanged) {
		m_onValueChanged = onValueChanged;
	}
}