package to.etc.domui.component.input;

import to.etc.domui.component.input.ComboboxButton.HAlign;
import to.etc.domui.component.meta.MetaManager;
import to.etc.domui.converter.ConverterRegistry;
import to.etc.domui.converter.IObjectToStringConverter;
import to.etc.domui.dom.css.DisplayType;
import to.etc.domui.dom.html.Div;
import to.etc.domui.dom.html.IControl;
import to.etc.domui.dom.html.IValueChanged;
import to.etc.domui.dom.html.NodeBase;
import to.etc.domui.themes.Theme;
import to.etc.domui.util.DomUtil;
import to.etc.webapp.nls.NlsContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 18-2-18.
 */
public class EditableComboLookup<T> extends Div implements IControl<T> {
	@Nonnull
	private final Class<T> m_type;

	@Nonnull
	private ComboboxButton<T> m_picker = new ComboboxButton<>();

	@Nonnull
	private TextStr m_text = new TextStr();

	private T m_value;

	/** The list of values to pick from. */
	@Nullable
	private List<T> m_data;

	@Nullable
	private String m_dropDownIcon;

	@Nonnull
	private HAlign m_halign = ComboboxButton.HAlign.LEFT;

	@Nullable
	private IObjectToStringConverter<T> m_converter;

	@Nullable
	private IValueChanged<?> m_onValueChanged;

	private boolean m_mandatory;

	private boolean m_readOnly;

	private boolean m_disabled;

	public EditableComboLookup(@Nonnull Class<T> type) {
		super();
		m_type = type;
	}

	@Override
	public void createContent() throws Exception {
		super.createContent();
		setDisplay(DisplayType.INLINE_BLOCK);

		ComboboxButton<T> picker = m_picker;
		picker.setData(m_data);
		String dropDownIcon = m_dropDownIcon;
		if(dropDownIcon == null) {
			dropDownIcon = Theme.BTN_EDIT;
		}
		if(!isReadOnly()) {
			picker.setSrc(dropDownIcon);                    // FIXME WTF is that button doing here when readonly?
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
			String text = m_text.getValueSafe();
			m_text.clearMessage();
			adjustSelection(combo, text);
		});

		picker.setOnValueChanged((IValueChanged<ComboboxButton<T>>) component -> {
			T value = m_value = picker.getValueSafe();
			m_text.setValue(convertObjectToString(value));
			IValueChanged<EditableComboLookup<T>> ovc = (IValueChanged<EditableComboLookup<T>>) getOnValueChanged();
			if(ovc != null) {
				ovc.onValueChanged(this);
			}
		});

		add(m_text);
		add(picker);

		appendAfterMe(picker);
		picker.build();
		appendCreateJS("WebUI.initAutocomplete('" + m_text.getActualID() + "','" + m_picker.getSelectControl().getActualID() + "')");
	}

	private String convertObjectToString(T val) {
		IObjectToStringConverter<T> converter = m_converter;
		if(converter == null) {
			return ConverterRegistry.getConverter(m_type, null).convertObjectToString(NlsContext.getLocale(), val);
		} else {
			return converter.convertObjectToString(NlsContext.getLocale(), val);
		}
	}

	private void adjustSelection(ComboLookup<T> combo, String text) throws Exception {
		boolean found = false;
		ComboboxButton<T> picker = DomUtil.nullChecked(m_picker);
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
	@Nullable public List<T> getData() {
		return m_data;
	}

	/**
	 * Sets data that is used for picker select options.
	 */
	@Nonnull
	public EditableComboLookup<T> setData(@Nonnull List<T> data) {
		if(m_data == data) {
			return this;
		}
		m_data = data;
		m_picker.setData(data);
		if(!data.contains(m_value)) {
			m_value = null;
		}
		initSelectSizeAndValue();
		return this;
	}

	private void initSelectSizeAndValue() {
		List<T> data = getData();
		if(null == data)
			return;

		if(isMandatory()) {
			setComboSize(data.size());

			//workaround: we have to set a value to avoid rendering of empty option for mandatory combo
			if(!data.isEmpty() && data.get(0) != null) {
				m_picker.getSelectControl().setValue(data.get(0));
			}
		} else {
			setComboSize(data.size() + 1);
		}
	}

	private void setComboSize(int size) {
		int newSize = size > ComboboxButton.DEFAULT_COMBO_SIZE ? ComboboxButton.DEFAULT_COMBO_SIZE : size;
		m_picker.getSelectControl().setSize(newSize);
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

	@Nonnull
	public HAlign getHalign() {
		return m_halign;
	}

	public void setHalign(@Nonnull HAlign halign) {
		m_halign = halign;
		if(m_picker != null) {
			m_picker.setHalign(halign);
		}
	}

	@Nullable
	public IObjectToStringConverter<T> getConverter() {
		return m_converter;
	}

	public void setConverter(@Nullable IObjectToStringConverter<T> converter) {
		m_converter = converter;
		forceRebuild();
	}

	@Nullable @Override public T getValue() {
		String text = m_text.getValueSafe();
		if(null != text) {
			List<T> data = getData();
			if(data != null) {
				for(T datum : data) {
					String s = convertObjectToString(datum);
					if(text.equalsIgnoreCase(s)) {
						return datum;
					}
				}
			}
		}
		return null;
	}

	@Override public boolean isReadOnly() {
		return m_readOnly;
	}

	@Override public void setReadOnly(boolean ro) {
		if(m_readOnly == ro)
			return;
		m_readOnly = ro;
		forceRebuild();
	}

	@Override public boolean isDisabled() {
		return m_disabled;
	}

	@Override public void setDisabled(boolean d) {
		if(m_disabled == d)
			return;
		m_disabled = d;
		forceRebuild();
	}

	@Override public boolean isMandatory() {
		return m_mandatory;
	}

	@Override public void setMandatory(boolean ro) {
		m_mandatory = ro;
		m_picker.setMandatory(ro);
		m_text.setMandatory(true);

	}

	@Override public void setValue(T value) {
		if(MetaManager.areObjectsEqual(value, m_value))
			return;
		m_value = value;
		forceRebuild();
	}

	@Override @Nullable public IValueChanged<?> getOnValueChanged() {
		return m_onValueChanged;
	}

	@Override public void setOnValueChanged(@Nullable IValueChanged<?> onValueChanged) {
		m_onValueChanged = onValueChanged;
	}

	@Nullable @Override public NodeBase getForTarget() {
		return m_text;
	}
}
