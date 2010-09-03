package to.etc.domui.ajax;

import java.io.*;

import javax.servlet.http.*;

import to.etc.domui.annotations.*;
import to.etc.domui.login.*;
import to.etc.domui.server.*;
import to.etc.domui.state.*;

public class AjaxRequestContext implements IRpcCallContext {
	private final RequestContextImpl m_rctx;

	private final AjaxRequestHandler m_rh;

	private final RpcCallHandler m_callHandler;

	public AjaxRequestContext(final AjaxRequestHandler ajaxRequestHandler, final RpcCallHandler ch, final RequestContextImpl ctx) {
		m_rh = ajaxRequestHandler;
		m_rctx = ctx;
		m_callHandler = ch;
	}

	public RequestContextImpl getRctx() {
		return m_rctx;
	}

	private HttpServletResponse getResponse() {
		return m_rctx.getResponse();
	}

	/*--------------------------------------------------------------*/
	/*	CODING:	ServiceCallerCallback interface.					*/
	/*--------------------------------------------------------------*/
	@Override
	public <T> T createHandlerClass(final Class<T> clz) throws Exception {
		return m_rh.makeCallClass(clz, this);
	}

	@Override
	public boolean hasRight(final String role) {
		IUser user = PageContext.getCurrentUser();
		if(user == null)
			return false;
		return user.hasRight(role);
	}

	@Override
	public <T> T allocateOutput(final Class<T> oc, final ResponseFormat rf) throws Exception {
		return null;
	}

	@Override
	public void outputCompleted(final Object output) throws Exception {}

	@Override
	public Writer getResponseWriter(final ResponseFormat format, final String callname) throws Exception {
		switch(format){
			default:
				throw new IllegalStateException("Unknown response format: " + format);

			case JSON:
				getResponse().setContentType("text/html"); // Jal 20060922 Do not change to text/javascript!! This makes Prototype eval() the response as a JS program which it is not.
				getResponse().setCharacterEncoding("utf-8");
				getResponse().addHeader("X-ETC-AJAX-CALL", callname);
				return getResponse().getWriter();

			case XML:
				getResponse().setContentType("text/xml");
				getResponse().setCharacterEncoding("utf-8");
				getResponse().addHeader("X-ETC-AJAX-CALL", callname);
				return getResponse().getWriter();
		}
	}

	/**
	 * Decode the call format (json, json-bulk, xml, xml-bulk), then execute.
	 * @param rurl2
	 * @throws Exception
	 */
	void execute(final String rurl) throws Exception {
		try {
			if(rurl == null)
				throw new RpcException("Missing url segment");
			int sx = 0;
			int ex = rurl.length();
			if(rurl.startsWith("/"))
				sx = 1;
			if(rurl.endsWith("/"))
				ex = ex - 1;
			int pos = rurl.lastIndexOf('.'); // Remove the suffix (.ajax usually)
			if(pos != -1)
				ex = pos;
			String call = rurl.substring(sx, ex); // Base name contains class and method.
			if(call.equals("bulk")) {
				executeBulkRequest();
				return;
			}

			//-- This is a parameter-based call, i.e. the call parameters are URL parameters
			String s = m_rctx.getParameter("_format"); // Format override present in request?
			ResponseFormat rf = null;
			if(s != null)
				rf = ResponseFormat.valueOf(s);

			IParameterProvider pp = new URLParameterProvider(m_rctx);
			m_callHandler.executeSingleCall(this, pp, call, rf);
		} catch(RpcException sx) {
			sx.setUrl(getRctx().getRequest().getRequestURI());
			//			sx.setContext(this);
			throw sx;
		} finally {

		}
	}

	/**
	 * Handles a bulk (multicall) request.
	 */
	private void executeBulkRequest() throws Exception {
		String json = m_rctx.getParameter("json");
		if(json != null) {
			m_callHandler.executeBulkJSON(this, json);
			return;
		}
		throw new RuntimeException("Bulk calls implemented for JSON only (and no json= parameter found).");
	}


}
