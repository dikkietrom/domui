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
package to.etc.domui.component2.controlfactory;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import to.etc.domui.component.input.Text2;
import to.etc.domui.component.meta.NumericPresentation;
import to.etc.domui.component.meta.PropertyMetaModel;
import to.etc.domui.dom.html.IControl;

import java.math.BigDecimal;

/**
 * Factory which creates a Text input specialized for entering monetary amounts. This
 * accepts properties with type=Double/double or BigDecimal, and with one of the monetary
 * numeric presentations.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Aug 4, 2009
 */
public class ControlCreatorMoney implements IControlCreator {
	/**
	 * Accept any type using a string.
	 */
	@Override
	public <T> int accepts(PropertyMetaModel<T> pmm, Class< ? extends IControl<T>> controlClass) {
		if(controlClass != null && !controlClass.isAssignableFrom(Text2.class)) 		// This will create a Text class,
			return -1;
		Class<?> clz = pmm.getActualType();
		if(clz != Double.class && clz != double.class && clz != BigDecimal.class)	// Must be proper type
			return -1;
		if(!NumericPresentation.isMonetary(pmm.getNumericPresentation()))
			return -1;
		return 2;
	}

	/**
	 * Create a Text control with the basic monetary converter, or the proper converter for the specified type.
	 */
	@Override
	public <T, C extends IControl<T>> C createControl(@NonNull PropertyMetaModel<T> pmm, @Nullable Class<C> controlClass) {
		Text2<T> txt;
		if(pmm.getActualType() == Double.class || pmm.getActualType() == double.class) {
			txt = (Text2<T>) Text2.createDoubleMoneyInput((PropertyMetaModel<Double>) pmm, true);
		} else if(pmm.getActualType() == BigDecimal.class) {
			txt = (Text2<T>) Text2.createBDMoneyInput((PropertyMetaModel<BigDecimal>) pmm, true);
		} else
				throw new IllegalStateException("Cannot handle type=" + pmm.getActualType() + " in monetary control factory");
		return (C) txt;
	}
}
