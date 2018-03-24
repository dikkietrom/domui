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
package to.etc.domui.util;

/**
 * This interface handles drag-related events for objects. Objects that can be
 * dragged must have been associated with an IDraggable instance.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Dec 9, 2008
 */
public interface IDraggable {
	/**
	 * Set the drag handler to use for this thingy. When set (no an actual instance) this
	 * means the associated class can be dragged. When set to null the instance is undraggable.
	 * @param dh
	 */
	void setDragHandler(IDragHandler dh);

	/**
	 * Returns the current handler for dragging. If null this node cannot be dragged.
	 * @return
	 */
	IDragHandler getDragHandler();
}
