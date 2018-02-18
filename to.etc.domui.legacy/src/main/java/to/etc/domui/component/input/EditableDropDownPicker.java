package to.etc.domui.component.input;

import to.etc.domui.component.input.DropDownPicker.HAlign;
import to.etc.domui.converter.ConverterRegistry;
import to.etc.domui.converter.IObjectToStringConverter;
import to.etc.domui.dom.html.IValueChanged;
import to.etc.domui.themes.Theme;
import to.etc.domui.util.DomUtil;
import to.etc.webapp.nls.NlsContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * DEPRECATED: Use EditableComboLookup instead.
 *
 * This component incorrectly uses inheritance from TextStr but it handles values of
 * type T. This inconsistency means the IControl cannot be implemented, and many standard
 * methods on the control behave oddly as the control somehow is about 2 types at the same
 * time.
 *
 *
 * Encapsulates AutocompleteText and drop down picker into single component. Input behaves as autocomplete field that does search on select inside select within drop down picker.
 * Search is done at client side for faster user experience.
 * It also allows entering of new data (not contained inside predefined list), that is why it works on-top of String based input.
 *
 * FIXME Urgent This must implement IControl proper 8-(
 *
 * @author <a href="mailto:vmijic@execom.eu">Vladimir Mijic</a>
 * Created on Nov 6, 2012
 */
@Deprecated
public class EditableDropDownPicker<T> extends AutocompleteText {
	@Nullable
	private DropDownPicker<T> m_picker;

	@Nonnull
	private List<T> m_data = Collections.EMPTY_LIST;

	@Nullable
	private String m_dropDownIcon;

	@Nonnull
	private HAlign m_halign = DropDownPicker.HAlign.LEFT;

	@Nullable
	private IObjectToStringConverter<T> m_toStringConverter;

	@Nullable
	private T m_object;

	@Nonnull
	private final Class<T> m_type;

	/**
	 * Empty constructor.
	 * Before use, make sure to setup component using:
	 * <UL>
	 * <LI> {@link EditableDropDownPicker#setData(List)} </LI>
	 * <LI> {@link EditableDropDownPicker#setDropDownIcon(String)} </LI>
	 * <LI> {@link EditableDropDownPicker#setToStringConverter(IObjectToStringConverter)} in case of 'type' is not assignable from String.class</LI>
	 * </UL>
	 *
	 * @param type
	 */
	public EditableDropDownPicker(@Nonnull Class<T> type) {
		super();
		m_type = type;
	}

	/**
	 * Factory constructor.
	 * @param type
	 * @param data
	 * @param dropDownIcon
	 * @param toStringConverter In case of T = String, toStringConverter can be left null, otherwise it needs to be specified.
	 */
	public EditableDropDownPicker(@Nonnull Class<T> type, @Nonnull List<T> data, @Nonnull String dropDownIcon, @Nullable IObjectToStringConverter<T> toStringConverter) {
		this(type);
		m_data = data;
		m_dropDownIcon = dropDownIcon;
		m_toStringConverter = toStringConverter;
	}

	@Override
	public void createContent() throws Exception {
		super.createContent();

		DropDownPicker<T> picker = m_picker = new DropDownPicker<T>(m_data);
		String dropDownIcon = m_dropDownIcon;
		if(dropDownIcon == null) {
			dropDownIcon = Theme.BTN_EDIT;
		}
		if(!isReadOnly()) {
			picker.setSrc(dropDownIcon);					// FIXME WTF is that button doing here when readonly?
		}

		picker.setMandatory(isMandatory());
		picker.setValue(null);
		picker.setDisabled(isDisabled());
		picker.setReadOnly(isReadOnly());
		picker.setHalign(m_halign);
		picker.setAlignmentBase(this);

		//-- Make sure the lookup combo renders texts as we expect it
		picker.getSelectControl().setContentRenderer((node, object) -> node.add(convertObjectToString(object)));

		picker.setOnBeforeShow((sender, combo) -> {
			String text = getValueSafe();
			clearMessage();
			adjustSelection(combo, text);
		});

		picker.setOnValueChanged((IValueChanged<DropDownPicker<T>>) component -> {
			T value = m_object = component.getValueSafe();
			setValue(convertObjectToString(value));
			IValueChanged<EditableDropDownPicker<T>> onValueChanged = (IValueChanged<EditableDropDownPicker<T>>) EditableDropDownPicker.this.getOnValueChanged();
			if(onValueChanged != null) {
				onValueChanged.onValueChanged(EditableDropDownPicker.this);
			}
		});

		setSelect(picker.getSelectControl());
		appendAfterMe(picker);
		picker.build();
		initializeJS();
	}

	private String convertObjectToString(T val) {
		IObjectToStringConverter<T> converter = m_toStringConverter;
		if(converter == null) {
			return ConverterRegistry.getConverter(m_type, null).convertObjectToString(NlsContext.getLocale(), val);
		} else {
			return converter.convertObjectToString(NlsContext.getLocale(), val);
		}
	}

	private void adjustSelection(ComboLookup<T> combo, String text) throws Exception {
		boolean found = false;
		DropDownPicker<T> picker = DomUtil.nullChecked(m_picker);
		for(int i = 0; i < combo.getData().size(); i++) {
			T val = combo.getData().get(i);
			String optionText = convertObjectToString(val);
			if(text != null && text.equals(optionText)) {
				combo.setValue(val);
				picker.setButtonValue(text);
				found = true;
				break;
			}
		}
		initSelectSizeAndValue();
		if(!found) {
			picker.setButtonValue(null);
		}
	}

	/**
	 * Gets picker select options.
	 */
	public @Nonnull
	List<T> getData() {
		return m_data;
	}

	/**
	 * Sets data that is used for picker select options.
	 * @param data
	 */
	private void setData(@Nonnull List<T> data) {
		if(m_data != data) {
			m_data = data;
			if(null != m_picker) {
				//if picker is already created then switch it's data, otherwise data would be used when picker is creating
				m_picker.setData(data);
			}
		}
	}

	/**
	 * Update data displayed in picker select options.
	 * @param data
	 * @throws Exception
	 */
	public void updateData(@Nonnull List<T> data) throws Exception {
		setData(data);
		initSelectSizeAndValue();
	}


	private void initSelectSizeAndValue() throws Exception {
		if(isMandatory()){
			setComboSize(getData().size());
			//workaround: we have to set a value to avoid rendering of empty option for mandatory combo
			if(!getData().isEmpty() && getData().get(0) != null && m_picker != null){
				((ComboLookup<T>)m_picker.getSelectControl()).setValue(getData().get(0));
			}
		} else {
			setComboSize(getData().size() + 1);
		}
	}

	private void setComboSize(int size) throws Exception {
		int newSize = size > DropDownPicker.DEFAULT_COMBO_SIZE ? DropDownPicker.DEFAULT_COMBO_SIZE : size;
		if(m_picker != null){
			m_picker.getSelectControl().setSize(newSize);
		}
	}

	@Nullable
	public String getDropDownIcon() {
		return m_dropDownIcon;
	}

	public void setDropDownIcon(@Nullable String dropDownIcon) {
		m_dropDownIcon = dropDownIcon;
		if(m_picker != null) {
			m_picker.setSrc(dropDownIcon);
		}
	}

	public @Nonnull
	HAlign getHalign() {
		return m_halign;
	}

	public void setHalign(@Nonnull HAlign halign) {
		m_halign = halign;
		if(m_picker != null) {
			m_picker.setHalign(halign);
		}
	}

	@Nullable
	public IObjectToStringConverter<T> getToStringConverter() {
		return m_toStringConverter;
	}

	public void setToStringConverter(@Nullable IObjectToStringConverter<T> toStringConverter) {
		m_toStringConverter = toStringConverter;
		forceRebuild();
	}

	@Nullable
	public T getObject() {
		return m_object;
	}

	public void setObject(@Nullable T object) {
		m_object = object;
	}
}
