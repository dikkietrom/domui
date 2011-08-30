/*
 * DomUI Java User Interface library
 * Copyright (c) 2010 by Frits Jalvingh, Itris B.V.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * See the "sponsors" file for a list of supporters.
 *
 * The latest version of DomUI and related code, support and documentation
 * can be found at http://www.domui.org/
 * The contact for the project is Frits Jalvingh <jal@etc.to>.
 */
package to.etc.domui.dom.html;

import java.util.*;

import javax.annotation.*;

import to.etc.domui.component.layout.*;
import to.etc.domui.component.misc.*;
import to.etc.domui.dom.errors.*;
import to.etc.domui.dom.header.*;
import to.etc.domui.server.*;
import to.etc.domui.state.*;
import to.etc.domui.util.*;
import to.etc.webapp.nls.*;
import to.etc.webapp.query.*;

/**
 * This is the main owner of all nodes; this represents all that is needed for a
 * page to render. All nodes that are (indirectly) attached to the page directly
 * connect here. The page maintains a full ident map to all components currently
 * reachable on the page. In addition the page assigns IDs to nodes that have no
 * ID (or a duplicate ID).
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Aug 18, 2007
 */
final public class Page implements IQContextContainer {
	/** Next ID# for unidded nodes. */
	private int m_nextID = 1;

	/** The unique, random page ID generated to check for session expired problems */
	private final int m_pageTag;

	/** Temp work buffer to prevent lots of allocations. */
	private StringBuilder m_sb;

	/**
	 * The set of parameters that was used at page creation time.
	 */
	private PageParameters m_pageParameters;

	private ConversationContext m_cc;

	//	private boolean					m_built;

	private final Map<String, NodeBase> m_nodeMap = new HashMap<String, NodeBase>(127);

	private Map<String, NodeBase> m_beforeMap;

	/**
	 * Contains the header contributors in the order that they were added.
	 */
	private List<HeaderContributorEntry> m_orderedContributorList = Collections.EMPTY_LIST;

	/**
	 * As soon as header contributor are rendered to the browser this gets set to the
	 * length of the list rendered out. When new contributors are added by components
	 * this gets seen because the list is bigger than this index; we need to render
	 * from this index till end-of-list to include the contributors.
	 */
	private int m_lastContributorIndex;

	/**
	 * Set containing the same header contributors, but in a fast-lookup format.
	 */
	private Set<HeaderContributor> m_headerContributorSet;

	private StringBuilder m_appendJS;

	/** Number of exceptions in-a-row on a full render of this page. */
	private int m_pageExceptionCount;

	/** Set to T if an initial full render of the page completed OK. */
	private boolean m_fullRenderCompleted;

	private final UrlPage m_rootContent;

	/** The component that needs to be focused. This is null if no explicit focus request was done. */
	private NodeBase m_focusComponent;

	/**
	 * If a "pop-in" is present this contains the reference to it.
	 */
	private NodeContainer m_currentPopIn;

	private Map<String, Object> m_pageData = Collections.EMPTY_MAP;

	private boolean m_allowVectorGraphics;

	/**
	 * Contains all nodes that were added or marked as forceRebuild <i>during</i> the BUILD
	 * phase. If this set is non-empty after a build loop then the loop needs to repeat the
	 * build for the nodes and their subtrees in here. See bug 688.
	 */
	private Set<NodeBase> m_pendingBuildSet = new HashSet<NodeBase>();

	/**
	 * When calling user handlers on nodes this will keep track of the node the handler was
	 * called on. If that node becomes somehow removed it will move upward to it's parent,
	 * so that it points to an on-screen node always. This is needed for error handling.
	 */
	private NodeBase m_theCurrentNode;

	/**
	 * When set, the page will be rendered as XHTML. This is experimental, and used to implement
	 * the SVG/VML graphic flow editor.
	 */
	private boolean m_renderAsXHTML;

	/**
	 * The stack of floating windows on top of the main canvas, in ZIndex order.
	 */
	private List<FloatingDiv> m_floatingWindowStack;

	/**
	 * This gets incremented for every request that is handled.
	 */
	private int m_requestCounter;

	/**
	 * Nodes that are added to a render and that are removed by the Javascript framework are added here; this
	 * will force them to be removed from the tree after any render without causing a delta.
	 */
	private List<NodeBase> m_removeAfterRenderList = Collections.EMPTY_LIST;

