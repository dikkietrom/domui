package to.etc.domui.component.upload;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import to.etc.domui.component.buttons.DefaultButton;
import to.etc.domui.component.upload.BulkUpload.IUpload;
import to.etc.domui.dom.header.HeaderContributor;
import to.etc.domui.dom.html.Div;
import to.etc.domui.dom.html.IClicked;
import to.etc.domui.dom.html.Page;
import to.etc.domui.dom.html.Span;
import to.etc.domui.parts.ComponentPartRenderer;
import to.etc.domui.server.RequestContextImpl;
import to.etc.domui.state.ConversationContext;
import to.etc.domui.state.UIContext;
import to.etc.domui.util.Msgs;
import to.etc.domui.util.upload.UploadItem;

import java.util.ArrayList;
import java.util.List;

public class BulkUploadButton extends Div implements IUploadAcceptingComponent {
	/** The list of upload items not yet claimed by the UI code (coming in from Flash event). */
	final private List<UploadItem> m_newItemList = new ArrayList<UploadItem>();

	/** The list of claimed items. */
	final private List<UploadItem> m_itemList = new ArrayList<UploadItem>();

	@Nullable
	private IUpload m_onUpload;

	/** Event handler called when the file(s) have been selected and the upload of the 1st one has started, */
	@Nullable
	private IClicked<BulkUploadButton> m_onUploadsStarted;

	/** Event handler called when all uploads have completed. */
	@Nullable
	private IClicked<BulkUploadButton> m_onUploadsComplete;

	private DefaultButton m_startButton;


	public BulkUploadButton() {
		setCssClass("ui-bupb");
	}

	@Override
	public void createContent() throws Exception {
		m_startButton = new DefaultButton(Msgs.BUNDLE.getString(Msgs.BULKUPLD_SELECT_FILES));
		Span sp = new Span();
		add(sp);
		add(m_startButton);

		//-- Create the upload URL to UploadPart.
		StringBuilder sb = new StringBuilder();
		ComponentPartRenderer.appendComponentURL(sb, UploadPart.class, this, UIContext.getRequestContext());
		sb.append("?uniq=" + System.currentTimeMillis()); // Uniq the URL to prevent IE's stupid caching.
		String url = sb.toString();
		//		System.out.println("URL  = " + url);

		appendCreateJS("WebUI.bulkUpload('" + getActualID() + "','" + sp.getActualID() + "','" + url + "');");
	}

	@Override
	//@OverridingMethodsMustInvokeSuper
	public void onAddedToPage(Page p) {
		//-- Add the required javascript
		getPage().addHeaderContributor(HeaderContributor.loadJavascript("$js/swfupload.js"), 10);
		getPage().addHeaderContributor(HeaderContributor.loadJavascript("$js/jquery.swfupload.js"), 10);
		getPage().addHeaderContributor(HeaderContributor.loadJavascript("$js/swfupload.queue.js"), 10);
		getPage().addHeaderContributor(HeaderContributor.loadJavascript("$js/swfupload.swfobject.js"), 10);
		super.onAddedToPage(p);
	}

	@Nullable
	public IUpload getOnUpload() {
		return m_onUpload;
	}

	public void setOnUpload(IUpload onUpload) {
		m_onUpload = onUpload;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Handling callbacks.									*/
	/*--------------------------------------------------------------*/
	/**
	 * Handle an upload completion event. This event is called by the upload that finishes in the Flash
	 * plugin. Hence the response cannot be a delta. We register the added file with the "unseen file list", then
	 * we wait for the DomUI calling us - so we can render a proper response.
	 *
	 * @see to.etc.domui.component.upload.IUploadAcceptingComponent#handleUploadRequest(to.etc.domui.server.RequestContextImpl, to.etc.domui.state.ConversationContext)
	 */
	@Override
	public boolean handleUploadRequest(@NonNull RequestContextImpl param, @NonNull ConversationContext conversation) throws Exception {
		UploadItem[] uiar = param.getFileParameter("filedata");
		if(uiar != null) {
			for(UploadItem ui : uiar) {
				conversation.registerTempFile(ui.getFile());
				m_newItemList.add(ui);
			}
		}
		return false;								// Do not render
	}

	@Override
	public void componentHandleWebAction(@NonNull RequestContextImpl ctx, @NonNull String action) throws Exception {
		if("uploadDone".equals(action)) {
			handleUploadDone();
		} else if("queueComplete".equals(action)) {
			m_startButton.setDisabled(false);
			m_startButton.setTitle("");

			IClicked<BulkUploadButton> eh = getOnUploadsComplete();
			if(null != eh)
				eh.clicked(this);
		} else if("queueStart".equals(action)) {
			m_startButton.setDisabled(true);
			m_startButton.setTitle(Msgs.BUNDLE.getString(Msgs.BULKUPLD_DISABLED));
			IClicked<BulkUploadButton> eh = getOnUploadsStarted();
			if(null != eh)
				eh.clicked(this);
		} else
			super.componentHandleWebAction(ctx, action);
	}

	private void handleUploadDone() throws Exception {
		while(m_newItemList.size() > 0) {
			UploadItem item = m_newItemList.remove(0);
			boolean remove = true;
			IUpload onUpload = getOnUpload();
			if(onUpload != null) {
				//-- We have an upload handler. Pass the file there and be done
				try {
					remove = onUpload.fileUploaded(item);
				} finally {
					if(remove)
						item.getFile().delete();				// Discard.
					else
						m_itemList.add(item);
				}
			}
		}
	}

	/**
	 * Return the list of files uploaded to this control. This contains only those files that the {@link #getOnUpload()} handler returned
	 * "false" for, or all files if there is no onUpload handler at all.
	 * @return
	 */
	public List<UploadItem> getUploadFileList() {
		return new ArrayList<UploadItem>(m_itemList);
	}

	/**
	 * Event handler called when the file(s) have been selected and the upload of the 1st one has started.
	 * @return
	 */
	@Nullable
	public IClicked<BulkUploadButton> getOnUploadsStarted() {
		return m_onUploadsStarted;
	}

	public void setOnUploadsStarted(@Nullable IClicked<BulkUploadButton> onUploadsStarted) {
		m_onUploadsStarted = onUploadsStarted;
	}

	/**
	 * Event handler called when all uploads have completed.
	 * @return
	 */
	@Nullable
	public IClicked<BulkUploadButton> getOnUploadsComplete() {
		return m_onUploadsComplete;
	}

	public void setOnUploadsComplete(@Nullable IClicked<BulkUploadButton> onUploadsComplete) {
		m_onUploadsComplete = onUploadsComplete;
	}
}
