package to.etc.webapp.qsql;

import java.math.*;
import java.sql.*;
import java.util.*;

import to.etc.util.*;

/**
 * Utility class for JDBC code.
 *
 *
 * <h2>Preparation for calling SP's with TYPE xx IS RECORD parameters.</h2>
 * <p>Datadict table ALL_PROCEDURES contains all SP's in packages. The parameters for
 * SPs can be glanced from ALL_ARGUMENTS; something odd so far is that doing a selection:
 * <pre>
 * select * from sys.all_arguments where owner='DECADE' and package_name='GEBRUI' and object_name='LEES100';
 * </pre>
 * returns data that seem to indicate that the SP exists as a 2-parameter version but also an expanded version
 * having all parameter fields.</p>
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Dec 21, 2009
 */
public class JdbcUtil {
	private JdbcUtil() {}

	static public void setLong(PreparedStatement ps, int index, Long value) throws SQLException {
		if(value == null)
			ps.setNull(index, Types.NUMERIC);
		else
			ps.setLong(index, value.longValue());
	}

	static public void setInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
		if(value == null)
			ps.setNull(index, Types.NUMERIC);
		else
			ps.setInt(index, value.intValue());
	}

	static public void setDouble(PreparedStatement ps, int index, Double value) throws SQLException {
		if(value == null)
			ps.setNull(index, Types.DOUBLE);
		else
			ps.setDouble(index, value.doubleValue());
	}

	/**
	 * Sets a TIMESTAMP value containing both TIME and DATE values.
	 * @param ps
	 * @param index
	 * @param value
	 * @throws SQLException
	 */
	static public void setTimestamp(PreparedStatement ps, int index, java.util.Date value) throws SQLException {
		if(value == null)
			ps.setNull(index, Types.TIMESTAMP);
		else if(value instanceof Timestamp)
			ps.setTimestamp(index, (Timestamp) value);
		else
			ps.setTimestamp(index, new Timestamp(value.getTime()));
	}

	/**
	 * Sets a <b>truncated</b> date containing <i>only</i> the date part and a zero time.
	 * @param ps
	 * @param index
	 * @param value
	 * @throws SQLException
	 */
	static public void setDateTruncated(PreparedStatement ps, int index, java.util.Date value) throws SQLException {
		if(value == null)
			ps.setNull(index, Types.TIMESTAMP);
		else {
			value = DateUtil.truncateDate(value);
			ps.setDate(index, new java.sql.Date(value.getTime()));
		}
	}

	static public void	setString(PreparedStatement ps, int index, String value) throws SQLException {
		if(value == null || value.trim().length() == 0)
			ps.setNull(index, Types.VARCHAR);
		else
			ps.setString(index, value);
	}

	static public void setStringTruncated(PreparedStatement ps, int index, String value, int maxlen) throws SQLException {
		if(value == null || value.trim().length() == 0)
			ps.setNull(index, Types.VARCHAR);
		else {
			int len = value.length();
			if(len > maxlen)
				value = value.substring(0, maxlen);
			ps.setString(index, value);
		}
	}

	static public void setYN(PreparedStatement ps, int index, Boolean value) throws SQLException {
		if(value == null)
			ps.setNull(index, Types.VARCHAR);
		else
			ps.setString(index, value.booleanValue() ? "Y" : "N");
	}

	static public void setFK(PreparedStatement ps, int index, ILongIdentifyable foreigner) throws SQLException {
		if(foreigner == null)
			ps.setNull(index, Types.NUMERIC);
		else if(foreigner.getId() == null)
			throw new IllegalStateException("Reference to foreign object '" + foreigner + "' has a null ID.");
		else {
			ps.setLong(index, foreigner.getId().longValue());
		}
	}

	/**
	 * Quick method to select a single value of a given type from the database. Returns null if not found AND if the value was null...
	 * @param connection
	 * @param clz
	 * @param select
	 * @return
	 */
	public static <T> T selectOne(Connection connection, Class<T> clz, String select, Object... params) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = connection.prepareStatement(select);
			setParameters(ps, 1, params);
			rs = ps.executeQuery();
			if(! rs.next())
				return null;
			Object res = null;
			if(clz == String.class)
				return (T) rs.getString(1);
			else if(clz == Long.class || clz == long.class)
				res = Long.valueOf(rs.getLong(1));
			else if(clz == Integer.class || clz == int.class)
				res = Integer.valueOf(rs.getInt(1));
			else if(clz == java.util.Date.class) {
				java.sql.Timestamp ts = rs.getTimestamp(1);
				if(ts != null)
					res = new java.util.Date(ts.getTime());
			} else if(clz == Boolean.class || clz == boolean.class)
				res = Boolean.valueOf(rs.getBoolean(1));
			else
				throw new IllegalStateException("Call error: cannot handle requested return type " + clz);
			if(rs.wasNull())
				return null;
			return (T) res;
		} catch(SQLException x) {
			String msg = x.getMessage();
			if(rs != null && msg != null && msg.contains("internal representation")) {
				String res = "(cannot obtain value)";
				try {
					res = rs.getString(1);
				} catch(Exception xx) {}
				throw new SQLException("Cannot convert '" + res + "' to internal representation " + clz + ": " + x, x);
			}
			throw x;
		} finally {
			try {
				if(rs != null)
					rs.close();
			} catch(Exception x) {}
			try {
				if(ps != null)
					ps.close();
			} catch(Exception x) {}
		}
	}

	/**
	 * Quick method to select a single value of a given type from the database. Returns null if not found AND if the value was null...
	 * @param connection
	 * @param clz
	 * @param select
	 * @return
	 */
	public static <T> List<T> selectSingleColumnList(Connection connection, Class<T> clz, String select, Object... params) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = connection.prepareStatement(select);
			setParameters(ps, 1, params);
			rs = ps.executeQuery();
			List<T> list = new ArrayList<T>();
			while(rs.next()) {
				Object res = null;
				if(clz == String.class)
					res = rs.getString(1);
				else if(clz == Long.class || clz == long.class)
					res = Long.valueOf(rs.getLong(1));
				else if(clz == Integer.class || clz == int.class)
					res = Integer.valueOf(rs.getInt(1));
				else if(clz == java.util.Date.class) {
					java.sql.Timestamp ts = rs.getTimestamp(1);
					if(ts != null)
						res = new java.util.Date(ts.getTime());
				} else if(clz == Boolean.class || clz == boolean.class)
					res = Boolean.valueOf(rs.getBoolean(1));
				else
					throw new IllegalStateException("Call error: cannot handle requested return type " + clz);
				if(rs.wasNull())
					list.add(null);
				else
					list.add((T) res);
			}
			return list;
		} catch(SQLException x) {
			String msg = x.getMessage();
			if(rs != null && msg != null && msg.contains("internal representation")) {
				String res = "(cannot obtain value)";
				try {
					res = rs.getString(1);
				} catch(Exception xx) {}
				throw new SQLException("Cannot convert '" + res + "' to internal representation " + clz + ": " + x, x);
			}
			throw x;
		} finally {
			try {
				if(rs != null)
					rs.close();
			} catch(Exception x) {}
			try {
				if(ps != null)
					ps.close();
			} catch(Exception x) {}
		}
	}

	public static void setParameters(PreparedStatement ps, int startindex, Object[] params) throws SQLException {
		if(params == null)
			return;
		for(int i = 0; i < params.length; i++) {
			Object val = params[i];
			int px = i + startindex;
			if(val == null)
				ps.setString(px, null);
			else if(val instanceof String) {
				ps.setString(px, ((String) val));
			} else if(val instanceof Long) {
				ps.setLong(px, ((Long) val).longValue());
			} else if(val instanceof Integer) {
				ps.setInt(px, ((Integer) val).intValue());
			} else if(val instanceof BigDecimal) {
				ps.setBigDecimal(px, (BigDecimal) val);
			} else if(val instanceof Double) {
				ps.setDouble(px, ((Double) val).doubleValue());
			} else if(val instanceof java.sql.Timestamp) {
				ps.setTimestamp(px, (java.sql.Timestamp) val);
			} else if(val instanceof java.util.Date) {
				ps.setTimestamp(px, new Timestamp(((java.util.Date) val).getTime()));
			} else if(val instanceof Boolean) {
				ps.setBoolean(px, ((Boolean) val).booleanValue());
			} else
				throw new IllegalStateException("Call error: unknown SQL parameter of type " + val.getClass());
		}
	}

	static public List<JdbcAnyRecord> queryAny(Connection dbc, String select, Object... parameters) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = dbc.prepareStatement(select);
			setParameters(ps, 1, parameters);
			rs = ps.executeQuery();
			return queryAny(select, rs);
		} finally {
			try {
				if(rs != null)
					rs.close();
			} catch(Exception x) {}
			try {
				if(ps != null)
					ps.close();
			} catch(Exception x) {}
		}
	}

	static public List<JdbcAnyRecord> queryAny(String tblname, ResultSet rs) throws SQLException {
		List<JdbcAnyRecord> l = new ArrayList<JdbcAnyRecord>();
		ResultSetMetaData md = rs.getMetaData();
		while(rs.next()) {
			JdbcAnyRecord a = new JdbcAnyRecord();
			a.initFromRS(tblname, md, rs);
			l.add(a);
		}
		return l;
	}

	static public JdbcAnyRecord queryAnyOne(Connection dbc, String select, Object... parameters) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = dbc.prepareStatement(select);
			setParameters(ps, 1, parameters);
			rs = ps.executeQuery();
			return queryAnyOne(select, rs);
		} finally {
			try {
				if(rs != null)
					rs.close();
			} catch(Exception x) {}
			try {
				if(ps != null)
					ps.close();
			} catch(Exception x) {}
		}
	}

	static public JdbcAnyRecord queryAnyOne(String tblname, ResultSet rs) throws SQLException {
		if(!rs.next())
			return null;
		ResultSetMetaData md = rs.getMetaData();
		JdbcAnyRecord a = new JdbcAnyRecord();
		a.initFromRS(tblname, md, rs);
		if(rs.next())
			throw new SQLException("Got >1 result for queryAnyOne");
		return a;
	}

	/**
	 * Back to the 60's: oracle JDBC cannot interface with PL/SQL boolean type, of course. So
	 * we need to call PL/SQL methods that use that incredibly complex type using a wrapper. To
	 * prevent having to create a zillion wrapper procedures we create an anonymous block
	 * doing that, hopefully.
	 *
	 * Oracle: costs a lot, works like shit.
	 *
	 * @param <T>
	 * @param dbc
	 * @param rv
	 * @param sp
	 * @return
	 */
	static public <T> T oracleMoronsCallSP(Connection dbc, Class<T> rtype, String sp, Object... args) throws SQLException {
		if(rtype == Boolean.class || rtype == boolean.class)
			return (T) Boolean.valueOf(oracleMoronsCallSPReturningBool(dbc, sp, args));

		StringBuilder sb = new StringBuilder();
		sb.append("begin ");
		int startix = 1;
		if(rtype != null && rtype != Void.class) {
			sb.append("? := ");
			startix = 2;
		}
		List<Object> pars = new ArrayList<Object>(args.length);
		sb.append(sp).append('(');
		appendSPParameters(sb, pars, args);
		sb.append(");");
		sb.append("end;");
		String stmt = sb.toString();
		System.out.println("CALLING: " + stmt);

		//-- Call the SP
		CallableStatement ps = null;
		try {
			ps = dbc.prepareCall(stmt);
			if(startix != 1)
				ps.registerOutParameter(1, calcSQLTypeFor(rtype));
			setParameters(ps, startix, pars.toArray());
			ps.execute();

			if(startix != 1) {
				return readPsValue(ps, 1, rtype);
			} else {
				return null;
			}
		} finally {
			try {
				if(ps != null)
					ps.close();
			} catch(Exception x) {}
		}
	}

	static private int calcSQLTypeFor(Class< ? > rt) {
		if(rt == String.class)
			return Types.VARCHAR;
		else if(rt == Integer.class || rt == int.class || rt == Long.class || rt == long.class || rt == BigDecimal.class || rt == Double.class || rt == double.class)
			return Types.NUMERIC;
		else
			throw new IllegalStateException("Call error: cannot get SQLType for java type=" + rt);
	}

	private static void appendSPParameters(StringBuilder sb, List<Object> pars, Object[] args) {
		//-- Handle parameters, and handle boolean arguments, sigh.
		for(int i = 0; i < args.length; i++) {
			Object val = args[i];
			if(i > 0)
				sb.append(',');
			if(val instanceof Boolean) {
				sb.append(((Boolean) val).booleanValue() ? "true" : "false");
			} else {
				sb.append("?");
				pars.add(val);
			}
		}
	}

	public static boolean oracleMoronsCallSPReturningBool(Connection dbc, String sp, Object... args) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("declare l_res number; ");
		sb.append("begin if(").append(sp).append("(");
		List<Object> pars = new ArrayList<Object>(args.length);
		appendSPParameters(sb, pars, args);
		sb.append(")) then l_res := 1; else l_res := 0; end if; ? := l_res;");
		sb.append("end;");
		String stmt = sb.toString();
		System.out.println("CALLING: " + stmt + ", out=" + (pars.size() + 1));

		//-- Call the SP
		CallableStatement ps = null;
		try {
			ps = dbc.prepareCall(stmt);
			setParameters(ps, 1, pars.toArray());
			ps.registerOutParameter(pars.size() + 1, Types.NUMERIC);
			ps.execute();
			int res = ps.getInt(pars.size() + 1);
			return res != 0;
		} finally {
			try {
				if(ps != null)
					ps.close();
			} catch(Exception x) {}
		}
	}

	public static boolean executeStatement(Connection dbc, String sql, Object... args) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = dbc.prepareStatement(sql);
			setParameters(ps, 1, args);
			return ps.execute();
		} finally {
			try {
				if(ps != null)
					ps.close();
			} catch(Exception x) {}
		}
	}

	/**
	 * Similar as {@link JdbcUtil#oracleMoronsCallSP(Connection, Class, String, Object...)}.
	 * Provides additional interface to read OUT values that are defined in SP/function call.
	 * Only constraint is that OUT params are always after all IN params.
	 * @param <T>
	 * @param con
	 * @param rtype
	 * @param sp
	 * @param outParams
	 * @param args
	 * @return
	 * @throws SQLException
	 */
	static public <T> T oracleFunctionCallSP(Connection con, Class<T> rtype, String sp, JdbcOutParam<?>[] outParams, Object... args) throws SQLException {
		if(rtype == Boolean.class || rtype == boolean.class) {
			return (T) Boolean.valueOf(oracleFunctionCallSPReturningBool(con, sp, outParams, args));
		}

		StringBuilder sb = new StringBuilder();
		sb.append("begin ");
		int startix = 1;
		if(rtype != null && rtype != Void.class) {
			sb.append("? := ");
			startix = 2;
		}
		List<Object> pars = new ArrayList<Object>(args.length);
		sb.append(sp).append('(');
		appendSPParameters(sb, pars, args);
		if (args.length > 0) {
			sb.append(',');
		}
		if(outParams != null) {
			for(int i = 0; i < outParams.length; i++) {
				sb.append('?');
				if(i < (outParams.length - 1)) {
					sb.append(',');
				}
			}
		}
		sb.append(");");
		sb.append("end;");
		String stmt = sb.toString();
		System.out.println("CALLING: " + stmt);

		//-- Call the SP
		CallableStatement ps = null;
		try {
			ps = con.prepareCall(stmt);
			if(startix != 1)
				ps.registerOutParameter(1, calcSQLTypeFor(rtype));
			setParameters(ps, startix, pars.toArray());
			if(outParams != null) {
				registerOutSPParameters(ps, startix + pars.size(), outParams);
			}

			ps.execute();
			if(outParams != null) {
				int outIndex = startix + pars.size();
				for(JdbcOutParam< ? > outParam : outParams) {
					setOutParamValue(ps, outIndex++, outParam.getClassType(), outParam);
				}
			}

			if(startix != 1) {
				return readPsValue(ps, 1, rtype);
			} else {
				return null;
			}
		} finally {
			FileTool.closeAll(ps);
		}
	}

	/**
	 * Similar as {@link JdbcUtil#oracleFunctionCallSP(Connection, Class, String, JdbcOutParam[], Object...)},
	 * adjusted to handle returning of oracle boolean type properly.
	 * @param con
	 * @param sp
	 * @param outParams
	 * @param args
	 * @return
	 * @throws SQLException
	 */
	public static boolean oracleFunctionCallSPReturningBool(Connection con, String sp, JdbcOutParam< ? >[] outParams, Object... args) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("declare l_res number; ");
		sb.append("begin if(").append(sp).append("(");
		List<Object> pars = new ArrayList<Object>(args.length);
		appendSPParameters(sb, pars, args);
		if(args.length > 0) {
			sb.append(',');
		}
		int outSize = 0;
		if(outParams != null) {
			outSize = outParams.length;
			for(int i = 0; i < outParams.length; i++) {
				sb.append('?');
				if(i < (outSize - 1)) {
					sb.append(',');
				}
			}
		}
		sb.append(")) then l_res := 1; else l_res := 0; end if; ? := l_res;");
		sb.append("end;");
		String stmt = sb.toString();
		System.out.println("CALLING: " + stmt + ", outResult=" + (pars.size() + outSize + 1));

		//-- Call the SP
		CallableStatement ps = null;
		try {
			ps = con.prepareCall(stmt);
			setParameters(ps, 1, pars.toArray());
			if(outParams != null) {
				registerOutSPParameters(ps, pars.size() + 1, outParams);
			}
			ps.registerOutParameter(pars.size() + outSize + 1, Types.NUMERIC);
			ps.execute();
			if(outParams != null) {
				int outIndex = pars.size() + 1;
				for(JdbcOutParam< ? > outParam : outParams) {
					setOutParamValue(ps, outIndex++, outParam.getClassType(), outParam);
				}
			}
			int res = ps.getInt(pars.size() + outSize + 1);
			return res != 0;
		} finally {
			FileTool.closeAll(ps);
		}
	}

	/**
	 * Just a overload wrapper for single OUT param function call.
	 * See {@link JdbcUtil#oracleFunctionCallSPReturningBool(Connection, String, JdbcOutParam[], Object...)}
	 * @param con
	 * @param sp
	 * @param outParam
	 * @param args
	 * @return
	 * @throws SQLException
	 */
	public static boolean oracleFunctionCallSPReturningBool(Connection con, String sp, JdbcOutParam< ? > outParam, Object... args) throws SQLException {
		JdbcOutParam< ? >[] outParams = new JdbcOutParam[1];
		outParams[0] = outParam;
		return oracleFunctionCallSPReturningBool(con, sp, outParams, args);
	}

	private static <T> void setOutParamValue(CallableStatement ps, int index, Class<T> rtype, JdbcOutParam< ? > pOutParam) throws SQLException {
		JdbcOutParam<T> outParam = (JdbcOutParam<T>) pOutParam;
		outParam.setValue(readPsValue(ps, index, rtype));
	}

	private static <T> T readPsValue(CallableStatement ps, int index, Class<T> rtype) throws SQLException {
		if(rtype == String.class) {
			return (T) ps.getString(index);
		} else if(rtype == Integer.class || rtype == int.class) {
			return (T) (Integer.valueOf(ps.getInt(index)));
		} else if(rtype == Long.class || rtype == long.class) {
			return (T) (Long.valueOf(ps.getLong(index)));
		} else if(rtype == Double.class || rtype == double.class) {
			return (T) (Double.valueOf(ps.getDouble(index)));
		} else if(rtype == BigDecimal.class) {
			return (T) ps.getBigDecimal(index);
		} else {
			throw new IllegalStateException("Call error: cannot get out parameter for result java type=" + rtype);
		}
	}

	private static void registerOutSPParameters(CallableStatement ps, int startix, JdbcOutParam< ? >[] outParams) throws SQLException {
		for (int i = 0; i < outParams.length; i++) {
			ps.registerOutParameter(startix + i, calcSQLTypeFor(outParams[i].getClassType()));
		}
	}


}
