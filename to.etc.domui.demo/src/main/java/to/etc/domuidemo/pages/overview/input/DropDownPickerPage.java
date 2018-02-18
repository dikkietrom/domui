package to.etc.domuidemo.pages.overview.input;

import to.etc.domui.component.buttons.DefaultButton;
import to.etc.domui.component.input.EditableDropDownPicker;
import to.etc.domui.dom.html.Div;
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

		EditableDropDownPicker<Date> picker = new EditableDropDownPicker<>(Date.class);
		add(picker);
		picker.updateData(dates);
		SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy");
		picker.setToStringConverter((l, a) -> a == null ? null : sdf.format(a));

		picker.setOnValueChanged(a -> {
			System.out.println(">> " + a);
		});


		DefaultButton btn = new DefaultButton("validate", a -> {
			Div d = new Div();
			add(d);
			d.add("Control value = " + picker.getValue());
		});
		add(btn);
	}
}
