package to.etc.domui.dom.header;

import to.etc.domui.dom.*;
import to.etc.domui.dom.html.*;

/**
 * Header to load Google charts library if needed
 *
 * Created by tinie on 28-10-15.
 */
final public class GoogleChartsContributor extends HeaderContributor {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if(obj == null)
			return false;
		if(this == obj)
			return true;
		return getClass() == obj.getClass();
	}

	/**
	 * Generate the specified scriptlet as a script tag.
	 * @see HeaderContributor#contribute(HtmlFullRenderer)
	 */
	@Override
	public void contribute(final HtmlFullRenderer r) throws Exception {
		r.renderLoadJavascript("https://www.google.com/jsapi");
		r.o().tag("script");
		r.o().attr("language", "javascript");
		r.o().endtag();
		r.o().writeRaw("<!--\n"); // Embed JS in comment IMPORTANT: the \n is required!!!
		r.o().writeRaw("google.load('visualization', '1', {'packages':['corechart','gauge']});");
		r.o().writeRaw("\n-->");
		r.o().closetag("script");
	}

	@Override
	public void contribute(OptimalDeltaRenderer r) throws Exception {
		r.renderLoadJavascript("https://www.google.com/jsapi");
		r.o().writeRaw("google.load('visualization', '1', {'packages':['corechart','gauge']});");
	}
}
