package to.etc.domui.component.input;

import to.etc.domui.component.meta.ClassMetaModel;
import to.etc.domui.component.meta.MetaManager;
import to.etc.domui.component.tbl.DataTable;
import to.etc.domui.component.tbl.IClickableRowRenderer;
import to.etc.domui.component.tbl.SimpleListModel;
import to.etc.domui.dom.css.DisplayType;
import to.etc.domui.dom.css.Overflow;
import to.etc.domui.dom.css.PositionType;
import to.etc.domui.dom.html.Div;
import to.etc.domui.dom.html.IForTarget;
import to.etc.domui.dom.html.Img;
import to.etc.domui.dom.html.Input;
import to.etc.domui.dom.html.NodeBase;
import to.etc.domui.dom.html.NodeContainer;
import to.etc.domui.dom.html.Span;
import to.etc.domui.util.IRenderInto;
import to.etc.domui.util.Msgs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for search-as-you-type input controls. This is not itself a control but forms the
 * basis for things that are.
 *
 * This class is an &lt;input&gt; control which can do on-the-fly lookup of data that is being typed. How
 * the lookup is done is fully transparant: the method {@link IQuery#queryFromString(String, int)} in a
 * handler interface {@link IQuery} is called, and it should return the results to show in the popup box.
 * The method gets passed the partially typed input string plus a maximal #of results to return.
 * <p>
 * <p>The control handles a specific type T, which stands for the type of object being searched for by
 * this control.</p>
 * <p>
 * <p>If the string entered by this control is just finished and enter is pressed then this control fires
 * an {@link IQuery#onEnter(String)} event with the entered string as parameter. This can then be used to
 * either locate the specific instance or a new instance can be added for this string. This means that
 * entering a string and pressing return will <b>not</b> automatically select a result from the list, if
 * shown.</p>
 * <p>If the user selects one of the results as returned by the {@link IQuery#queryFromString(String, int)}
 * method then the control will fire the {@link IQuery#onSelect(T)} event with the selected instance.</p>
 * <p>
 * <p>In both of these cases the input area of the control will be cleared, and any popup will be removed.
 * The control will be ready for another lookup/input action.</p>
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Aug 9, 2011
 */
public class SearchAsYouTypeBase<T> extends Div implements IForTarget {
	static private final int MAX_RESULTS = 7;

	static private final Set<Class<?>> SIMPLECLASSES = new HashSet<Class<?>>(Arrays.asList(String.class, Date.class, Integer.class, int.class, Long.class, long.class));

	@Nonnull final private ClassMetaModel m_dataModel;

	@Nonnull final private Class<T> m_actualType;

	@Nullable
	private List<String> m_columns;

	@Nullable
	private IQuery<T> m_handler;

	private Img m_imgWaiting = new Img("THEME/lui-keyword-wait.gif");

	private Div m_pnlSearchPopup;

	private NodeContainer m_resultMessageContainer;

	private int m_lastResultCount = -1;

	private boolean m_addSingleMatch;

	@Nullable
	private IClickableRowRenderer<T> m_rowRenderer;

	private Input m_input = new Input();

	/**
	 * Inner interface to define the query to execute to lookup data, and the handlers for
	 * completion events. Users of this control must define a handler and pass it to the
	 * control to make it work.
	 *
	 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
	 * Created on Aug 9, 2011
	 */
	public interface IQuery<T> {
		/**
		 * This gets called when it is time to lookup something. The string entered in
		 * the text field is passed. This method must return the result as a list; the list
		 * should be at most (max+1) elements big; in that case the code will show "too
		 * many results". If the list is empty it will show "no results", in all other cases
		 * it will display the result's rows to select from.
		 * <p>
		 * as an indicator.
		 *
		 * @param input
		 * @return
		 * @throws Exception
		 */
		@Nonnull
		List<T> queryFromString(@Nonnull String input, int max) throws Exception;

		/**
		 * When a literal value in the result combo is selected this will be called
		 * with that literal value. At that point the input box for this control will
		 * already have been cleared.
		 *
		 * @param instance
		 * @throws Exception
		 */
		void onSelect(@Nonnull T instance) throws Exception;

		/**
		 * When a value is entered and ENTER is pressed in the input box this gets
		 * called with the literal string entered. It can be used to either create
		 * or select some value. When called it should return true if the input box
		 * is to be cleared.
		 *
		 * @param value
		 * @throws Exception
		 */
		void onEnter(@Nonnull String value) throws Exception;
	}

	/**
	 * Create a control for the specified type, and show the specified properties in the popup list. This
	 * constructor creates an <b>incomplete</b> control: you must call {@link #setHandler(IQuery)} to completely
	 * define the control or use the SearchInput(IQuery, Class, String...) constructor.
	 *
	 * @param clz
	 * @param columns
	 */
	public SearchAsYouTypeBase(@Nonnull Class<T> clz, String... columns) {
		this(null, clz, columns);
	}

	/**
	 * Create a control for the specified type, using the handler to query and handle events.
	 *
	 * @param handler The IQUery instance which handles queries and accepts events.
	 * @param clz     The data class to display/handle
	 * @param columns The property names to show in the popup window.
	 */
	public SearchAsYouTypeBase(IQuery<T> handler, @Nonnull Class<T> clz, String... columns) {
		m_handler = handler;
		m_actualType = clz;
		m_columns = Arrays.asList(columns);
		m_dataModel = MetaManager.findClassMeta(clz);
	}

	@Override
	public void createContent() throws Exception {
		setCssClass("ui-qsi");

		//		setPosition(PositionType.RELATIVE);
		//		setDisplay(DisplayType.INLINE_BLOCK);
		m_imgWaiting.setCssClass("ui-lui-waiting");
		m_imgWaiting.setDisplay(DisplayType.NONE);
		add(m_imgWaiting);
		m_input.setCssClass("ui-lui-keyword");
		m_input.setMaxLength(40);
		m_input.setSize(14);
		add(m_input);

		m_input.setOnLookupTyping((component, done) -> handleLookupTyping(done));
	}

	@Nullable @Override public NodeBase getForTarget() {
		return m_input;
	}

	@Nullable @Override protected String getFocusID() {
		return m_input.getActualID();
	}

	/**
	 * Called on keyboard entry to handle querying or return presses.
	 */
	private void handleLookupTyping(boolean done) throws Exception {
		//-- If input is empty just clear all presentation but do not call any handler.
		String curdata = m_input.getRawValue();
		if(curdata.length() == 0) {
			showResults(null);
			return;
		}

		//-- If just enter is pressed-> call handler and be done.
		IQuery<T> handler = m_handler;
		if(done) {
			if(handler != null) {
				handler.onEnter(curdata);
			}
			clearResultPopup();
			clearResultMessage();
			m_input.setRawValue("");
			return;
		}

		//-- We need to do a query.. Ask the handler for a result
		List<T> res = null;
		if(handler != null) {
			res = handler.queryFromString(curdata, MAX_RESULTS);
			if(res.size() == 1 && m_addSingleMatch) {
				handleSelectValueFromPopup(res.get(0));
				res = null;
			}
		}
		showResults(res);
	}

	/**
	 * Show the results of a lookup query. The parameter can have the following values:
	 * <ul>
	 * <li>null: this clears all search presentation; it means that a query is not necessary/possible. It does <b>not</b> mean
	 * that there are no results!</li>
	 * <li>empty list: indicates that a query returned no results. This will cause the control to display the "no query results" presentation.</li>
	 * <li>List with too many items: if the list contains more that max items the control will show the "too many results" presentation.</li>
	 * <li>List with &lt;= max items: all of the items will be shown in a selection popup; the user can select one with mouse or keyboard.</li>
	 * </ul>
	 */
	private void showResults(@Nullable List<T> isl) throws Exception {
		if(null == isl) {
			//-- Null means: there is no query entered at all. Remove both popup and message panels.
			clearResultMessage();
			clearResultPopup();
			return;
		}

		int rc = isl.size();
		System.out.println("search: count=" + rc);
		if(rc == 0) {
			if(m_lastResultCount == 0)
				return;
			setResultMessage("ui-lui-keyword-no-res", Msgs.BUNDLE.getString(Msgs.UI_KEYWORD_SEARCH_NO_MATCH));
			return;
		}
		if(rc > MAX_RESULTS) {
			if(m_lastResultCount > MAX_RESULTS)
				return;
			setResultMessage("ui-lui-keyword-large", Msgs.BUNDLE.formatMessage(Msgs.UI_KEYWORD_SEARCH_LARGE_MATCH, "" + MAX_RESULTS));
			return;
		}
		clearResultMessage();

		//-- Create the result list popup.
		if(m_pnlSearchPopup == null)
			m_pnlSearchPopup = new Div();
		else
			m_pnlSearchPopup.removeAllChildren();
		if(!m_pnlSearchPopup.isAttached()) {
			add(m_pnlSearchPopup);
			m_pnlSearchPopup.setCssClass("ui-lui-keyword-popup");
			m_pnlSearchPopup.setPosition(PositionType.ABSOLUTE);
			m_pnlSearchPopup.setZIndex(10);
		}

		SimpleListModel<T> mdl = new SimpleListModel<T>(isl);
		IClickableRowRenderer<T> rr = calculateRenderer();
		rr.setRowClicked(val -> handleSelectValueFromPopup(val));
		DataTable<T> tbl = new DataTable<>(mdl, rr);
		m_pnlSearchPopup.add(tbl);
		tbl.setWidth("100%");
		tbl.setOverflow(Overflow.HIDDEN);
		tbl.setPosition(PositionType.RELATIVE);
	}

	private IClickableRowRenderer<T> calculateRenderer() {
		IClickableRowRenderer<T> rowr = m_rowRenderer;
		if(null != rowr)
			return rowr;
		KeyWordPopupRowRenderer<T> rr = new KeyWordPopupRowRenderer<T>(m_dataModel);

		if(isSimpleType()) {
			rr.addColumns("", (IRenderInto<Object>) (node, object) -> node.add(String.valueOf(object)));
		} else {
			List<String> columns = m_columns;
			if(columns != null && columns.size() > 0) {
				for(String column : columns) {
					rr.addColumn(column);
				}
			}
		}
		return rr;
	}

	/**
	 * T if the actual type is a simple type like String or numeric.
	 */
	protected boolean isSimpleType() {
		return SIMPLECLASSES.contains(m_actualType);
	}

	private void handleSelectValueFromPopup(T val) throws Exception {
		System.out.println("GOT: " + val);
		if(null != m_handler) {
			m_handler.onSelect(val);
		}

		clearResultMessage();
		clearResultPopup();
		m_input.setRawValue("");
		m_input.setFocus();
	}

	private void clearResultPopup() {
		if(null != m_pnlSearchPopup && m_pnlSearchPopup.isAttached())
			m_pnlSearchPopup.remove();
	}

	/**
	 * Set a result count indicator field, using the specified text and the specified css class. If
	 * the field is already present it is updated, else it is created.
	 */
	private void setResultMessage(String css, String text) {
		if(m_resultMessageContainer == null)
			m_resultMessageContainer = new Span();
		m_resultMessageContainer.setCssClass(css);
		m_resultMessageContainer.setText(text);
		if(!m_resultMessageContainer.isAttached())
			add(m_resultMessageContainer);
		clearResultPopup();
	}

	private void clearResultMessage() {
		if(m_resultMessageContainer != null && m_resultMessageContainer.isAttached())
			m_resultMessageContainer.remove();
	}

	/**
	 * Get the current query/event handler for this control.
	 */
	public IQuery<T> getHandler() {
		return m_handler;
	}

	public void setHandler(IQuery<T> handler) {
		m_handler = handler;
	}

	public boolean isAddSingleMatch() {
		return m_addSingleMatch;
	}

	public void setAddSingleMatch(boolean addSingleMatch) {
		m_addSingleMatch = addSingleMatch;
	}

	@Nonnull public Class<T> getActualType() {
		return m_actualType;
	}

	@Nonnull public ClassMetaModel getDataModel() {
		return m_dataModel;
	}

	public boolean isReadOnly() {
		return m_input.isReadOnly();
	}

	public void setReadOnly(boolean ro) {
		m_input.setReadOnly(ro);
	}

	public boolean isDisabled() {
		return m_input.isDisabled();
	}

	public void setDisabled(boolean d) {
		m_input.setDisabled(d);
	}

	@Nullable public IClickableRowRenderer<T> getRowRenderer() {
		return m_rowRenderer;
	}

	public void setRowRenderer(@Nullable IClickableRowRenderer<T> rowRenderer) {
		m_rowRenderer = rowRenderer;
	}
}
