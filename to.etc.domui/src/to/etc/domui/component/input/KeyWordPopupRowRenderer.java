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
package to.etc.domui.component.input;

import javax.annotation.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.component.tbl.*;
import to.etc.domui.converter.*;
import to.etc.domui.dom.css.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;
import to.etc.webapp.nls.*;

/**
 * This is simplified row renderer that is used ad default render for popup results in keyword search.
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on 27 Jan 2010
 */
final class KeyWordPopupRowRenderer<T> implements IRowRenderer<T> {
	/** The class whose instances we'll render in this table. */
	@Nonnull
	private final Class<T> m_dataClass;

	@Nonnull
	final private ClassMetaModel m_metaModel;

	@Nullable
	private ICellClicked< ? > m_rowClicked;

	/** When the definition has completed (the object is used) this is TRUE; it disables all calls that change the definition */
	private boolean m_completed;

	@Nonnull
	private final ColumnDefList m_columnList;

	/*--------------------------------------------------------------*/
	/*	CODING:	Simple renderer initialization && parameterisation	*/
	/*--------------------------------------------------------------*/
	/**
	 * Create a renderer by handling the specified class and a list of properties off it.
	 * @param dataClass
	 * @param cols
	 */
	KeyWordPopupRowRenderer(@Nonnull final Class<T> dataClass, @Nonnull final ClassMetaModel cmm) {
		m_dataClass = dataClass;
		m_metaModel = cmm;
		m_columnList = new ColumnDefList(cmm);
		//		List<ExpandedDisplayProperty< ? >> xdpl;
		//		if(cols.length != 0)
		//			xdpl = ExpandedDisplayProperty.expandProperties(cmm, cols);
		//		else {
		//			final List<DisplayPropertyMetaModel> dpl = cmm.getTableDisplayProperties();
		//			if(dpl.size() == 0)
		//				throw new IllegalStateException("The list-of-columns to show for " + cmm + " is empty, and the class has no metadata (@MetaObject) defining a set of columns as default table columns, so there.");
		//			xdpl = ExpandedDisplayProperty.expandDisplayProperties(dpl, cmm, null);
		//		}
		//		addColumns(xdpl);
	}

	/**
	 * Throws an exception if this renderer has been completed and is unmutable.
	 */
	private void check() {
		if(m_completed)
			throw new IllegalStateException("Programmer error: This instance has been USED and cannot be changed anymore");
	}

	/**
	 * When set each row will be selectable (will react when the mouse hovers over it), and when clicked will call this handler.
	 * @return
	 */
	@Override
	@Nullable
	public ICellClicked< ? > getRowClicked() {
		return m_rowClicked;
	}

	/**
	 * When set each row will be selectable (will react when the mouse hovers over it), and when clicked will call this handler.
	 * @param rowClicked
	 */
	void setRowClicked(@Nonnull final ICellClicked< ? > rowClicked) {
		m_rowClicked = rowClicked;
	}

	/**
	 * This gets called by the data table component just before it starts to render
	 * a new page. When called the query has not yet been done and nothing is rendered
	 * for this object. This exposes the actual model that will be used during the rendering
	 * process and allows this component to define sorting, if needed.
	 *
	 * @see to.etc.domui.component.tbl.IRowRenderer#beforeQuery(to.etc.domui.component.tbl.DataTable)
	 */
	@Override
	public void beforeQuery(final @Nonnull TableModelTableBase<T> tbl) throws Exception {
		m_completed = true;
		if(m_columnList.size() == 0)
			addDefaultColumns();
	}

