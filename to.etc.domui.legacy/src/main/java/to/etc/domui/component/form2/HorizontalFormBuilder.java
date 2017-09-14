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
package to.etc.domui.component.form2;

import javax.annotation.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.dom.html.*;

public class HorizontalFormBuilder extends TableFormBuilder {
	private TR m_labelRow;

	private TR m_editRow;

	static private enum TableMode {
		perRow, perForm
	}

	private TableMode m_tableMode = TableMode.perForm;

	public HorizontalFormBuilder(@Nonnull IAppender a) {
		super(a);
	}

	public HorizontalFormBuilder(@Nonnull NodeContainer target) {
		super(target);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Simple helpers										*/
	/*--------------------------------------------------------------*/
	/**
	 * USE WITH CARE - this exposes the raw table interface.
	 * @see to.etc.domui.component.form.GenericTableFormBuilder#addCell()
	 */
	@Override
	@Nonnull
	public TD addCell() {
		return addCell(null, 1, 2);
	}

	/**
	 * USE WITH CARE - this exposes the raw table interface.
	 *
	 * @param colSpan
	 * @param rowSpan
	 * @return
	 */
	@Nonnull
	public TD addCell(int colSpan, int rowSpan) {
		return addCell(null, colSpan, rowSpan);
	}

	/**
	 * USE WITH CARE - this exposes the raw table interface.
	 * @param css
	 * @param colSpan
	 * @param rowSpan
	 * @return
	 */
	@Nonnull
	public TD addCell(String css, int colSpan, int rowSpan) {
		TR tr = getLabelRow();
		TD td = tr.addCell(css);
		if(colSpan > 1) {
			td.setColspan(colSpan);
		}
		if(rowSpan > 1) {
			td.setRowspan(rowSpan);
		}
		return td;
	}

	/**
	 * @return
	 */
	@Nonnull
	public TR getLabelRow() {
		checkRows();
		return m_labelRow;
	}

	@Nonnull
	public TR getEditRow() {
		checkRows();
		return m_editRow;
	}

	/**
	 * Ensure that a label row and an edit row are both available, create 'm if needed.
	 */
	private void checkRows() {
		if(m_labelRow != null && m_editRow != null) // If either is null recreate both of 'm
			return;

		//-- If we're in multitable mode we might need to have to create a new table...
		if(m_tableMode == TableMode.perRow) {
			if(getTable() == null)
				reset(); // Make sure everything is clear
			else {
				if(getTBody() == null)
					tbody();
				else {
					//-- We have an existing table and body... The body must be empty or we need to create new table and body.
					if(tbody().getChildCount() > 0) {
						reset();
					}
				}
			}
		}

		m_labelRow = addRow();
		m_editRow = addRow();
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Main workhorses for genericly adding controls.		*/
	/*--------------------------------------------------------------*/

	@Override
	public void addControl(@Nullable NodeBase label, @Nullable NodeBase labelnode, @Nonnull NodeBase[] list, boolean mandatory, boolean editable, PropertyMetaModel< ? > pmm) {
		modalAdd(label, list, editable);
		clearRun();
	}

	@Override
	public void addContent(@Nullable NodeBase label, @Nonnull NodeBase[] control, boolean editable) {
		modalAdd(label, control, editable);
		clearRun();
	}

	@Override
	protected void startBulkLayout() {}

	@Override
	protected void endBulkLayout() {
		clearRun();
	}


	/**
	 * Adds the presentation for a label AND a control to the form.
	 * @param l
	 * @param list
	 */
	private void modalAdd(@Nullable NodeBase l, @Nonnull NodeBase[] list, boolean editable) {
		TR tr = getLabelRow(); // Row containing zhe labelz.
		TD td = tr.addCell(); // Create cell for label;
		td.setCssClass(m_labelClass == null ? m_defaultLabelClass : m_labelClass);
		if(null != l)
			td.add(l);
		if(m_labelColSpan > 1)
			td.setColspan(m_labelColSpan);
		if(m_labelRowSpan > 1)
			td.setRowspan(m_labelRowSpan);
		if(m_labelNowrap != null)
			td.setNowrap(m_labelNowrap.booleanValue());
		if(m_labelWidth != null)
			td.setWidth(m_labelWidth);

		tr = getEditRow();
		td = tr.addCell();
		String css = editable ? m_defaultControlClass : "ui-fvs-do";
		if(m_controlClass != null)
			css = m_controlClass;

		td.setCssClass(css);
		if(m_controlColSpan > 1)
			td.setColspan(m_controlColSpan);
		if(m_controlRowSpan > 1)
			td.setRowspan(m_controlRowSpan);
		if(m_controlNoWrap != null)
			td.setNowrap(m_controlNoWrap.booleanValue());
		if(m_controlWidth != null)
			td.setWidth(m_controlWidth);

		for(NodeBase nb : list)
			td.add(nb);
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Layouter code.										*/
	/*--------------------------------------------------------------*/

	@Override
	protected void internalClearLocation() {
		m_labelRow = null;
		m_editRow = null;
	}

	/**
	 * Start a new row of input fields.
	 */
	public void nl() {
		m_labelRow = null;
		m_editRow = null;
		if(m_tableMode == TableMode.perRow)
			reset();
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Layouter configuration.								*/
	/*--------------------------------------------------------------*/
	/** Next row span to use. */
	//	private int m_nextRowSpan = 1;
	//
	//	private int m_nextColSpan = 1;

	private int m_labelRowSpan = 1;

	private int m_controlRowSpan = 1;

	private int m_labelColSpan = 1;

	private int m_controlColSpan = 1;

	private String m_defaultLabelClass = "ui-fvs-lbl";

	private String m_defaultControlClass = "ui-fvs-in";

	private String m_labelClass, m_controlClass;

	private String m_labelWidth, m_controlWidth;

	private Boolean m_labelNowrap, m_controlNoWrap;

	private void clearRun() {
		m_labelColSpan = 1;
		m_labelRowSpan = 1;
		m_controlColSpan = 1;
		m_controlRowSpan = 1;
		m_labelClass = null;
		m_controlClass = null;
		m_labelWidth = null;
		m_controlWidth = null;
		m_labelNowrap = null;
		m_controlNoWrap = null;
	}

	/**
	 * Set the colspan for both label and control for all controls added after this call.
	 * @param x
	 * @return
	 */
	@Nonnull
	public HorizontalFormBuilder colSpan(int x) {
		if(x < 0)
			throw new IllegalArgumentException("colspan " + x + " must be > 0");
		m_labelColSpan = x;
		m_controlColSpan = x;
		return this;
	}

	/**
	 * Set the rowspan for both label and control for all controls added after this call.
	 * @param x
	 * @return
	 */
	@Nonnull
	public HorizontalFormBuilder rowSpan(int x) {
		if(x < 0)
			throw new IllegalArgumentException("rowspan " + x + " must be > 0");
		m_labelRowSpan = x;
		m_controlRowSpan = x;
		return this;
	}

	/**
	 * Set the colspan for only the label for all controls added after this call.
	 * @param x
	 * @return
	 */
	@Nonnull
	public HorizontalFormBuilder labelColSpan(int x) {
		if(x < 0)
			throw new IllegalArgumentException("colspan " + x + " must be > 0");
		m_labelColSpan = x;
		return this;
	}

	/**
	 * Set the rowspan for only the label for all controls added after this call.
	 * @param x
	 * @return
	 */
	@Nonnull
	public HorizontalFormBuilder labelRowSpan(int x) {
		if(x < 0)
			throw new IllegalArgumentException("rowspan " + x + " must be > 0");
		m_labelRowSpan = x;
		return this;
	}

	/**
	 * Set the colspan for only the control for all controls added after this call.
	 * @param x
	 * @return
	 */
	@Nonnull
	public HorizontalFormBuilder controlColSpan(int x) {
		if(x < 0)
			throw new IllegalArgumentException("colspan " + x + " must be > 0");
		m_controlColSpan = x;
		return this;
	}

	/**
	 * Set the rowspan for only the control for all controls added after this call.
	 * @param x
	 * @return
	 */
	@Nonnull
	public HorizontalFormBuilder controlRowSpan(int x) {
		if(x < 0)
			throw new IllegalArgumentException("rowspan " + x + " must be > 0");
		m_controlRowSpan = x;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder defaultLabelClass(@Nullable String defaultLabelClass) {
		m_defaultLabelClass = defaultLabelClass;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder defaultControlClass(@Nullable String defaultControlClass) {
		m_defaultControlClass = defaultControlClass;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder labelClass(@Nullable String labelClass) {
		m_labelClass = labelClass;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder controlClass(@Nullable String controlClass) {
		m_controlClass = controlClass;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder tablePerRow() {
		m_tableMode = TableMode.perRow;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder tablePerForm() {
		m_tableMode = TableMode.perForm;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder labelWidth(@Nullable String s) {
		m_labelWidth = s;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder controlWidth(@Nullable String s) {
		m_controlWidth = s;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder labelNowrap() {
		m_labelNowrap = Boolean.TRUE;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder controlNowrap() {
		m_controlNoWrap = Boolean.TRUE;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder nowrap() {
		m_labelNowrap = Boolean.TRUE;
		m_controlNoWrap = Boolean.TRUE;
		return this;
	}

	@Nonnull
	public HorizontalFormBuilder width(@Nullable String s) {
		m_labelWidth = s;
		m_controlWidth = s;
		return this;
	}

}
