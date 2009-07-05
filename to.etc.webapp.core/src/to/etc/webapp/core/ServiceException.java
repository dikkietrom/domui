package to.etc.webapp.core;

import java.util.*;

import javax.servlet.http.*;

/**
 * Any kind of service requested thru a generic container.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Apr 14, 2006
 */
public class ServiceException extends Exception {
	/** The incoming request URL, if applicable. */
	private String m_url;

	/** The request type */
	private String m_method;

	/** The query string if this was a get */
	private String m_queryString;

	/** The incoming parameters, */
	private Map<String, String[]> m_parameters;

	private Map<String, String[]> m_headers;

	/** The servlet that is supposed to handle this */
	private String m_servletPath;

	/** The remote address for this thingy. */
	private String m_remoteAddress;

	private String m_remoteUser;

	private HttpServlet m_servlet;

	public ServiceException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public ServiceException(final String message) {
		super(message);
	}

	public boolean hasContext() {
		return m_method != null;
	}

	/**
	 * Retrieve info on the call from the context passed.
	 * @param ctx
	 */
	public void init(final HttpServletRequest req) {
		m_url = req.getRequestURI();
		m_queryString = req.getQueryString();
		m_method = req.getMethod();
		m_servletPath = req.getServletPath();
		m_remoteAddress = req.getRemoteHost();
		m_remoteUser = req.getRemoteUser();
		m_parameters = new HashMap<String, String[]>(req.getParameterMap());
		m_headers = new HashMap<String, String[]>();
		ArrayList<String> al = new ArrayList<String>();
		for(Enumeration<String> e = req.getHeaderNames(); e.hasMoreElements();) {
			String name = e.nextElement();
			for(Enumeration<String> ve = req.getHeaders(name); ve.hasMoreElements();)
				al.add(ve.nextElement());
			m_headers.put(name, al.toArray(new String[al.size()]));
			al.clear();
		}
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	Getters and setters.								*/
	/*--------------------------------------------------------------*/
	public Map<String, String[]> getHeaders() {
		return m_headers;
	}

	public void setHeaders(final Map<String, String[]> headers) {
		m_headers = headers;
	}

	public String getMethod() {
		return m_method;
	}

	public void setMethod(final String method) {
		m_method = method;
	}

	public Map<String, String[]> getParameters() {
		return m_parameters;
	}

	public void setParameters(final Map<String, String[]> parameters) {
		m_parameters = parameters;
	}

	public String getQueryString() {
		return m_queryString;
	}

	public void setQueryString(final String queryString) {
		m_queryString = queryString;
	}

	public String getRemoteAddress() {
		return m_remoteAddress;
	}

	public void setRemoteAddress(final String remoteAddress) {
		m_remoteAddress = remoteAddress;
	}

	public String getRemoteUser() {
		return m_remoteUser;
	}

	public void setRemoteUser(final String remoteUser) {
		m_remoteUser = remoteUser;
	}

	public String getServletPath() {
		return m_servletPath;
	}

	public void setServletPath(final String servlet) {
		m_servletPath = servlet;
	}

	public String getUrl() {
		return m_url;
	}

	public void setUrl(final String url) {
		m_url = url;
	}

	public void setServlet(final HttpServlet servlet) {
		m_servlet = servlet;
	}

	public HttpServlet getServlet() {
		return m_servlet;
	}
}
