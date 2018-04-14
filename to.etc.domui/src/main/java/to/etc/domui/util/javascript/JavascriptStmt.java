package to.etc.domui.util.javascript;

import org.eclipse.jdt.annotation.NonNull;
import to.etc.domui.dom.html.NodeBase;
import to.etc.json.JSON;
import to.etc.util.StringTool;

public class JavascriptStmt {
	@NonNull
	final private StringBuilder m_sb;

	private boolean m_instmt;

	private JsMethod m_currentMethod;

	public JavascriptStmt(@NonNull StringBuilder worksb) {
		m_sb = worksb;
	}

	@NonNull
	public StringBuilder sb() {
		return m_sb;
	}

	@NonNull
	public JavascriptStmt next() {
		if(!m_instmt)
			return this;
		if(m_sb.length() == 0)
			return this;
		char last = m_sb.charAt(m_sb.length() - 1);
		if(last != ';')
			m_sb.append(";");
		m_instmt = false;
		return this;
	}

	/**
	 * Add a JQuery selector.
	 * @param node
	 * @return
	 */
	@NonNull
	public JavascriptStmt select(@NonNull NodeBase node) {
		return select(node.getActualID());
	}

	/**
	 * Add a JQuery selector.
	 * @param id
	 * @return
	 */
	@NonNull
	public JavascriptStmt select(@NonNull String id) {
		sb().append("$('#").append(id).append("')");
		m_instmt = true;
		return this;
	}

	char lastChar() {
		int length = m_sb.length();
		if(length == 0)
			return 0;
		return m_sb.charAt(length - 1);
	}

	/**
	 * Just append a verbatim string.
	 * @param string
	 * @return
	 */
	@NonNull
	public JavascriptStmt append(@NonNull String string) {
		sb().append(string);
		m_instmt = true;
		return this;
	}

	/**
	 * Render any kind of Java object as a Javascript thingy.
	 * @param object
	 * @return
	 */
	@NonNull
	public JavascriptStmt object(@NonNull Object object) throws Exception {
		m_instmt = true;

		if(object == null) {
			sb().append("null");
		} else if(object instanceof String) {
			StringTool.strToJavascriptString(sb(), (String) object, false);
		} else if(object instanceof Number) {
			sb().append(object.toString());
		} else if(object instanceof java.util.Date) {
			sb().append(((java.util.Date) object).getTime());
		} else if(object instanceof Boolean) {
			sb().append(((Boolean) object).toString());
		} else {
			//-- Use JSON marshaller
			sb().append(JSON.render(object));
		}
		return this;
	}

	@NonNull
	public JavascriptStmt endmethod() {
		JsMethod jm = m_currentMethod;
		if(null != jm) {
			m_currentMethod = null;
			jm.flush();
		}
		return this;
	}

	@NonNull
	public JsMethod method(@NonNull String name) {
		m_instmt = true;
		endmethod();
		char lc = lastChar();
		if(lc != 0 && lc != ';' && lc != '.' && lc != '(') {
			sb().append('.');
		}
		sb().append(name);
		sb().append('(');
		return m_currentMethod = new JsMethod(this);
	}

	public static void main(String[] args) throws Exception {
		StringBuilder sb = new StringBuilder();
		JavascriptStmt st = new JavascriptStmt(sb);

		st.select("_IZ").method("options").arg("hello").arg(Integer.valueOf(1)).arg(Boolean.TRUE).end().next();

		System.out.println(sb.toString());

		sb.setLength(0);

		st.select("_IZ").method("options").arg("hello").arg(Integer.valueOf(1)).arg(Boolean.TRUE).end().next();


	}
}
