package to.etc.domui.component2.form4;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import to.etc.domui.dom.html.NodeBase;
import to.etc.domui.dom.html.NodeContainer;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 6-3-18.
 */
public interface IFormLayouter {
	void setHorizontal(boolean horizontal);

	void addControl(@NonNull NodeBase control, @Nullable NodeContainer lbl, @Nullable String controlCss, @Nullable String labelCss, boolean append);

	void clear();

	void appendAfterControl(NodeBase what);
}
