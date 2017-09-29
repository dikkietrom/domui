package to.etc.domui.hibernate.types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * A UserType implementation to map a boolean primitive object to a numeric value.<br /> A true
 * value maps to 1 and a false value maps to 0. This type does not recognise
 * nullity; it gets interpreted as a false.
 * @author jal
 */
final public class BooleanPrimitiveOneZeroType implements UserType {
	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return null;
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		if(value == null)
			return value;
		return Boolean.valueOf(((Boolean) value).booleanValue());
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		return null;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if(x != null)
			return x.equals(y);
		else
			return x == y;
	}

	@Override
	public int hashCode(Object arg0) throws HibernateException {
		return arg0.hashCode();
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws HibernateException, SQLException {
		if(resultSet == null)
			return null;
		long v = resultSet.getLong(names[0]);
		if(resultSet.wasNull())
			return Boolean.FALSE;
		return Boolean.valueOf(v != 0);
	}

	@Override
	public void nullSafeSet(PreparedStatement statement, Object value, int index) throws HibernateException, SQLException {
		statement.setLong(index, value == null ? 0 : ((Boolean) value).booleanValue() ? 1 : 0);
	}

	@Override
	public Object replace(Object arg0, Object arg1, Object arg2) throws HibernateException {
		return null;
	}

	@Override
	public Class< ? > returnedClass() {
		return Boolean.class;
	}

	@Override
	public int[] sqlTypes() {
		return new int[]{Types.VARCHAR};
	}

	/**
	 * Parsing of a String yields the following results: TRUE: if src equals
	 * y,yes,1 or 'true' (case insensitive) FALSE: in all other cases
	 *
	 * @param src
	 * @return
	 */
	public static Boolean parse(String src) {
		if("1".equals(src) || "true".equalsIgnoreCase(src) || "Y".equalsIgnoreCase(src) || "yes".equalsIgnoreCase(src)) {
			return Boolean.TRUE;
		} else {
			return Boolean.FALSE;
		}
	}

}