	@Override
	public void renderHeader(@Nonnull TableModelTableBase<T> tbl, @Nonnull HeaderContainer<T> cc) throws Exception {
		//-- Do not render a header.
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Actual rendering: a row.							*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 * @see to.etc.domui.component.tbl.IRowRenderer#renderRow(to.etc.domui.component.tbl.ColumnContainer, int, java.lang.Object)
	 */
	@Override
	public void renderRow(final @Nonnull TableModelTableBase<T> tbl, final @Nonnull ColumnContainer<T> cc, final int index, final @Nonnull T instance) throws Exception {
		if(m_rowClicked != null) {
			cc.getTR().setClicked(new IClicked<TR>() {
				@Override
				@SuppressWarnings("unchecked")
				public void clicked(final TR b) throws Exception {
					((ICellClicked<T>) getRowClicked()).cellClicked(b, instance);
				}
			});
			cc.getTR().addCssClass("ui-keyword-popup-row");
		}

		//must be set as bug fix for IE table rendering
		Object tblBase = cc.getTR().findParent(Table.class);
		if(tblBase instanceof Table) {
			((Table) tblBase).setWidth("100%");
			((Table) tblBase).setOverflow(Overflow.HIDDEN);
		}

		for(final SimpleColumnDef cd : m_columnList) {
			renderColumn(tbl, cc, index, instance, cd);
		}
	}

	/**
	 * Render a single column fully.
	 * @param tbl
	 * @param cc
	 * @param index
	 * @param instance
	 * @param cd
	 * @throws Exception
	 */
	private <X> void renderColumn(final TableModelTableBase<T> tbl, final ColumnContainer<T> cc, final int index, final T instance, final SimpleColumnDef cd) throws Exception {
		//-- If a value transformer is known get the column value, else just use the instance itself (case when Renderer is used)
		X colval;
		if(cd.getValueTransformer() == null)
			colval = (X) instance;
		else
			colval = (X) cd.getValueTransformer().getValue(instance);

		//-- Is a node renderer used?
		TD cell;
		Div wrapDiv = new Div();
		wrapDiv.setCssClass("no-wrap");
		cell = cc.add((NodeBase) null); // Add the new row
		cell.add(wrapDiv); // Add no-wrap div

		if(null != cd.getContentRenderer()) {
			((INodeContentRenderer<Object>) cd.getContentRenderer()).renderNodeContent(tbl, wrapDiv, colval, instance); // %&*(%&^%*&%&( generics require casting here
		} else {
			String s;
			if(colval == null)
				s = null;
			else {
				if(cd.getPresentationConverter() != null)
					s = ((IConverter<X>) cd.getPresentationConverter()).convertObjectToString(NlsContext.getLocale(), colval);
				else
					s = String.valueOf(colval);
			}
			if(s != null) {
				wrapDiv.add(s);
			}
		}

		if(cd.getAlign() != null)
			cell.setTextAlign(cd.getAlign());
		else {
			String cssc = cd.getCssClass();
			if(cssc != null) {
				cell.addCssClass(cssc);
			}
		}
	}

	public void add(SimpleColumnDef cd) {
		check();
		m_columnList.add(cd);
	}

	public <R> void addColumns(Object... cols) {
		check();
		m_columnList.addColumns(cols);
	}

	public void addDefaultColumns() {
		check();
		m_columnList.addDefaultColumns();
	}

	//	private void addColumns(final List<ExpandedDisplayProperty< ? >> xdpl) {
	//		for(final ExpandedDisplayProperty< ? > xdp : xdpl) {
	//			if(xdp instanceof ExpandedDisplayPropertyList) {
	//				//-- Flatten: call for subs recursively.
	//				final ExpandedDisplayPropertyList xdl = (ExpandedDisplayPropertyList) xdp;
	//				addColumns(xdl.getChildren());
	//				continue;
	//			}
	//
	//			//-- Create a column def from the metadata
	//			final SimpleColumnDef scd = new SimpleColumnDef(xdp);
	//			m_columnList.add(scd); // ORDER!
	//
	//			if(scd.getNumericPresentation() != null && scd.getNumericPresentation() != NumericPresentation.UNKNOWN) {
	//				scd.setCssClass("ui-numeric");
	//				scd.setHeaderCssClass("ui-numeric");
	//			}
	//		}
	//	}
}
