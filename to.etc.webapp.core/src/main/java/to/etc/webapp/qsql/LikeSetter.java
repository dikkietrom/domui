package to.etc.webapp.qsql;

import org.eclipse.jdt.annotation.NonNull;

import java.sql.PreparedStatement;

class LikeSetter implements IQValueSetter {
	private int m_index;

	private String m_value;

	private JdbcPropertyMeta m_property;

	public LikeSetter(int index, String like, JdbcPropertyMeta pm) {
		m_index = index;
		m_value = like;
		m_property = pm;
	}

	public JdbcPropertyMeta getProperty() {
		return m_property;
	}

	public int getIndex() {
		return m_index;
	}

	/**
	 * @see to.etc.webapp.qsql.IQValueSetter#assign(java.sql.PreparedStatement)
	 */
	@Override
	public void assign(@NonNull PreparedStatement ps) throws Exception {
		ps.setString(getIndex(), m_value);
	}
}
