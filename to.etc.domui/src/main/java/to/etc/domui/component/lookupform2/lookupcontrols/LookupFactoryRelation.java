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
package to.etc.domui.component.lookupform2.lookupcontrols;

import to.etc.domui.component.input.LookupInput;
import to.etc.domui.component.lookup.EqLookupControlImpl;
import to.etc.domui.component.lookup.ILookupControlFactory;
import to.etc.domui.component.lookup.ILookupControlInstance;
import to.etc.domui.component.meta.MetaUtils;
import to.etc.domui.component.meta.PropertyMetaModel;
import to.etc.domui.component.meta.PropertyRelationType;
import to.etc.domui.component.meta.SearchPropertyMetaModel;
import to.etc.domui.dom.html.IControl;

import javax.annotation.Nonnull;

final class LookupFactoryRelation implements ILookupControlFactory {
	@Override
	public <T, X extends IControl<T>> int accepts(final @Nonnull SearchPropertyMetaModel spm, final X control) {
		final PropertyMetaModel< ? > pmm = MetaUtils.getLastProperty(spm);
		if(pmm.getRelationType() ==  PropertyRelationType.UP) {		// Accept only relations.
			return 4;
		}
		return -1;
	}

	@Override
	public <T, X extends IControl<T>> ILookupControlInstance<?> createControl(final @Nonnull SearchPropertyMetaModel spm, final X control) {
		final PropertyMetaModel< ? > pmm = MetaUtils.getLastProperty(spm);
		IControl< ? > input = control;
		if(input == null) {
			final LookupInput<Object> l = new LookupInput<Object>((Class<Object>) pmm.getActualType()); // Create a lookup thing for this one
			String hint = MetaUtils.findHintText(spm);
			if(null != hint)
				l.setHint(hint);
			input = l;
			if (spm.isPopupSearchImmediately()){
				l.setSearchImmediately(true);
			}
			if (spm.isPopupInitiallyCollapsed()){
				l.setPopupInitiallyCollapsed(true);
			}
		}
		return new EqLookupControlImpl<>(spm.getPropertyName(), input);
	}
}
