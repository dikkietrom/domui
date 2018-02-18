package to.etc.domuidemo.pages.overview.input;

import to.etc.domui.component.buttons.DefaultButton;
import to.etc.domui.component.input.EditableComboLookup;
import to.etc.domui.dom.html.Checkbox;
import to.etc.domui.dom.html.Div;
import to.etc.domui.dom.html.Label;
import to.etc.domui.dom.html.UrlPage;
import to.etc.util.DateUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 18-2-18.
 */
public class DropDownPickerPage extends UrlPage {
	@Override public void createContent() throws Exception {
		//-- Create a list of first day of the month dates
		List<Date> dates = new ArrayList<>();
		Calendar cal = Calendar.getInstance();
		DateUtil.clearTime(cal);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		for(int i = 0; i < 3*12; i++) {
			cal.add(Calendar.MONTH, -1);
			dates.add(cal.getTime());
		}

		EditableComboLookup<Date> picker = new EditableComboLookup<>(Date.class);
		add(picker);
		picker.setData(dates);
		SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy");
		picker.setConverter((l, a) -> a == null ? null : sdf.format(a));

		picker.setMandatory(true);

		picker.setOnValueChanged(a -> {
			System.out.println(">> " + a);
		});


		Div d = new Div();
		add(d);
		DefaultButton btn = new DefaultButton("validate", a -> {
			Div d2 = new Div();
			add(d2);
			d2.add("Control value = " + picker.getValue());
		});
		d.add(btn);

		Checkbox readOnly = new Checkbox();
		Label lb = new Label(readOnly, "Read Only");
		d.add(readOnly);
		d.add(lb);

		readOnly.bind().to(picker, "readOnly");
		readOnly.immediate();



	}
}
