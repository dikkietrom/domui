package to.etc.domui.log.tailer;

import org.eclipse.jdt.annotation.NonNull;
import to.etc.domui.annotations.UIUrlParameter;
import to.etc.domui.component.buttons.DefaultButton;
import to.etc.domui.component.layout.title.AppPageTitleBar;
import to.etc.domui.component.misc.InfoPanel;
import to.etc.domui.dom.html.IClicked;
import to.etc.domui.dom.html.UrlPage;
import to.etc.domui.server.IRequestContext;
import to.etc.domui.state.PageParameters;
import to.etc.domui.state.UIContext;
import to.etc.domui.state.UIGoto;

public class ServerLogPage extends UrlPage {
	private String m_logPath;

	private String m_key;

	static private int m_ix;


	@UIUrlParameter(name = "key")
	public String getKey() {
		return m_key;
	}

	public void setKey(String key) {
		m_key = key;
	}

	@Override
	public void createContent() throws Exception {
		add(new AppPageTitleBar("Follow a server log file", true));

		//-- Get data.
		String log = (String) UIContext.getRequestContext().getSession().getAttribute(getKey());
		if(log == null) {
			add(new InfoPanel("The key for the file to access is no longer valid. Please select another file to show."));
			return;
		}

		add(new LogTailerFragment(log));

		add(new DefaultButton("Back", new IClicked<DefaultButton>() {
			@Override
			public void clicked(@NonNull DefaultButton clickednode) throws Exception {
				UIGoto.back();
			}
		}));
	}

	static private synchronized String newKey() {
		return "f" + (m_ix++);
	}

	static public PageParameters createParameters(IRequestContext ctx, String path) throws Exception {
		PageParameters pp = new PageParameters();
		String key = newKey();
		ctx.getSession().setAttribute(key, path);
		pp.addParameters("key", key);
		return pp;
	}

	static public void moveSub(String path) throws Exception {
		UIGoto.moveSub(ServerLogPage.class, createParameters(UIContext.getRequestContext(), path));
	}

}
