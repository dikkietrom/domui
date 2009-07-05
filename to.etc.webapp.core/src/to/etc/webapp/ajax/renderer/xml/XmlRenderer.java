package to.etc.webapp.ajax.renderer.xml;

import java.io.*;
import java.util.*;

import to.etc.webapp.ajax.renderer.*;
import to.etc.xml.*;

public class XmlRenderer extends ObjectRenderer {
	public XmlRenderer(final XmlRegistry r, final XmlWriter w) {
		super(r, w);
	}

	public XmlWriter xw() {
		return (XmlWriter) getWriter();
	}

	@Override
	protected void renderRoot(final Object root) throws Exception {
		xw().println("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
		if(root == null)
			xw().tagnl("result");
		else
			xw().tagnl("result", "type", getTypeName(root.getClass()));
		super.renderRoot(root);
		xw().tagendnl();
	}

	public void xmlTag(final String name, final Class< ? > type, final String val) throws IOException {
		xmlTag(type, name);
		xw().cdata(val);
		xw().tagendnl();
	}

	public void xmlFullTag(final String name, final String type, final String val) throws IOException {
		XmlWriter w = xw();
		w.tag(name, "type", type);
		xw().cdata(val);
		xw().tagendnl();
	}

	public String getTypeName(final Class< ? > type) {
		if(type == null)
			return null;
		return ((XmlRegistry) getRegistry()).getType(type);
	}

	public void xmlTag(final Class< ? > type, final String name) throws IOException {
		XmlWriter w = xw();
		if(type != null) {
			String tyname = getTypeName(type);
			if(tyname != null) {
				w.tag(name, "type", tyname);
				return;
			}
		}
		w.tag(name, ">");
	}

	@Override
	public void renderObjectEnd(final Object o) throws Exception {
	//		XmlWriter w = xw();
	//		w.tagendnl();
	}

	/**
	 * Renders the start tag for a class type to be rendered. The name of the
	 * class is the class name.
	 * @see to.etc.webapp.ajax.renderer.ObjectRenderer#renderObjectStart(java.lang.Object, java.lang.Class, java.lang.String)
	 */
	@Override
	public void renderObjectStart(final Object o) throws Exception {
		//		xmlTag(type, name);
		xw().forceNewline();
	}

	@Override
	protected void renderObjectMember(final Object o, final String name, final Class< ? > declaredType) throws Exception {
		Class< ? > type = o == null ? declaredType : o.getClass();
		xmlTag(type, name);
		renderSub(o);
		xw().tagendnl();
	}

	@Override
	public void renderListStart(final Collection< ? > l, final String name) throws Exception {
		xw().tagnl(name, "type", "xsi:list");
	}

	@Override
	public void renderListEnd(final Collection< ? > l, final String name) throws Exception {
		xw().tagendnl();
	}

	@Override
	public void renderArrayStart(final Object ar) throws Exception {}

	@Override
	public void renderArrayEnd(final Object ar) throws Exception {}

	@Override
	protected void renderArrayElement(final Object o, final Class< ? > declaredType, final int ix) throws Exception {
		Class< ? > type = o == null ? declaredType : o.getClass();
		xw().forceNewline();
		xmlTag(type, "item");
		renderSub(o);
		xw().tagendnl();
	}

	@Override
	public void renderMapStart(final Map< ? , ? > l) throws Exception {
	//		xw().tagnl(name, "type", "xsi:map");
	}

	@Override
	public void renderMapEnd(final Map< ? , ? > l) throws Exception {
	//		xw().tagendnl();
	}

	@Override
	public void renderMapEntry(final Object key, final Object value, final int itemnr, final int maxitemnr) throws Exception {
		xw().tagnl("item");
		xmlTag(key.getClass(), "key");
		renderSub(key);
		xw().tagendnl();
		xmlTag(value == null ? null : value.getClass(), "value");
		renderSub(value);
		xw().tagendnl();
		xw().tagendnl();
	}
}
