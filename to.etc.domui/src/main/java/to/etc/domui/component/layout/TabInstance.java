package to.etc.domui.component.layout;

import to.etc.domui.component.event.*;
import to.etc.domui.dom.errors.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;

import javax.annotation.*;
import java.util.*;

/**
 * @author <a href="mailto:yoeri.nijs@itris.nl">Yoeri Nijs, Vladimir Mijic</a>
 * vmijic 20090923 TabInstance can be registered as ErrorMessageListener in case when TabPanel has m_markErrorTabs set.
 * Created on 18-8-17.
 */
@DefaultNonNull
public class TabInstance implements IErrorMessageListener, ITabHandle {

	@Nullable
	private NodeBase m_label;

	@Nullable
	private NodeBase m_content;

	@Nullable
	private Img m_img;

	@Nullable
	private Li m_tab;

	@Nullable
	private Li m_separator;

	private boolean m_lazy;

	private boolean m_added;

	private boolean m_closable;

	private List<UIMessage> m_msgList = new ArrayList<UIMessage>();

	@Nullable
	private INotify<ITabHandle> m_onClose;

	public TabInstance() {}

	public TabInstance(@Nullable NodeBase label, @Nullable NodeBase content, @Nullable String image) {
		m_label = label;
		m_content = content;
		if(null != image) {
			setImage(image);
		}
	}

	public NodeBase getContent() {
		return DomUtil.nullChecked(m_content, "Content not provided!");
	}

	public void setContent(@Nonnull NodeBase content) {
		m_content = content;
	}

	public NodeBase getLabel() {
		return DomUtil.nullChecked(m_label, "Label not provided!");
	}

	public void setLabel(@Nonnull NodeBase label) {
		m_label = label;
	}

	public Li getTab() {
		return DomUtil.nullChecked(m_tab, "Tab not provided!");
	}

	public void setTab(@Nonnull Li tab) {
		m_tab = tab;
	}

	public Li getSeparator() {
		return DomUtil.nullChecked(m_separator, "Separator not provided!");
	}

	public void setSeparator(@Nonnull Li separator) {
		m_separator = separator;
	}

	public Img getImg() {
		return DomUtil.nullChecked(m_img, "Image not provided!");
	}

	public void setImage(@Nonnull Img image) {
		m_img = image;
	}

	public void setImage(@Nonnull String image) {
		if(image.isEmpty()) {
			return; // If string is empty, we do not have to create an image.
		}
		Img img = createIcon(image);
		m_img = img;
	}

	public boolean isLazy() {
		return m_lazy;
	}

	public void setLazy(boolean lazy) {
		m_lazy = lazy;
	}

	public boolean isAdded() {
		return m_added;
	}

	protected void setAdded(boolean added) {
		m_added = added;
	}

	/**
	 * If true this tab can be closed. A cross is added.
	 *
	 * @return
	 */
	public boolean isCloseable() {
		return m_closable;
	}

	public void closable(boolean closeable) {
		m_closable = closeable;
	}

	@Override
	public void setOnClose(@Nullable INotify<ITabHandle> notify) {
		m_onClose = notify;
	}

	@Nullable
	public INotify<ITabHandle> getOnClose() {
		return m_onClose;
	}

	@Override
	public void errorMessageAdded(@Nonnull UIMessage m) {
		if(isPartOfContent(m.getErrorNode())) {
			if(m_msgList.contains(m))
				return;
			m_msgList.add(m);
			adjustUI();
		}
	}

	@Override
	public void errorMessageRemoved(@Nonnull UIMessage m) {
		if(isPartOfContent(m.getErrorNode())) {
			if(!m_msgList.remove(m))
				return;
			adjustUI();
		}
	}

	/**
	 * Returns T if the node passed - or any of it's parents - is part of this content area.
	 *
	 * @param errorNode
	 * @return
	 */
	final private boolean isPartOfContent(@Nullable NodeBase errorNode) {
		while(errorNode != null) {
			if(errorNode == m_content) {
				return true;
			}
			if(!errorNode.hasParent())
				return false;
			errorNode = errorNode.getParent();
		}
		return false;
	}

	private void adjustUI() {
		if(hasErrors()) {
			getTab().addCssClass("ui-tab-err");
			//FIXME: this code can not work since there is refresh problem (error image is added only after refresh in browser is pressed)
			//is this same 'HTML rendering already done for visited node' bug in framework?
			//for now error image is set through css
			/*
			if(m_errorInfo == null) {
				m_errorInfo = new Img("THEME/mini-error.png");
				m_errorInfo.setTitle("Tab contain errors.");
				if(m_tab.getChildCount() > 0 && m_tab.getChild(0) instanceof ATag) {
					((ATag) m_tab.getChild(0)).add(m_errorInfo);
				}
			}
			*/
		} else {
			getTab().removeCssClass("ui-tab-err");
			//FIXME: this code can not work since there is refresh problem (error image is added only after refresh in browser is pressed)
			//is this same 'HTML rendering already done for visited node' bug in framework?
			/*
			if(m_errorInfo != null) {
				if(m_tab.getChildCount() > 0 && m_tab.getChild(0) instanceof ATag) {
					((ATag) m_tab.getChild(0)).removeChild(m_errorInfo);
				}
				m_errorInfo = null;
			}
			*/
		}
	}

	public boolean hasErrors() {
		return m_msgList.size() > 0;
	}

	private Img createIcon(String icon) {
		Img i = new Img();
		i.setSrc(icon);
		i.setCssClass("ui-tab-icon");
		i.setBorder(0);
		return i;
	}
}
