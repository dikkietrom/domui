package to.etc.formbuilder.pages;

import org.eclipse.jdt.annotation.NonNull;
import to.etc.domui.component.meta.PropertyMetaModel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ComboPropertyEditorFactory implements IPropertyEditorFactory {
	@NonNull
	private Class<?> m_actualClass;

	@NonNull
	private Object[]	m_values;

	private boolean m_required;

	@NonNull
	static private final Map<Class< ? >, ComboPropertyEditorFactory> m_identMap = new HashMap<Class< ? >, ComboPropertyEditorFactory>();

	private ComboPropertyEditorFactory(@NonNull Class< ? > actualClass, @NonNull Object[] values, boolean required) {
		m_actualClass = actualClass;
		m_values = values;
		m_required = required;
	}

	@NonNull
	static public ComboPropertyEditorFactory createFactory(@NonNull PropertyMetaModel< ? > pmm) {
		Class< ? > clz = pmm.getActualType();
		ComboPropertyEditorFactory ef = m_identMap.get(clz);
		if(ef == null) {
			ef = new ComboPropertyEditorFactory(clz, pmm.getDomainValues(), pmm.isRequired() || clz.isPrimitive());
			m_identMap.put(clz, ef);
		}
		return ef;
	}

	@Override
	public IPropertyEditor createEditor(@NonNull PropertyDefinition pd) {
		return new ComboPropertyEditor(pd, this);
	}

	@NonNull
	public Class< ? > getActualClass() {
		return m_actualClass;
	}

	@NonNull
	public Object[] getValues() {
		return m_values;
	}

	public boolean isRequired() {
		return m_required;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_actualClass == null) ? 0 : m_actualClass.hashCode());
		result = prime * result + (m_required ? 1231 : 1237);
		result = prime * result + Arrays.hashCode(m_values);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		ComboPropertyEditorFactory other = (ComboPropertyEditorFactory) obj;
		if(m_actualClass == null) {
			if(other.m_actualClass != null)
				return false;
		} else if(!m_actualClass.equals(other.m_actualClass))
			return false;
		if(m_required != other.m_required)
			return false;
		return Arrays.equals(m_values, other.m_values);
	}
}
