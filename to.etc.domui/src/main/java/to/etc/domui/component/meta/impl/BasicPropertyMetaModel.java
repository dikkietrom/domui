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
package to.etc.domui.component.meta.impl;

import org.eclipse.jdt.annotation.NonNull;
import to.etc.domui.component.controlfactory.PropertyControlFactory;
import to.etc.domui.component.meta.NumericPresentation;
import to.etc.domui.component.meta.PropertyMetaValidator;
import to.etc.domui.component.meta.SortableType;
import to.etc.domui.component.meta.TemporalPresentationType;
import to.etc.domui.component.meta.YesNoType;
import to.etc.domui.converter.IConverter;

public class BasicPropertyMetaModel<T> {
	static public final String[] NO_NAMES = new String[0];

	static private final PropertyMetaValidator[] NO_VALIDATORS = new PropertyMetaValidator[0];

	private IConverter<T> m_converter;

	@NonNull
	private SortableType m_sortable = SortableType.UNKNOWN;

	private int m_displayLength = -1;

	@NonNull
	private YesNoType m_nowrap = YesNoType.UNKNOWN;

	private short m_precision = -1;

	private byte m_scale = -1;

	private boolean m_required;

	@NonNull
	private YesNoType m_readOnly = YesNoType.UNKNOWN;

	@NonNull
	private TemporalPresentationType m_temporal = TemporalPresentationType.UNKNOWN;

	@NonNull
	private NumericPresentation m_numericPresentation = NumericPresentation.UNKNOWN;

	@NonNull
	private PropertyMetaValidator[] m_validators = NO_VALIDATORS;

	private String m_regexpValidator;

	private String m_regexpUserString;

	/** T if marked as @Transient */
	private boolean m_transient;

	private PropertyControlFactory m_controlFactory;

	@NonNull
	private String[] m_columnNames = NO_NAMES;

	public IConverter<T> getConverter() {
		return m_converter;
	}

	public void setConverter(IConverter<T> converter) {
		m_converter = converter;
	}

	@NonNull
	public SortableType getSortable() {
		return m_sortable;
	}

	public void setSortable(@NonNull SortableType sortable) {
		m_sortable = sortable;
	}

	public int getDisplayLength() {
		return m_displayLength;
	}

	public void setDisplayLength(int displayLength) {
		m_displayLength = displayLength;
	}

	public boolean isRequired() {
		return m_required;
	}

	public void setRequired(boolean required) {
		m_required = required;
	}

	@NonNull
	public YesNoType getReadOnly() {
		return m_readOnly;
	}

	public void setReadOnly(@NonNull YesNoType readOnly) {
		m_readOnly = readOnly;
	}

	@NonNull
	public TemporalPresentationType getTemporal() {
		return m_temporal;
	}

	public void setTemporal(@NonNull TemporalPresentationType temporal) {
		m_temporal = temporal;
	}

	@NonNull
	public NumericPresentation getNumericPresentation() {
		return m_numericPresentation;
	}

	public void setNumericPresentation(@NonNull NumericPresentation numericPresentation) {
		m_numericPresentation = numericPresentation;
	}

	public int getPrecision() {
		return m_precision;
	}

	public void setPrecision(int prec) {
		if(prec < -1 || prec > Short.MAX_VALUE)
			throw new IllegalArgumentException("Precision out of range: " + prec);
		m_precision = (short) prec;
	}

	public int getScale() {
		return m_scale;
	}

	public void setScale(int scale) {
		if(scale < -1 || scale > Byte.MAX_VALUE)
			throw new IllegalArgumentException("Scale out of range: " + scale);
		m_scale = (byte) scale;
	}

	@NonNull
	public PropertyMetaValidator[] getValidators() {
		return m_validators;
	}

	public void setValidators(@NonNull PropertyMetaValidator[] validators) {
		m_validators = validators;
	}

	public String getRegexpValidator() {
		return m_regexpValidator;
	}

	public void setRegexpValidator(String regexpValidator) {
		m_regexpValidator = regexpValidator;
	}

	public String getRegexpUserString() {
		return m_regexpUserString;
	}

	public void setRegexpUserString(String regexpUserString) {
		m_regexpUserString = regexpUserString;
	}

	public boolean isTransient() {
		return m_transient;
	}

	public void setTransient(boolean transient1) {
		m_transient = transient1;
	}

	public PropertyControlFactory getControlFactory() {
		return m_controlFactory;
	}

	public void setControlFactory(PropertyControlFactory controlFactory) {
		m_controlFactory = controlFactory;
	}

	@NonNull
	public String[] getColumnNames() {
		return m_columnNames;
	}

	public void setColumnNames(@NonNull String[] columnNames) {
		if(null == columnNames)
			throw new IllegalArgumentException("Cannot accept null");
		m_columnNames = columnNames;
	}

	@NonNull
	public YesNoType getNowrap() {
		return m_nowrap;
	}

	public void setNowrap(@NonNull YesNoType nowrap) {
		m_nowrap = nowrap;
	}
}
