package to.etc.domui.logic.errors;

import javax.annotation.*;

import to.etc.domui.component.meta.*;
import to.etc.domui.dom.errors.*;

public class ProblemInstance {
	static private final Object[] NONE = new Object[0];

	final private Problem m_problem;

	final private Object m_instance;

	@Nullable
	final private PropertyMetaModel< ? > m_property;

	private Object[] m_parameters = NONE;

	private MsgType m_severity = MsgType.ERROR;

	ProblemInstance(Problem problem, Object instance, PropertyMetaModel< ? > property) {
		m_problem = problem;
		m_instance = instance;
		m_property = property;
	}

	ProblemInstance(Problem problem, Object instance) {
		m_problem = problem;
		m_instance = instance;
		m_property = null;
	}

	public Problem getProblem() {
		return m_problem;
	}

	public Object getInstance() {
		return m_instance;
	}

	public PropertyMetaModel< ? > getProperty() {
		return m_property;
	}

	/**
	 * Add message parameters to the error.
	 * @param arguments
	 * @return
	 */
	public ProblemInstance using(Object... arguments) {
		if(m_parameters.length == 0)
			m_parameters = arguments;
		else if(arguments.length > 0) {
			Object[] initial = new Object[arguments.length + m_parameters.length];
			int index = 0;
			for(Object o : m_parameters)
				initial[index++] = o;
			for(Object o : arguments)
				initial[index++] = o;
			m_parameters = initial;
		}
		return this;
	}

	/**
	 * Set the severity to warning.
	 * @return
	 */
	public ProblemInstance warning() {
		m_severity = MsgType.WARNING;
		return this;
	}

	/**
	 * Set the severity to error (it is that by default).
	 * @return
	 */
	public ProblemInstance error() {
		m_severity = MsgType.ERROR;
		return this;
	}

	/**
	 * Set the severity to info.
	 * @return
	 */
	public ProblemInstance info() {
		m_severity = MsgType.INFO;
		return this;
	}
}