	public Page(final UrlPage pageContent) throws Exception {
		m_pageTag = DomApplication.internalNextPageTag(); // Unique page ID.
		m_rootContent = pageContent;
		registerNode(pageContent); // First node.
		pageContent.internalSetTag("body"); // Override it's tagname
		pageContent.setErrorFence(); // The body ALWAYS accepts ANY errors.

		//-- Localize calendar resources
		String res = DomApplication.get().findLocalizedResourceName("$js/calendarnls", ".js", NlsContext.getLocale());
		if(res == null)
			throw new IllegalStateException("internal: missing calendar NLS resource $js/calendarnls{nls}.js");
		addHeaderContributor(HeaderContributor.loadJavascript(res), -760);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Initialization.										*/
	/*--------------------------------------------------------------*/
	/**
	 * Assign required data to the page.
	 * @param pp
	 * @param cc
	 */
	final public void internalInitialize(final PageParameters pp, final ConversationContext cc) {
		if(pp == null)
			throw new IllegalStateException("Internal: Page parameters cannot be null here");
		if(cc == null)
			throw new IllegalStateException("Internal: ConversationContext cannot be null here");

		m_cc = cc;
		m_pageParameters = pp;
		pp.setReadOnly();
	}

	public void setTheCurrentNode(NodeBase b) {
		if(b != null && b.getPage() != this)
			throw new IllegalStateException("The node is not part of this page!");
		m_theCurrentNode = b;
	}

	public NodeBase getTheCurrentNode() {
		return m_theCurrentNode;
	}

	/**
	 * This tries to locate the control that the "theCurrentNode" is associated with. If no control
	 * can be found it returns the node verbatim.
	 * @return
	 */
	public NodeBase getTheCurrentControl() {
		//-- Locate the best encapsulating control if possible.
		NodeBase nb = getTheCurrentNode();
		while(nb != null && !(nb instanceof IControl< ? >)) {
			nb = nb.getParent();
		}
		return nb != null ? nb : getTheCurrentNode();
	}

	public Map<String, NodeBase> internalNodeMap() {
		return m_nodeMap;
	}

	private StringBuilder sb() {
		if(m_sb == null)
			m_sb = new StringBuilder(64);
		else
			m_sb.setLength(0);
		return m_sb;
	}

	public DomApplication getApplication() {
		return DomApplication.get();
	}

	public PageParameters getPageParameters() {
		return m_pageParameters;
	}

	public int getRequestCounter() {
		return m_requestCounter;
	}

	public void internalIncrementRequestCounter() {
		m_requestCounter++;
	}

	public final int getPageTag() {
		return m_pageTag;
	}

	/**
	 * Calculates a new ID for a node.
	 * @return
	 */
	final String nextID() {
		StringBuilder sb = sb();
		sb.append("_");
		int id = m_nextID++;
		while(id != 0) {
			int d = id % 36;
			if(d <= 9)
				d = d + '0';
			else
				d = ('A' + (d - 10));
			sb.append((char) d);
			id = id / 36;
		}
		return sb.toString();
	}

	/**
	 * Registers the node with this page. If the node has no ID or the ID is invalid then
	 * a new ID is assigned.
	 * @param n
	 */
	final void registerNode(final NodeBase n) {
		if(n.getPage() != null)
			throw new IllegalStateException("Node still attached to other page");

		/*
		 * jal 20081211 If a node already has an ID reuse that if not currently assigned. This should fix
		 * the following bug in drag and drop: a dropped node is REMOVED, then ADDED to another node (2 ops).
		 * This would reassign an ID, causing the delta to be rendered with a delete of the NEW ID instead
		 * of the old ID.
		 */
		String id = n.internalGetID();
		if(id != null) {
			if(m_nodeMap.containsKey(id)) { // Duplicate key?
				id = nextID(); // Assign new ID
				n.setActualID(id); // Save in node.
			}
		} else {
			//-- Assign new ID
			id = nextID(); // Assign new ID
			n.setActualID(id); // Save in node.
		}
		if(null != m_nodeMap.put(id, n))
			throw new IllegalStateException("Duplicate node ID '" + id + "'!?!?");
		n.setPage(this);
		n.onHeaderContributors(this); // Ask the node for it's header contributors.
		n.internalOnAddedToPage(this);
		if(n.isFocusRequested()) {
			setFocusComponent(n);
			n.clearFocusRequested();
		}
		internalAddPendingBuild(n);

		//-- Experimental fix for bug# 787: cannot locate error fence. Allow errors to be posted on disconnected nodes.
		if(n.getMessage() != null) {
			IErrorFence fence = DomUtil.getMessageFence(n); // Get the fence that'll handle the message by looking UPWARDS in the tree
			fence.addMessage(n, n.getMessage());
		}
	}

	/**
	 * Removes this node from the IDmap.
	 * @param n
	 */
	final void unregisterNode(final NodeBase n) {
		if(n.getPage() != this)
			throw new IllegalStateException("This node does not belong to this page!?");
		if(n.getActualID() == null)
			throw new IllegalStateException("This-node's actual ID has gone!?");
		if(m_theCurrentNode == n)
			m_theCurrentNode = n.getParent();
		n.internalOnRemoveFromPage(this);
		n.setPage(null);
		if(m_nodeMap.remove(n.getActualID()) == null)
			throw new IllegalStateException("The node with ID=" + n.getActualID() + " was not found!?");
		m_pendingBuildSet.remove(n); // ?? Needed?
	}

	public NodeBase findNodeByID(final String id) {
		return m_nodeMap.get(id);
	}

	/*
	 * When components itself are changed at application-invocation time we
	 * have two different possibilities:
	 * 1. 	component attributes are changed. The change is detected by the renderer
	 * 		for the component which keeps the "previous" values of the properties. At
	 * 		tree render time only the changed attributes are sent as a change list.
	 * 2.	The tree structure changes because components are moved, added or deleted.
	 *
	 * This code handles case 2. To prevent us from always having to create a before
	 * image all calls that change the tree (removeComponent, addComponent) call
	 * a signal function here. Only when that function gets called (the 1st time) will
	 * a copy of the before-tree be made. This copy only encapsulates the structure;
	 * the components themselves are not copied.
	 * The existence of the before-structure will indicate that a full tree delta is
	 * to be done at response time.
	 */

	/**
	 * Called by all methods that change this tree. As soon as this gets called
	 * it checks to see if a before-image is present. If not then it gets created
	 * so that the structure before the changes is maintained.
	 */
	final protected void copyIdMap() {
		if(m_beforeMap != null)
			return;
		m_beforeMap = new HashMap<String, NodeBase>(m_nodeMap);
	}

	final public Map<String, NodeBase> getBeforeMap() {
		return m_beforeMap;
	}

	public void internalClearDeltaFully() {
		for(NodeBase nb : m_removeAfterRenderList) {
			nb.remove();
		}
		m_removeAfterRenderList.clear();

		getBody().internalClearDeltaFully();
		m_beforeMap = null;
		m_sb = null;
	}


	public void addRemoveAfterRenderNode(NodeBase node) {
		if(m_removeAfterRenderList == Collections.EMPTY_LIST) {
			m_removeAfterRenderList = new ArrayList<NodeBase>();
		}
		m_removeAfterRenderList.add(node);
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Header contributors									*/
	/*--------------------------------------------------------------*/
	/**
	 * Call from within the onHeaderContributor call on a node to register any header
	 * contributors needed by a node.
	 * @param hc
	 */
	final public void addHeaderContributor(final HeaderContributor hc, int order) {
		if(m_headerContributorSet == null) {
			m_headerContributorSet = new HashSet<HeaderContributor>(30);
			m_orderedContributorList = new ArrayList<HeaderContributorEntry>(30);
			m_headerContributorSet.add(hc);
			m_orderedContributorList.add(new HeaderContributorEntry(hc, order));
			return;
		}
		if(m_headerContributorSet.contains(hc)) // Already registered?
			return;
		m_headerContributorSet.add(hc);
		m_orderedContributorList.add(new HeaderContributorEntry(hc, order));
	}

	public synchronized void internalAddContributors(List<HeaderContributorEntry> full) {
		full.addAll(m_orderedContributorList);
	}

	public List<HeaderContributorEntry> getHeaderContributorList() {
		return new ArrayList<HeaderContributorEntry>(m_orderedContributorList);
	}

	public List<HeaderContributorEntry> getAddedContributors() {
		if(m_orderedContributorList == null || m_lastContributorIndex >= m_orderedContributorList.size())
			return Collections.EMPTY_LIST;
		return new ArrayList<HeaderContributorEntry>(m_orderedContributorList.subList(m_lastContributorIndex, m_orderedContributorList.size()));
	}

	public void internalContributorsRendered() {
		m_lastContributorIndex = m_orderedContributorList == null ? 0 : m_orderedContributorList.size();
	}

	/**
	 * Return the BODY component for this page.
	 * @return
	 */
	public UrlPage getBody() {
		return m_rootContent;
	}

	public <T> void setData(final T inst) {
		if(m_pageData == Collections.EMPTY_MAP)
			m_pageData = new HashMap<String, Object>();
		m_pageData.put(inst.getClass().getName(), inst);
	}

	public <T> T getData(final Class<T> clz) {
		return (T) m_pageData.get(clz.getName());
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Handle the floating window stack.					*/
	/*--------------------------------------------------------------*/


	/**
	 * Add a floating thing to the floater stack.
	 * @param originalParent
	 * @param in
	 */
	void internalAddFloater(NodeContainer originalParent, NodeBase in) {
		//-- Sanity checks.
		if(!(in instanceof FloatingDiv))
			throw new IllegalStateException("Floaters can only be FloatingDiv-derived, and " + in + " is not.");
		if(getBody() == null)
			throw new IllegalStateException("Ehm- I have no body?"); // Existential problems are the hardest...

		//-- Be very sure it's not already in the stack
		final FloatingDiv window = (FloatingDiv) in;
		for(FloatingDiv fr : getFloatingStack()) {
			if(fr == window)
				return;
		}

		//-- It needs to be added. Calculate the zIndex to use; calculate a Z-Index that is higher than the current "topmost" window.
		int zindex = 100;
		for(FloatingDiv fr : getFloatingStack()) {
			if(fr.getZIndex() >= zindex)
				zindex = fr.getZIndex() + 100;
		}
		window.setZIndex(zindex);
		//		System.out.println("New floater got zIndex=" + zindex);

		//-- If this is MODAL create a hider for it.
		if(window.isModal()) {
			Div hider = new Div();
			getBody().add(hider);
			hider.setCssClass("ui-flw-hider");
			hider.setZIndex(zindex - 1); // Just below the new floater.
			window.internalSetHider(hider);

			//-- Add a click handler which will close the floater when the hider div is clicked.
			hider.setClicked(new IClicked<NodeBase>() {
				@Override
				public void clicked(NodeBase clickednode) throws Exception {
					window.closePressed();
				}
			});
		}
		getFloatingStack().add(window); // Add on top (defines order)

		//-- Add the floater to the body,
		getBody().internalAdd(Integer.MAX_VALUE, window); // Add to body,
	}

	/**
	 * Callback called by a floating window when it is removed from the page.
	 * @param floater
	 */
	public void internalRemoveFloater(FloatingDiv floater) {
		if(!getFloatingStack().remove(floater)) // If already removed exit
			return;
		if(floater.internalGetHider() != null) {
			floater.internalGetHider().remove();
			floater.internalSetHider(null);
		}
	}

	private List<FloatingDiv> getFloatingStack() {
		if(m_floatingWindowStack == null)
			m_floatingWindowStack = new ArrayList<FloatingDiv>();
		return m_floatingWindowStack;
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	BUILD phase coding (see bug 688).					*/
	/*--------------------------------------------------------------*/
//	/**
//	 * @throws Exception
//	 */
//	public void build() throws Exception {
//		getBody().build();
//	}

	void internalAddPendingBuild(NodeBase n) {
		m_pendingBuildSet.add(n);
	}

	/**
	 * This handles the BUILD phase for a FULL page render.
	 * @throws Exception
	 */
	public void internalFullBuild() throws Exception {
		m_pendingBuildSet.clear();
		buildSubTree(getBody());
		rebuildLoop();
	}

	/**
	 * This handles the BUILD phase for the DELTA build. It walks only the nodes that
	 * are marked as changed initially and does not descend subtrees that are unchanged.
	 *
	 * @throws Exception
	 */
	public void internalDeltaBuild() throws Exception {
		m_pendingBuildSet.clear();
		buildChangedTree(getBody());
//		buildSubTree(getBody());
		rebuildLoop();
	}

	/**
	 * Loop over the changed-nodeset until it stays empty.
	 * @throws Exception
	 */
	private void rebuildLoop() throws Exception {
		int tries = 0;
		while(m_pendingBuildSet.size() > 0) {
			if(tries++ > 10)
				throw new IllegalStateException("Internal: building the tree failed after " + tries + " attempts: the tree keeps changing every build....");
			NodeBase[] todo = m_pendingBuildSet.toArray(new NodeBase[m_pendingBuildSet.size()]); // Dup todolist,
			m_pendingBuildSet.clear();
			for(NodeBase nd : todo)
				buildSubTree(nd);
		}
	}

	/**
	 * Call 'build' on all subtree nodes and reset all rebuild markers in the set code.
	 * @param nd
	 * @throws Exception
	 */
	private void buildSubTree(NodeBase nd) throws Exception {
		nd.build();
		m_pendingBuildSet.remove(nd); // We're building this dude.
		if(!(nd instanceof NodeContainer))
			return;
		NodeContainer nc = (NodeContainer) nd;
		for(int i = 0, len = nc.getChildCount(); i < len; i++) {
			buildSubTree(nc.getChild(i));
		}
	}

	private void buildChangedTree(NodeBase nd) throws Exception {
		m_pendingBuildSet.remove(nd);
		if(!(nd instanceof NodeContainer)) {
			//-- NodeBase only- simple; always rebuild.
			nd.build();
			return;
		}
		NodeContainer nc = (NodeContainer) nd;
		if(nc.childHasUpdates() && nc.internalGetOldChildren() == null) {
			nc.build();
			for(int i = 0, len = nc.getChildCount(); i < len; i++) {
				buildChangedTree(nc.getChild(i));
			}
		}
		if(nc.internalGetOldChildren() != null || nc.childHasUpdates() || nc.mustRenderChildrenFully()) {
			buildSubTree(nc);
		}
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Per-request Javascript handling.					*/
	/*--------------------------------------------------------------*/
	/*
	 * The code below allows Javascript to be added for every event roundtrip. All of the
	 * javascript added gets executed /after/ all of the DOM delta has been executed by
	 * the browser.
	 */
	/**
	 * Add a Javascript statement (MUST be a valid, semicolon-terminated statement or statement list) to
	 * execute on return to the browser (once).
	 */
	public void appendJS(final CharSequence sq) {
		if(m_appendJS == null)
			m_appendJS = new StringBuilder(sq.length() + 100);
		m_appendJS.append(sq);
	}

	public StringBuilder internalGetAppendedJS() {
		StringBuilder sb = m_appendJS;
		m_appendJS = null;
		return sb;
	}

	/**
	 * DEPRECATED: Should use {@link DomUtil#createOpenWindowJS(String, WindowParameters)}.
	 *
	 * Force the browser to open a new window with a user-specified URL. The new window does NOT
	 * inherit any DomUI session data, of course, and has no WindowSession. After creation the
	 * window cannot be manipulated by DomUI code.
	 *
	 * @param windowURL	The url to open. If this is a relative path it will get the webapp
	 * 					context appended to it.
	 * @param wp
	 */
	@Deprecated
	public void openWindow(@Nonnull String windowURL, @Nullable WindowParameters wp) {
		if(windowURL == null || windowURL.length() == 0)
			throw new IllegalArgumentException("Empty window URL");
		String js = DomUtil.createOpenWindowJS(DomUtil.calculateURL(UIContext.getRequestContext(), windowURL), wp);
		appendJS(js);
	}

	/**
	 * DEPRECATED: Should use {@link DomUtil#createOpenWindowJS(Class, PageParameters, WindowParameters)}.
	 * Open a DomUI page in a separate browser popup window. This window will create it's own WindowSession.
	 * FIXME URGENT This code needs to CREATE the window session BEFORE referring to it!!!!
	 *
	 * @param clz
	 * @param pp
	 * @param wp
	 */
	@Deprecated
	public void openWindow(@Nonnull Class< ? extends UrlPage> clz, @Nullable PageParameters pp, @Nullable WindowParameters wp) {
		String js = DomUtil.createOpenWindowJS(clz, pp, wp);
		appendJS(js);
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Component focus handling.							*/
	/*--------------------------------------------------------------*/
	/**
	 * Return the component that currently has a focus request.
	 * @return
	 */
	public NodeBase getFocusComponent() {
		return m_focusComponent;
	}

	public void setFocusComponent(final NodeBase focusComponent) {
		m_focusComponent = focusComponent;
	}

	//	/**
	//	 * Walks the component tree, and makes the first input component hold the focus.
	//	 */
	//	public void	focusFirstInput() {
	//		focusFirstInput(getBody());
	//	}
	//	private boolean focusFirstInput(NodeBase b) {
	//		System.out.println("FFN: "+b);
	//		if(b instanceof IInputNode) {
	//			b.setFocus();
	//			return true;
	//		}
	//
	//		if(b instanceof NodeContainer) {
	//			NodeContainer nc = (NodeContainer) b;
	//			for(NodeBase ch: nc) {
	//				if(focusFirstInput(ch))
	//					return true;
	//			}
	//		}
	//		return false;
	//	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Context handling code.								*/
	/*--------------------------------------------------------------*/
	public ConversationContext getConversation() {
		if(m_cc == null)
			throw new IllegalStateException("The conversational context is null??????");
		m_cc.checkAttached();
		return m_cc;
	}

	public ConversationContext internalGetConversation() {
		return m_cc;
	}

	public int getPageExceptionCount() {
		return m_pageExceptionCount;
	}

	public void setPageExceptionCount(final int pageExceptionCount) {
		m_pageExceptionCount = pageExceptionCount;
	}

	public boolean isFullRenderCompleted() {
		return m_fullRenderCompleted;
	}

	public void setFullRenderCompleted(final boolean fullRenderCompleted) {
		m_fullRenderCompleted = fullRenderCompleted;
	}

	/** Temp for checking shelve order. */
	private boolean m_shelved;

	/**
	 * Call all onShelve() handlers on all attached components.
	 * @throws Exception
	 */
	public void internalShelve() throws Exception {
		if(m_shelved)
			throw new IllegalStateException("Calling SHELVE on already-shelved page " + this);
		m_shelved = true;
		getBody().internalShelve();
	}

	/**
	 * Call all unshelve handlers on all attached components.
	 *
	 * @throws Exception
	 */
	public void internalUnshelve() throws Exception {
		if(!m_shelved)
			throw new IllegalStateException("Calling UNSHELVE on already-unshelved page " + this);
		m_shelved = false;
		getBody().internalUnshelve();
	}

	public boolean isShelved() {
		return m_shelved;
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Pop-in support.										*/
	/*--------------------------------------------------------------*/
	/**
	 * This sets a new pop-in. This does NOT add the popin to the tree, that
	 * must be done manually.
	 *
	 * @param pin
	 */
	public void setPopIn(final NodeContainer pin) {
		if(m_currentPopIn != null && m_currentPopIn != pin) {
			NodeContainer old = m_currentPopIn;
			m_currentPopIn = null;
			old.remove();
		}
		m_currentPopIn = pin;
	}

	/**
	 * Remove any pending pop-in.
	 */
	public void clearPopIn() {
		if(m_currentPopIn != null) {
			NodeContainer old = m_currentPopIn;
			m_currentPopIn = null;
			old.remove();
		}
	}

	public NodeContainer getPopIn() {
		return m_currentPopIn;
	}

	@Override
	public String toString() {
		return "Page[" + getBody().getClass().getName() + "]";
	}

	public boolean isDestroyed() {
		return m_cc != null && !m_cc.isValid();
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	IQContextContainer implementation.					*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 */
	@Override
	public QDataContext internalGetSharedContext() {
		return getConversation().internalGetSharedContext();
	}

	/**
	 *
	 * @see to.etc.webapp.query.IQContextContainer#internalSetSharedContext(to.etc.webapp.query.QDataContext)
	 */
	@Override
	public void internalSetSharedContext(final QDataContext c) {
		getConversation().internalSetSharedContext(c);
	}

	@Override
	public QDataContextFactory internalGetDataContextFactory() {
		return getConversation().internalGetDataContextFactory();
	}

	@Override
	public void internalSetDataContextFactory(final QDataContextFactory s) {
		getConversation().internalSetDataContextFactory(s);
	}

	public boolean isAllowVectorGraphics() {
		return m_allowVectorGraphics;
	}

	public void setAllowVectorGraphics(boolean allowVectorGraphics) {
		m_allowVectorGraphics = allowVectorGraphics;
	}

	public boolean isRenderAsXHTML() {
		return m_renderAsXHTML;
	}

	public void setRenderAsXHTML(boolean renderAsXHTML) {
		m_renderAsXHTML = renderAsXHTML;
	}
}
